package com.skinsshowcase.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Тело PATCH /auth/me/trade-link. {@code tradeUrl}: валидная HTTPS-ссылка Steam trade или {@code null}/пустая строка для сброса.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UpdateTradeLinkRequestDto(String tradeUrl) {
}
