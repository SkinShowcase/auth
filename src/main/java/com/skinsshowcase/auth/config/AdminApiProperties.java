package com.skinsshowcase.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ключ для HTTP-заголовка {@code X-Admin-Api-Key} на эндпоинтах {@code /auth/admin/**}.
 */
@ConfigurationProperties(prefix = "admin")
public class AdminApiProperties {

    /**
     * Пустое значение — админ-API отключён (503).
     */
    private String apiKey = "";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey != null ? apiKey : "";
    }
}
