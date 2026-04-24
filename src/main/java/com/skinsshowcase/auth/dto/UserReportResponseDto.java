package com.skinsshowcase.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record UserReportResponseDto(
        UUID id,
        String reporterSteamId,
        String reportedSteamId,
        String reason,
        String details,
        Instant createdAt
) {
}
