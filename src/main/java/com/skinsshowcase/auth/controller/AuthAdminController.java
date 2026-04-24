package com.skinsshowcase.auth.controller;

import com.skinsshowcase.auth.config.AdminApiProperties;
import com.skinsshowcase.auth.dto.AdminReportsPageDto;
import com.skinsshowcase.auth.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Админ-операции по пользователям и жалобам. Защита: заголовок {@code X-Admin-Api-Key}.
 */
@RestController
@RequestMapping("/auth/admin")
public class AuthAdminController {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Api-Key";

    private final AdminApiProperties adminApiProperties;
    private final UserService userService;

    public AuthAdminController(AdminApiProperties adminApiProperties, UserService userService) {
        this.adminApiProperties = adminApiProperties;
        this.userService = userService;
    }

    @GetMapping("/reports")
    public ResponseEntity<AdminReportsPageDto> listReports(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireValidAdminKey(apiKey);
        var body = userService.listReportsForAdmin(page, size);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/users/{steamId}/block")
    public ResponseEntity<Void> blockUser(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @PathVariable String steamId) {
        requireValidAdminKey(apiKey);
        userService.setUserBlocked(steamId, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{steamId}/unblock")
    public ResponseEntity<Void> unblockUser(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @PathVariable String steamId) {
        requireValidAdminKey(apiKey);
        userService.setUserBlocked(steamId, false);
        return ResponseEntity.noContent().build();
    }

    private void requireValidAdminKey(String providedHeader) {
        var configured = adminApiProperties.getApiKey();
        if (configured == null || configured.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Admin API is not configured");
        }
        if (!constantTimeEqual(configured, providedHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin API key");
        }
    }

    private static boolean constantTimeEqual(String expected, String provided) {
        var a = expected.getBytes(StandardCharsets.UTF_8);
        var b = (provided == null ? "" : provided).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
