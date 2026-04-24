package com.skinsshowcase.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamProfileSummaryDto(
        String steamId,
        String personaName,
        String avatarUrl,
        String avatarMediumUrl
) {
}
