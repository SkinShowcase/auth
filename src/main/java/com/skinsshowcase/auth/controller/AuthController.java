package com.skinsshowcase.auth.controller;

import com.skinsshowcase.auth.config.FrontendRedirectProperties;
import com.skinsshowcase.auth.dto.UpdatePresetAvatarRequestDto;
import com.skinsshowcase.auth.dto.BatchPresetAvatarResponseDto;
import com.skinsshowcase.auth.dto.BatchSteamIdsRequestDto;
import com.skinsshowcase.auth.dto.MeResponseDto;
import com.skinsshowcase.auth.dto.PrivacyResponseDto;
import com.skinsshowcase.auth.dto.ReportUserRequestDto;
import com.skinsshowcase.auth.dto.TradeLinkResponseDto;
import com.skinsshowcase.auth.dto.UpdateDisplayNameRequestDto;
import com.skinsshowcase.auth.dto.UpdateTradeLinkRequestDto;
import com.skinsshowcase.auth.dto.UserSteamIdResponseDto;
import com.skinsshowcase.auth.entity.User;
import com.skinsshowcase.auth.exception.AccountBlockedException;
import com.skinsshowcase.auth.service.JwtService;
import com.skinsshowcase.auth.service.SteamOpenIdService;
import com.skinsshowcase.auth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Авторизация через Steam OpenID.
 * Фронт НЕ передаёт Steam ID — пользователь редиректится на /auth/steam, затем Steam возвращает на callback.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final SteamOpenIdService steamOpenIdService;
    private final JwtService jwtService;
    private final FrontendRedirectProperties frontendRedirect;
    private final UserService userService;

    public AuthController(SteamOpenIdService steamOpenIdService,
                          JwtService jwtService,
                          FrontendRedirectProperties frontendRedirect,
                          UserService userService) {
        this.steamOpenIdService = steamOpenIdService;
        this.jwtService = jwtService;
        this.frontendRedirect = frontendRedirect;
        this.userService = userService;
    }

    /**
     * Редирект на Steam OpenID. Пользователь нажимает "Войти через Steam" и переходит сюда.
     */
    @GetMapping("/steam")
    public ResponseEntity<Void> loginSteam() {
        var redirectUrl = steamOpenIdService.buildRedirectUrl();
        return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(redirectUrl)).build();
    }

    /**
     * Callback от Steam после логина. Проверяем ответ у Steam, выдаём JWT, редирект на фронт с токеном в fragment.
     */
    @GetMapping("/steam/callback")
    public ResponseEntity<Void> steamCallback(HttpServletRequest request) {
        var params = extractOpenIdParams(request);
        var steamId = steamOpenIdService.validateAndExtractSteamId(params);
        userService.getOrCreate(steamId);
        if (userService.isUserBlocked(steamId)) {
            var blockedUrl = frontendRedirect.getRedirectUrl() + "#error=account_blocked";
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(blockedUrl)).build();
        }
        userService.refreshSteamProfileFromGateway(steamId);
        var token = jwtService.createToken(steamId);
        var redirectTo = buildRedirectWithToken(token);
        return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(redirectTo)).build();
    }

    /**
     * Лёгкая проверка JWT и отсутствия блокировки (для API Gateway и real-time подключений).
     * 204 — сессия активна; 401 — нет/невалидный токен; 403 — аккаунт заблокирован.
     */
    @GetMapping("/session")
    public ResponseEntity<Void> session(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        var steamId = resolveSteamIdFromAuthHeader(authorization);
        if (steamId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        throwIfUserBlocked(steamId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Текущий пользователь по JWT (Authorization: Bearer &lt;token&gt;).
     */
    @GetMapping("/me")
    public ResponseEntity<MeResponseDto> me(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        var steamId = resolveSteamIdFromAuthHeader(authorization);
        if (steamId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        throwIfUserBlocked(steamId);
        var user = userService.getOrCreate(steamId);
        return ResponseEntity.ok(toMeResponse(steamId, user));
    }

    /**
     * Получить текущую настройку приватности профиля.
     */
    @GetMapping("/me/privacy")
    public ResponseEntity<PrivacyResponseDto> getPrivacy(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        var steamId = resolveSteamIdFromAuthHeader(authorization);
        if (steamId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        throwIfUserBlocked(steamId);
        var user = userService.getOrCreate(steamId);
        return ResponseEntity.ok(new PrivacyResponseDto(user.isPrivateProfile()));
    }

    /**
     * Переключить приватность профиля: true → false, false → true. Тело запроса не требуется.
     */
    @PatchMapping("/me/privacy")
    public ResponseEntity<Void> updatePrivacy(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        var steamId = resolveSteamIdFromAuthHeader(authorization);
        if (steamId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        throwIfUserBlocked(steamId);
        var user = userService.getOrCreate(steamId);
        userService.setPrivacy(steamId, !user.isPrivateProfile());
        return ResponseEntity.noContent().build();
    }

    /**
     * Обновить время последнего онлайна в приложении (heartbeat).
     */
    @PatchMapping("/me/last-online")
    public ResponseEntity<Void> updateLastOnline(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        var steamId = resolveSteamIdFromAuthHeader(authorization);
        if (steamId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        throwIfUserBlocked(steamId);
        userService.updateLastOnline(steamId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Задать или сбросить Steam trade-ссылку текущего пользователя. Тело: {@code {"tradeUrl":"https://..."}} или {@code {"tradeUrl":""}} / null для сброса.
     */
    @PatchMapping("/me/trade-link")
    public ResponseEntity<MeResponseDto> updateTradeLink(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody UpdateTradeLinkRequestDto body) {
        var steamId = resolveSteamIdFromAuthHeader(authorization);
        if (steamId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        throwIfUserBlocked(steamId);
        userService.updateSteamTradeLink(steamId, body.tradeUrl());
        var user = userService.getOrCreate(steamId);
        return ResponseEntity.ok(toMeResponse(steamId, user));
    }

    /**
     * Трейд-ссылка пользователя по Steam ID. Нужен JWT. При приватном профиле (и чужой просмотр) — 404. Невалидный формат steamId — 400.
     */
    @GetMapping("/users/{steamId}/trade-link")
    public ResponseEntity<TradeLinkResponseDto> getUserTradeLink(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String steamId) {
        var viewerSteamId = resolveSteamIdFromAuthHeader(authorization);
        if (viewerSteamId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        throwIfUserBlocked(viewerSteamId);
        if (!userService.matchesSteamIdFormat(steamId)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.of(userService.getTradeLinkForViewer(viewerSteamId, steamId));
    }

    /**
     * Найти пользователя по имени (Steam ID или persona name). Для резолва в чатах и др.
     * При приватном профиле цели чужой пользователь не находится (404), как для trade-link.
     */
    @GetMapping("/users/by-username/{username}")
    public ResponseEntity<UserSteamIdResponseDto> getByUsername(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String username) {
        var viewerSteamId = resolveSteamIdFromAuthHeader(authorization);
        if (viewerSteamId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        throwIfUserBlocked(viewerSteamId);
        return ResponseEntity.of(userService.getSteamIdByUsernameForViewer(viewerSteamId, username)
                .map(UserSteamIdResponseDto::new));
    }

    /**
     * Пакетно: id пресетной аватарки (1–8) для списка пользователей. Нужен JWT. Используется messaging для GET /api/chats.
     */
    @PostMapping("/users/preset-avatar-ids")
    public ResponseEntity<BatchPresetAvatarResponseDto> batchPresetAvatarIds(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) BatchSteamIdsRequestDto body) {
        var viewerSteamId = resolveSteamIdFromAuthHeader(authorization);
        if (viewerSteamId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        throwIfUserBlocked(viewerSteamId);
        var steamIds = body != null ? body.steamIds() : null;
        var map = userService.mapPresetAvatarIdsBySteamIdForViewer(steamIds);
        return ResponseEntity.ok(new BatchPresetAvatarResponseDto(map));
    }

    @PatchMapping("/me/display-name")
    public ResponseEntity<MeResponseDto> updateDisplayName(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody UpdateDisplayNameRequestDto body) {
        var steamId = resolveSteamIdFromAuthHeader(authorization);
        if (steamId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        throwIfUserBlocked(steamId);
        userService.updateDisplayName(steamId, body.displayName());
        var user = userService.getOrCreate(steamId);
        return ResponseEntity.ok(toMeResponse(steamId, user));
    }

    @PostMapping("/users/{steamId}/report")
    public ResponseEntity<Void> reportUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String steamId,
            @RequestBody ReportUserRequestDto body) {
        var reporterSteamId = resolveSteamIdFromAuthHeader(authorization);
        if (reporterSteamId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        throwIfUserBlocked(reporterSteamId);
        if (!userService.matchesSteamIdFormat(steamId)) {
            return ResponseEntity.badRequest().build();
        }
        userService.reportUser(reporterSteamId, steamId, body.reason(), body.details());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/avatar")
    public ResponseEntity<MeResponseDto> updateAvatar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody UpdatePresetAvatarRequestDto body) {
        var steamId = resolveSteamIdFromAuthHeader(authorization);
        if (steamId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        throwIfUserBlocked(steamId);
        userService.updatePresetAvatar(steamId, body.presetAvatarId());
        var user = userService.getOrCreate(steamId);
        return ResponseEntity.ok(toMeResponse(steamId, user));
    }

    private MeResponseDto toMeResponse(String steamId, User user) {
        return new MeResponseDto(
                steamId,
                user.getDisplayName(),
                user.isPrivateProfile(),
                user.getSuccessfulTradesCount(),
                user.getLastOnlineAt(),
                user.getSteamTradeLink(),
                userService.resolveEffectiveAvatarUrl(user),
                user.getSelectedPresetAvatarId(),
                user.getAvatarSource(),
                user.isBlocked()
        );
    }

    private void throwIfUserBlocked(String steamId) {
        if (userService.isUserBlocked(steamId)) {
            throw new AccountBlockedException();
        }
    }

    private Map<String, String> extractOpenIdParams(HttpServletRequest request) {
        return Collections.list(request.getParameterNames()).stream()
                .filter(name -> name != null && name.startsWith("openid."))
                .collect(Collectors.toMap(name -> name, request::getParameter));
    }

    /** Токен в fragment (#token=...), чтобы не попадал в query и логи. redirectUrl в конфиге — без fragment. */
    private String buildRedirectWithToken(String token) {
        return frontendRedirect.getRedirectUrl() + "#token=" + token;
    }

    private String resolveSteamIdFromAuthHeader(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        var token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return null;
        }
        try {
            return jwtService.parseSubject(token);
        } catch (Exception e) {
            logger.debug("Invalid or expired token: {}", e.getMessage());
            return null;
        }
    }
}
