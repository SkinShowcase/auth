package com.skinsshowcase.auth.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AuthSteamGatewayProperties {

    private final String baseUrl;

    public AuthSteamGatewayProperties(@Value("${auth.steam-gateway.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
