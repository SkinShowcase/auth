package com.skinsshowcase.auth.dto;

/**
 * Ответ поиска пользователя по имени: только Steam ID (для резолва в messaging и др.).
 */
public record UserSteamIdResponseDto(String steamId) {
}
