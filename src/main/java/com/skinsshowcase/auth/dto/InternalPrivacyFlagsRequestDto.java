package com.skinsshowcase.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InternalPrivacyFlagsRequestDto(
        @NotNull
        @Size(max = 200)
        List<String> steamIds
) {
}
