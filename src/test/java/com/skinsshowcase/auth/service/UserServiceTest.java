package com.skinsshowcase.auth.service;

import com.skinsshowcase.auth.client.SteamGatewayProfileClient;
import com.skinsshowcase.auth.dto.SteamProfileSummaryDto;
import com.skinsshowcase.auth.entity.User;
import com.skinsshowcase.auth.entity.UserReport;
import com.skinsshowcase.auth.exception.ConflictException;
import com.skinsshowcase.auth.metrics.AuthMetrics;
import com.skinsshowcase.auth.repository.UserReportRepository;
import com.skinsshowcase.auth.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    private static final String SID = "76561198000000001";
    private static final String SID2 = "76561198000000002";
    private static final String H1 = "hash_one";
    private static final String H2 = "hash_two";

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserReportRepository userReportRepository;
    @Mock
    private SteamGatewayProfileClient steamGatewayProfileClient;
    @Mock
    private PresetAvatarService presetAvatarService;
    @Mock
    private UserDataHashingService userDataHashingService;

    private AuthMetrics authMetrics;
    private UserService userService;

    @BeforeEach
    void setUp() {
        authMetrics = new AuthMetrics(new SimpleMeterRegistry());
        userService = new UserService(
                userRepository,
                userReportRepository,
                steamGatewayProfileClient,
                presetAvatarService,
                authMetrics,
                userDataHashingService);
        lenient().when(userDataHashingService.sha256(any())).thenAnswer(inv -> {
            var s = inv.getArgument(0);
            if (s == null) {
                return "h_null";
            }
            var str = (String) s;
            return "dh_" + str.hashCode();
        });
        when(userDataHashingService.sha256(eq(SID))).thenReturn(H1);
        when(userDataHashingService.sha256(eq(SID2))).thenReturn(H2);
        lenient().when(userRepository.findByDisplayName(any())).thenReturn(Optional.empty());
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(presetAvatarService.requireValidPresetId(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(presetAvatarService.publicUrlFor(any(Integer.class))).thenReturn("http://avatar/1");
    }

    @Test
    void getSteamIdByUsernameForViewer_privateOther_empty() {
        var u = new User(H2);
        u.setSteamId64(SID2);
        u.setPrivateProfile(true);
        when(userRepository.findByDisplayName("bob")).thenReturn(Optional.of(u));

        assertThat(userService.getSteamIdByUsernameForViewer(SID, "bob")).isEmpty();
    }

    @Test
    void getSteamIdByUsernameForViewer_ownerSeesPrivate() {
        var u = new User(H1);
        u.setSteamId64(SID);
        u.setPrivateProfile(true);
        when(userRepository.findByDisplayName("bob")).thenReturn(Optional.of(u));

        assertThat(userService.getSteamIdByUsernameForViewer(SID, "bob")).contains(SID);
    }

    @Test
    void getSteamIdByUsernameForViewer_steamIdAsUsername_returnsSame() {
        var u = new User(H1);
        u.setSteamId64(SID);
        u.setPrivateProfile(false);
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));
        assertThat(userService.getSteamIdByUsernameForViewer(SID2, SID)).contains(SID);
    }

    @Test
    void mapPrivateProfileBySteamId_skipsInvalid() {
        when(userRepository.findById(H1)).thenReturn(Optional.empty());
        var raw = new ArrayList<String>();
        raw.add("");
        raw.add("bad");
        raw.add(null);
        raw.add(SID);
        assertThat(userService.mapPrivateProfileBySteamId(raw)).containsEntry(SID, false);
    }

    @Test
    void mapProfileLabelBySteamId_usesDisplayName() {
        var u = new User(H1);
        u.setDisplayName("Nick");
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));

        assertThat(userService.mapProfileLabelBySteamId(List.of(SID))).containsEntry(SID, "Nick");
    }

    @Test
    void mapProfileLabelBySteamId_blankDisplay_skipped() {
        var u = new User(H1);
        u.setDisplayName("  ");
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));

        assertThat(userService.mapProfileLabelBySteamId(List.of(SID))).isEmpty();
    }

    @Test
    void getByUsername_blank_empty() {
        assertThat(userService.getByUsername("  ")).isEmpty();
    }

    @Test
    void getByUsername_steam64_findById() {
        var u = new User(H1);
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));
        assertThat(userService.getByUsername(SID)).contains(u);
    }

    @Test
    void getOrCreate_existing_ensuresDisplay() {
        var u = new User(H1);
        u.setSteamId64(SID);
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));
        when(userRepository.save(u)).thenReturn(u);

        var out = userService.getOrCreate(SID);
        assertThat(out).isSameAs(u);
        verify(userRepository).save(u);
    }

    @Test
    void getOrCreate_newUser_saves() {
        when(userRepository.findById(H1)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var out = userService.getOrCreate(SID);
        assertThat(out.getSteamId64()).isEqualTo(SID);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void refreshSteamProfileFromGateway_missingUser_recordsMetric() {
        when(userRepository.findById(H1)).thenReturn(Optional.empty());
        userService.refreshSteamProfileFromGateway(SID);
        verify(steamGatewayProfileClient, never()).fetchPlayerSummary(anyString());
    }

    @Test
    void refreshSteamProfileFromGateway_emptyProfile() {
        var u = new User(H1);
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));
        when(steamGatewayProfileClient.fetchPlayerSummary(SID)).thenReturn(Optional.empty());
        userService.refreshSteamProfileFromGateway(SID);
        verify(userRepository, never()).save(any());
    }

    @Test
    void refreshSteamProfileFromGateway_updates() {
        var u = new User(H1);
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));
        when(steamGatewayProfileClient.fetchPlayerSummary(SID))
                .thenReturn(Optional.of(new SteamProfileSummaryDto(SID, "p", null, null)));
        when(userRepository.save(u)).thenReturn(u);
        userService.refreshSteamProfileFromGateway(SID);
        verify(userRepository).save(u);
    }

    @Test
    void updateDisplayName_noopWhenSame() {
        var u = new User(H1);
        u.setDisplayName("Valid_Name");
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));
        userService.updateDisplayName(SID, "Valid_Name");
        verify(userRepository, times(1)).save(u);
    }

    @Test
    void updateDisplayName_conflictWhenTaken() {
        var u = new User(H1);
        u.setSteamId64(SID);
        u.setDisplayName("Old");
        var other = new User(H2);
        other.setDisplayName("New_Name");
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));
        when(userRepository.findByDisplayName("New_Name")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> userService.updateDisplayName(SID, "New_Name"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateDisplayName_dataIntegrity_conflict() {
        var u = new User(H1);
        u.setSteamId64(SID);
        u.setDisplayName("Old");
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));
        when(userRepository.findByDisplayName("New_Name")).thenReturn(Optional.empty());
        when(userRepository.save(u))
                .thenReturn(u)
                .thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> userService.updateDisplayName(SID, "New_Name"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reportUser_self_illegal() {
        assertThatThrownBy(() -> userService.reportUser(SID, SID, "spam", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reportUser_invalidReported_illegal() {
        assertThatThrownBy(() -> userService.reportUser(SID, "bad", "spam", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reportUser_duplicate_conflict() {
        when(userReportRepository.existsByReporterSteamIdAndReportedSteamIdAndCreatedAtAfter(
                eq(H1), eq(H2), any())).thenReturn(true);

        assertThatThrownBy(() -> userService.reportUser(SID, SID2, "spam", "details"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reportUser_saves() {
        when(userReportRepository.existsByReporterSteamIdAndReportedSteamIdAndCreatedAtAfter(
                eq(H1), eq(H2), any())).thenReturn(false);
        userService.reportUser(SID, SID2, "spam", "x".repeat(3000));
        verify(userReportRepository).save(any(UserReport.class));
    }

    @Test
    void updatePresetAvatar_updatesUser() {
        var u = new User(H1);
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));
        when(userRepository.save(u)).thenReturn(u);
        userService.updatePresetAvatar(SID, 3);
        assertThat(u.getSelectedPresetAvatarId()).isEqualTo(3);
        assertThat(u.getAvatarSource()).isEqualTo("PRESET");
    }

    @Test
    void listReportsForAdmin_mapsPage() {
        var r = new UserReport(UUID.randomUUID(), H1, H2, "a", "b", Instant.now());
        var page = new PageImpl<>(List.of(r), PageRequest.of(0, 20), 1);
        when(userReportRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(page);

        var dto = userService.listReportsForAdmin(0, 20);
        assertThat(dto.content()).hasSize(1);
        assertThat(dto.totalElements()).isEqualTo(1);
    }

    @Test
    void isUserBlocked_unknown_false() {
        when(userRepository.findById(H1)).thenReturn(Optional.empty());
        assertThat(userService.isUserBlocked(SID)).isFalse();
    }

    @Test
    void setUserBlocked_invalidSteamId() {
        assertThatThrownBy(() -> userService.setUserBlocked("x", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setUserBlocked_userMissing() {
        when(userRepository.findById(H1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.setUserBlocked(SID, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setUserBlocked_updates() {
        var u = new User(H1);
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));
        when(userRepository.save(u)).thenReturn(u);
        userService.setUserBlocked(SID, true);
        assertThat(u.isBlocked()).isTrue();
    }

    @Test
    void getTradeLinkForViewer_privateOther_empty() {
        var u = new User(H2);
        u.setPrivateProfile(true);
        when(userRepository.findById(H2)).thenReturn(Optional.of(u));
        assertThat(userService.getTradeLinkForViewer(SID, SID2)).isEmpty();
    }

    @Test
    void updateSteamTradeLink_valid_https() {
        var u = new User(H1);
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));
        when(userRepository.save(u)).thenReturn(u);
        var url = "https://steamcommunity.com/tradeoffer/new/?partner=1&token=abc";
        userService.updateSteamTradeLink(SID, url);
        assertThat(u.getSteamTradeLink()).isEqualTo(url);
    }

    @Test
    void updateSteamTradeLink_invalid() {
        var u = new User(H1);
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> userService.updateSteamTradeLink(SID, "http://evil.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setPrivacy_missingUser_noSave() {
        when(userRepository.findById(H1)).thenReturn(Optional.empty());
        userService.setPrivacy(SID, true);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateLastOnline_missingUser() {
        when(userRepository.findById(H1)).thenReturn(Optional.empty());
        userService.updateLastOnline(SID);
        verify(userRepository, never()).save(any());
    }

    @Test
    void mapPresetAvatarIds_batchTooLarge() {
        var ids = IntStream.rangeClosed(0, 100)
                .mapToObj(i -> String.format("76561198%09d", i))
                .toList();
        assertThat(ids).hasSize(101);
        assertThatThrownBy(() -> userService.mapPresetAvatarIdsBySteamIdForViewer(ids))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapPresetAvatarIds_invalidSteamInBatch() {
        assertThatThrownBy(() -> userService.mapPresetAvatarIdsBySteamIdForViewer(List.of("bad")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapPresetAvatarIds_unknownUser_nullValue() {
        when(userRepository.findById(H1)).thenReturn(Optional.empty());
        assertThat(userService.mapPresetAvatarIdsBySteamIdForViewer(List.of(SID))).containsEntry(SID, null);
    }

    @Test
    void resolveEffectiveAvatarUrl() {
        var u = new User(H1);
        u.setSelectedPresetAvatarId(2);
        assertThat(userService.resolveEffectiveAvatarUrl(u)).isEqualTo("http://avatar/1");
        verify(presetAvatarService).publicUrlFor(2);
    }

    @Test
    void normalizeDisplayName_invalid() {
        var u = new User(H1);
        u.setSteamId64(SID);
        u.setDisplayName("Valid_Nm");
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> userService.updateDisplayName(SID, "a!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reportUser_reasonRequired() {
        assertThatThrownBy(() -> userService.reportUser(SID, SID2, "  ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapPrivateProfileBySteamId_emptyInput() {
        assertThat(userService.mapPrivateProfileBySteamId(List.of())).isEmpty();
    }

    @Test
    void mapProfileLabelBySteamId_unknownUser() {
        when(userRepository.findById(H1)).thenReturn(Optional.empty());
        assertThat(userService.mapProfileLabelBySteamId(List.of(SID))).isEmpty();
    }

    @Test
    void getBySteamId_delegates() {
        var u = new User(H1);
        when(userRepository.findById(H1)).thenReturn(Optional.of(u));
        assertThat(userService.getBySteamId(SID)).contains(u);
    }

    @Test
    void matchesSteamIdFormat() {
        assertThat(userService.matchesSteamIdFormat(SID)).isTrue();
        assertThat(userService.matchesSteamIdFormat("abc")).isFalse();
    }

    @Test
    void listReportsForAdmin_clampsSize() {
        when(userReportRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(
                new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));
        userService.listReportsForAdmin(-1, 500);
        var cap = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(userReportRepository).findAllByOrderByCreatedAtDesc(cap.capture());
        assertThat(cap.getValue().getPageSize()).isEqualTo(100);
    }
}
