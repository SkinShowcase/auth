package com.skinsshowcase.auth.dto;

import java.util.List;

/**
 * Тело POST /auth/users/preset-avatar-ids — список Steam ID собеседников (для чатов и т.п.).
 */
public record BatchSteamIdsRequestDto(List<String> steamIds) {
}
