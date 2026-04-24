package com.skinsshowcase.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Ответ GET /auth/users/{steamId}/trade-link.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TradeLinkResponseDto(String tradeUrl) {
}
