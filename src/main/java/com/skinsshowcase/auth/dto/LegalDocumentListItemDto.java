package com.skinsshowcase.auth.dto;

import java.time.Instant;

public record LegalDocumentListItemDto(
        String slug,
        int version,
        String title,
        Instant effectiveFrom
) {
}
