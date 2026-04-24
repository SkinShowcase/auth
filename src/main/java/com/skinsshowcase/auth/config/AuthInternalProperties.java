package com.skinsshowcase.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Вызовы между сервисами: /auth/internal/**. Если service-key не задан — проверка заголовка отключена (только для локальной разработки).
 */
@Data
@ConfigurationProperties(prefix = "auth.internal")
public class AuthInternalProperties {

    /**
     * Ожидаемое значение заголовка X-Internal-Service-Key. Env: AUTH_INTERNAL_SERVICE_KEY.
     */
    private String serviceKey = "";
}
