package com.skinsshowcase.auth.dto;

import java.util.Map;

/**
 * Ответ POST /auth/internal/users/profile-labels: отображаемое имя из БД (display_name, иначе persona_name).
 */
public record InternalProfileLabelsResponseDto(Map<String, String> labelBySteamId) {
}
