package com.skinsshowcase.auth.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * URL фронта, куда редиректить пользователя после успешного логина через Steam.
 * Токен передаётся в fragment (#token=...) чтобы не светился в логах сервера.
 */
@Component
@Validated
@Getter
public class FrontendRedirectProperties {

    private final String redirectUrl;

    public FrontendRedirectProperties(@NotBlank @Value("${auth.frontend.redirect-url}") String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
}
