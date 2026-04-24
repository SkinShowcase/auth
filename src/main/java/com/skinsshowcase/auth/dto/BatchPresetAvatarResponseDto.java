package com.skinsshowcase.auth.dto;

import java.util.Map;

/**
 * Ответ POST /auth/users/preset-avatar-ids: для каждого Steam ID — id пресета (1–8) или null, если пользователь не найден.
 */
public record BatchPresetAvatarResponseDto(Map<String, Integer> presetAvatarIdBySteamId) {
}
