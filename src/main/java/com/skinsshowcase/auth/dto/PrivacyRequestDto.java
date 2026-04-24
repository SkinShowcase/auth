package com.skinsshowcase.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * Тело запроса PATCH /auth/me/privacy: включить/выключить приватность профиля.
 */
public record PrivacyRequestDto(@NotNull @JsonProperty("private") Boolean privateProfile) {
}
