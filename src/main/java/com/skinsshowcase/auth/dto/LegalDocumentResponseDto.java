package com.skinsshowcase.auth.dto;

import java.time.Instant;

public record LegalDocumentResponseDto(
        String slug,
        int version,
        String title,
        String content,
        Instant effectiveFrom
) {
}
