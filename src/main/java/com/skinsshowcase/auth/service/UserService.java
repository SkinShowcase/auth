package com.skinsshowcase.auth.service;

import com.skinsshowcase.auth.client.SteamGatewayProfileClient;
import com.skinsshowcase.auth.dto.AdminReportsPageDto;
import com.skinsshowcase.auth.dto.TradeLinkResponseDto;
import com.skinsshowcase.auth.dto.UserReportResponseDto;
import com.skinsshowcase.auth.entity.User;
import com.skinsshowcase.auth.entity.UserReport;
import com.skinsshowcase.auth.exception.ConflictException;
import com.skinsshowcase.auth.metrics.AuthMetrics;
import com.skinsshowcase.auth.repository.UserReportRepository;
import com.skinsshowcase.auth.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern STEAM_ID_PATTERN = Pattern.compile("^[0-9]{17}$");
    private static final int BATCH_PRESET_AVATAR_MAX = 100;
    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9 _\\-]{2,32}$");
    private static final int DISPLAY_NAME_MAX_ATTEMPTS = 24;
    private static final int REPORT_COOLDOWN_HOURS = 24;

    private final UserRepository userRepository;
    private final UserReportRepository userReportRepository;
    private final SteamGatewayProfileClient steamGatewayProfileClient;
    private final PresetAvatarService presetAvatarService;
    private final AuthMetrics authMetrics;
    private final UserDataHashingService userDataHashingService;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(UserRepository userRepository,
                       UserReportRepository userReportRepository,
                       SteamGatewayProfileClient steamGatewayProfileClient,
                       PresetAvatarService presetAvatarService,
                       AuthMetrics authMetrics,
                       UserDataHashingService userDataHashingService) {
        this.userRepository = userRepository;
        this.userReportRepository = userReportRepository;
        this.steamGatewayProfileClient = steamGatewayProfileClient;
        this.presetAvatarService = presetAvatarService;
        this.authMetrics = authMetrics;
        this.userDataHashingService = userDataHashingService;
    }

    /**
     * Steam ID по имени для просмотра: при приватном профиле чужой пользователь не резолвится (как 404).
     */
    @Transactional(readOnly = true)
    public Optional<String> getSteamIdByUsernameForViewer(String viewerSteamId, String username) {
        var userOpt = getByUsername(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        var user = userOpt.get();
        if (!sameSteamId(viewerSteamId, user) && user.isPrivateProfile()) {
            return Optional.empty();
        }
        if (!matchesSteamIdFormat(username)) {
            return Optional.empty();
        }
        return Optional.of(username.trim());
    }

    /**
     * Для внутреннего API: флаг приватности по Steam ID (только известные пользователи; иначе не попадают в map).
     */
    @Transactional(readOnly = true)
    public Map<String, Boolean> mapPrivateProfileBySteamId(Collection<String> steamIds) {
        var out = new LinkedHashMap<String, Boolean>();
        if (steamIds == null || steamIds.isEmpty()) {
            return out;
        }
        for (var raw : steamIds) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            var trimmed = raw.trim();
            if (!matchesSteamIdFormat(trimmed)) {
                continue;
            }
            var user = findBySteamId(trimmed).orElse(null);
            out.put(trimmed, user != null && user.isPrivateProfile());
        }
        return out;
    }

    /**
     * Для внутреннего API: отображаемое имя пользователя — {@code display_name}, иначе сохранённый Steam {@code persona_name}.
     * В map попадают только известные пользователи с непустым лейблом.
     */
    @Transactional(readOnly = true)
    public Map<String, String> mapProfileLabelBySteamId(Collection<String> steamIds) {
        var out = new LinkedHashMap<String, String>();
        if (steamIds == null || steamIds.isEmpty()) {
            return out;
        }
        for (var raw : steamIds) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            var trimmed = raw.trim();
            if (!matchesSteamIdFormat(trimmed)) {
                continue;
            }
            var user = findBySteamId(trimmed).orElse(null);
            if (user == null) {
                continue;
            }
            var label = resolveStoredProfileLabel(user);
            if (label == null) {
                continue;
            }
            out.put(trimmed, label);
        }
        return out;
    }

    private static String resolveStoredProfileLabel(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName().trim();
        }
        return null;
    }

    /**
     * Резолв: SteamID64, display_name или persona_name (Steam).
     */
    public Optional<User> getByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        var trimmed = username.trim();
        if (STEAM_ID_PATTERN.matcher(trimmed).matches()) {
            return findBySteamId(trimmed);
        }
        var byDisplay = userRepository.findByDisplayName(trimmed);
        return byDisplay;
    }

    @Transactional
    public User getOrCreate(String steamId) {
        var existing = findBySteamId(steamId);
        if (existing.isPresent()) {
            var u = existing.get();
            ensureDisplayNameAssigned(u);
            authMetrics.recordUserEnsure("existing");
            applyUserHashes(u);
            return userRepository.save(u);
        }
        var user = new User(hashSteamId(steamId));
        assignUniqueDisplayName(user);
        authMetrics.recordUserEnsure("created");
        applyUserHashes(user);
        return userRepository.save(user);
    }

    @Transactional
    public void refreshSteamProfileFromGateway(String steamId) {
        var user = findBySteamId(steamId).orElse(null);
        if (user == null) {
            authMetrics.recordProfileRefresh("user_missing");
            return;
        }
        var profile = steamGatewayProfileClient.fetchPlayerSummary(steamId);
        if (profile.isEmpty()) {
            authMetrics.recordProfileRefresh("gateway_empty");
            return;
        }
        applyUserHashes(user);
        userRepository.save(user);
        authMetrics.recordProfileRefresh("updated");
    }

    @Transactional
    public void updateDisplayName(String steamId, String rawDisplayName) {
        var user = getOrCreate(steamId);
        var normalized = normalizeDisplayName(rawDisplayName);
        if (normalized.equals(user.getDisplayName())) {
            return;
        }
        var taken = userRepository.findByDisplayName(normalized);
        if (taken.isPresent() && !sameSteamId(steamId, taken.get())) {
            throw new ConflictException("Display name already taken");
        }
        user.setDisplayName(normalized);
        applyUserHashes(user);
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Display name already taken");
        }
    }

    @Transactional
    public void reportUser(String reporterSteamId, String reportedSteamId, String reason, String details) {
        if (reporterSteamId.equals(reportedSteamId)) {
            throw new IllegalArgumentException("Cannot report yourself");
        }
        if (!matchesSteamIdFormat(reportedSteamId)) {
            throw new IllegalArgumentException("Invalid steam id");
        }
        var since = Instant.now().minus(REPORT_COOLDOWN_HOURS, ChronoUnit.HOURS);
        var reporterHash = hashSteamId(reporterSteamId);
        var reportedHash = hashSteamId(reportedSteamId);
        if (userReportRepository.existsByReporterSteamIdAndReportedSteamIdAndCreatedAtAfter(
                reporterHash, reportedHash, since)) {
            throw new ConflictException("Report already submitted recently for this user");
        }
        var r = normalizeReportReason(reason);
        var d = truncateDetails(details);
        var report = new UserReport(UUID.randomUUID(), reporterHash, reportedHash, r, d, Instant.now());
        userReportRepository.save(report);
    }

    @Transactional
    public void updatePresetAvatar(String steamId, Integer presetAvatarId) {
        var user = getOrCreate(steamId);
        var id = presetAvatarService.requireValidPresetId(presetAvatarId);
        user.setAvatarSource("PRESET");
        user.setSelectedPresetAvatarId(id);
        applyUserHashes(user);
        userRepository.save(user);
    }

    public Optional<User> getBySteamId(String steamId) {
        return findBySteamId(steamId);
    }

    @Transactional(readOnly = true)
    public AdminReportsPageDto listReportsForAdmin(int page, int rawSize) {
        var size = Math.clamp(rawSize, 1, 100);
        var safePage = Math.max(page, 0);
        var pageable = PageRequest.of(safePage, size);
        var result = userReportRepository.findAllByOrderByCreatedAtDesc(pageable);
        var content = mapReportsToDto(result.getContent());
        return new AdminReportsPageDto(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private List<UserReportResponseDto> mapReportsToDto(List<UserReport> reports) {
        var list = new ArrayList<UserReportResponseDto>();
        for (var r : reports) {
            list.add(toReportResponseDto(r));
        }
        return list;
    }

    private static UserReportResponseDto toReportResponseDto(UserReport r) {
        return new UserReportResponseDto(
                r.getId(),
                r.getReporterSteamId(),
                r.getReportedSteamId(),
                r.getReason(),
                r.getDetails(),
                r.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public boolean isUserBlocked(String steamId) {
        var opt = findBySteamId(steamId);
        if (opt.isEmpty()) {
            return false;
        }
        return opt.get().isBlocked();
    }

    @Transactional
    public void setUserBlocked(String steamId, boolean blocked) {
        if (!matchesSteamIdFormat(steamId)) {
            throw new IllegalArgumentException("Invalid steam id");
        }
        var user = findBySteamId(steamId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        user.setBlocked(blocked);
        applyUserHashes(user);
        userRepository.save(user);
    }

    public boolean matchesSteamIdFormat(String steamId) {
        return steamId != null && STEAM_ID_PATTERN.matcher(steamId).matches();
    }

    public Optional<TradeLinkResponseDto> getTradeLinkForViewer(String viewerSteamId, String targetSteamId) {
        var target = findBySteamId(targetSteamId).orElse(null);
        if (target == null) {
            return Optional.empty();
        }
        if (!sameSteamId(viewerSteamId, target) && target.isPrivateProfile()) {
            return Optional.empty();
        }
        return Optional.of(new TradeLinkResponseDto(target.getSteamTradeLink()));
    }

    @Transactional
    public void updateSteamTradeLink(String steamId, String rawTradeUrl) {
        var user = getOrCreate(steamId);
        var normalized = normalizeTradeLinkInput(rawTradeUrl);
        user.setSteamTradeLink(normalized);
        applyUserHashes(user);
        userRepository.save(user);
    }

    @Transactional
    public void setPrivacy(String steamId, boolean privateProfile) {
        var user = findBySteamId(steamId).orElse(null);
        if (user == null) {
            return;
        }
        user.setPrivateProfile(privateProfile);
        applyUserHashes(user);
        userRepository.save(user);
    }

    @Transactional
    public void updateLastOnline(String steamId) {
        var user = findBySteamId(steamId).orElse(null);
        if (user == null) {
            return;
        }
        user.setLastOnlineAt(Instant.now());
        applyUserHashes(user);
        userRepository.save(user);
    }

    /**
     * Для списка Steam ID: id выбранного пресета (1–8) для пользователя в БД; иначе null (не найден).
     */
    @Transactional(readOnly = true)
    public Map<String, Integer> mapPresetAvatarIdsBySteamIdForViewer(List<String> steamIds) {
        var result = new LinkedHashMap<String, Integer>();
        if (steamIds == null || steamIds.isEmpty()) {
            return result;
        }
        var uniqueOrdered = dedupeSteamIdsPreserveOrder(steamIds);
        if (uniqueOrdered.size() > BATCH_PRESET_AVATAR_MAX) {
            throw new IllegalArgumentException("At most " + BATCH_PRESET_AVATAR_MAX + " steam IDs allowed");
        }
        for (var steamId : uniqueOrdered) {
            appendPresetAvatarMapping(result, steamId);
        }
        return result;
    }

    public String resolveEffectiveAvatarUrl(User user) {
        var id = resolvePresetAvatarIdOrDefault(user);
        return presetAvatarService.publicUrlFor(id);
    }

    private static int resolvePresetAvatarIdOrDefault(User user) {
        var id = user.getSelectedPresetAvatarId();
        if (id == null) {
            return 1;
        }
        return id;
    }

    private static List<String> dedupeSteamIdsPreserveOrder(List<String> steamIds) {
        var seen = new HashSet<String>();
        var out = new ArrayList<String>();
        for (var raw : steamIds) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            var trimmed = raw.trim();
            if (!seen.add(trimmed)) {
                continue;
            }
            out.add(trimmed);
        }
        return out;
    }

    private void appendPresetAvatarMapping(Map<String, Integer> result, String steamId) {
        if (!STEAM_ID_PATTERN.matcher(steamId).matches()) {
            throw new IllegalArgumentException("Invalid Steam ID in batch");
        }
        var userOpt = findBySteamId(steamId);
        if (userOpt.isEmpty()) {
            result.put(steamId, null);
            return;
        }
        var user = userOpt.get();
        result.put(steamId, resolvePresetAvatarIdOrDefault(user));
    }

    private void ensureDisplayNameAssigned(User user) {
        if (user.getDisplayName() == null || user.getDisplayName().isBlank()) {
            assignUniqueDisplayName(user);
        }
    }

    private void assignUniqueDisplayName(User user) {
        for (var attempt = 0; attempt < DISPLAY_NAME_MAX_ATTEMPTS; attempt++) {
            var candidate = randomDisplayName();
            user.setDisplayName(candidate);
            if (!displayNameTakenByOther(user.getSteamId(), candidate)) {
                return;
            }
        }
        var hashTail = user.getSteamId().substring(Math.max(0, user.getSteamId().length() - 8));
        user.setDisplayName("User" + hashTail);
    }

    private boolean displayNameTakenByOther(String steamIdHash, String displayName) {
        var opt = userRepository.findByDisplayName(displayName);
        if (opt.isEmpty()) {
            return false;
        }
        return !opt.get().getSteamId().equals(steamIdHash);
    }

    private String randomDisplayName() {
        var n = 1_000_000 + secureRandom.nextInt(9_000_000);
        return "User " + n;
    }

    private static String normalizeDisplayName(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Display name required");
        }
        var t = raw.trim();
        if (!DISPLAY_NAME_PATTERN.matcher(t).matches()) {
            throw new IllegalArgumentException("Invalid display name");
        }
        return t;
    }

    private static String normalizeReportReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason required");
        }
        var t = reason.trim();
        if (t.length() > 64) {
            return t.substring(0, 64);
        }
        return t;
    }

    private static String truncateDetails(String details) {
        if (details == null || details.isBlank()) {
            return null;
        }
        var t = details.trim();
        if (t.length() <= 2000) {
            return t;
        }
        return t.substring(0, 2000);
    }

    private static String normalizeTradeLinkInput(String raw) {
        if (raw == null) {
            return null;
        }
        var trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!isValidSteamTradeUrl(trimmed)) {
            throw new IllegalArgumentException("Invalid Steam trade URL");
        }
        return trimmed;
    }

    private static boolean isValidSteamTradeUrl(String url) {
        var uri = parseUriOrNull(url);
        if (uri == null) {
            return false;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        var host = uri.getHost();
        if (host == null) {
            return false;
        }
        if (!isSteamCommunityHost(host)) {
            return false;
        }
        return hasTradeOfferNewPath(uri.getPath());
    }

    private static URI parseUriOrNull(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static boolean isSteamCommunityHost(String host) {
        var h = host.toLowerCase();
        return h.equals("steamcommunity.com") || h.endsWith(".steamcommunity.com");
    }

    private static boolean hasTradeOfferNewPath(String path) {
        if (path == null) {
            return false;
        }
        return path.contains("/tradeoffer/new");
    }

    private void applyUserHashes(User user) {
        user.setDisplayNameHash(userDataHashingService.sha256(user.getDisplayName()));
    }

    private Optional<User> findBySteamId(String steamId) {
        return userRepository.findById(hashSteamId(steamId));
    }

    private String hashSteamId(String steamId) {
        if (!matchesSteamIdFormat(steamId)) {
            throw new IllegalArgumentException("Invalid steam id");
        }
        return userDataHashingService.sha256(steamId);
    }

    private boolean sameSteamId(String steamId, User user) {
        return hashSteamId(steamId).equals(user.getSteamId());
    }
}
