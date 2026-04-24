package com.skinsshowcase.auth.controller;

import com.skinsshowcase.auth.config.AuthInternalProperties;
import com.skinsshowcase.auth.dto.InternalPrivacyFlagsRequestDto;
import com.skinsshowcase.auth.dto.InternalPrivacyFlagsResponseDto;
import com.skinsshowcase.auth.dto.InternalProfileLabelsRequestDto;
import com.skinsshowcase.auth.dto.InternalProfileLabelsResponseDto;
import com.skinsshowcase.auth.dto.PrivacyResponseDto;
import com.skinsshowcase.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;

/**
 * Внутренние вызовы (trades и др.): приватность профиля. Защита опциональным X-Internal-Service-Key.
 */
@RestController
@Validated
@RequestMapping("/auth/internal/users")
public class AuthInternalUserController {

    private static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";

    private final UserService userService;
    private final AuthInternalProperties internalProperties;

    public AuthInternalUserController(UserService userService, AuthInternalProperties internalProperties) {
        this.userService = userService;
        this.internalProperties = internalProperties;
    }

    @GetMapping("/{steamId}/privacy")
    public ResponseEntity<PrivacyResponseDto> getPrivacy(
            @PathVariable String steamId,
            @RequestHeader(value = INTERNAL_KEY_HEADER, required = false) String serviceKey) {
        assertInternalServiceKey(serviceKey);
        if (!userService.matchesSteamIdFormat(steamId)) {
            return ResponseEntity.badRequest().build();
        }
        var user = userService.getBySteamId(steamId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new PrivacyResponseDto(user.isPrivateProfile()));
    }

    @PostMapping("/privacy-flags")
    public InternalPrivacyFlagsResponseDto privacyFlags(
            @RequestHeader(value = INTERNAL_KEY_HEADER, required = false) String serviceKey,
            @Valid @RequestBody InternalPrivacyFlagsRequestDto body) {
        assertInternalServiceKey(serviceKey);
        var unique = dedupeSteamIds(body.steamIds());
        var map = userService.mapPrivateProfileBySteamId(unique);
        return new InternalPrivacyFlagsResponseDto(map);
    }

    @PostMapping("/profile-labels")
    public InternalProfileLabelsResponseDto profileLabels(
            @RequestHeader(value = INTERNAL_KEY_HEADER, required = false) String serviceKey,
            @Valid @RequestBody InternalProfileLabelsRequestDto body) {
        assertInternalServiceKey(serviceKey);
        var unique = dedupeSteamIds(body.steamIds());
        var map = userService.mapProfileLabelBySteamId(unique);
        return new InternalProfileLabelsResponseDto(map);
    }

    private static LinkedHashSet<String> dedupeSteamIds(java.util.List<String> steamIds) {
        var out = new LinkedHashSet<String>();
        if (steamIds == null) {
            return out;
        }
        for (var sid : steamIds) {
            if (sid != null && !sid.isBlank()) {
                out.add(sid.trim());
            }
        }
        return out;
    }

    private void assertInternalServiceKey(String provided) {
        var expected = internalProperties.getServiceKey();
        if (expected == null || expected.isBlank()) {
            return;
        }
        if (!expected.equals(provided)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
