package com.skinsshowcase.auth.dto;

import java.util.List;

public record AdminReportsPageDto(
        List<UserReportResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
