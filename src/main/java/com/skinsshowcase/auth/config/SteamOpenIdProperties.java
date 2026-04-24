package com.skinsshowcase.auth.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Настройки Steam OpenID: realm, return_to (callback), endpoint.
 * realm и return_to должны совпадать по схеме/домену с фактическим хостом приложения.
 */
@Component
@Validated
@Getter
public class SteamOpenIdProperties {

    private final String openIdNs;
    private final String identifierSelect;
    private final String realm;
    private final String returnTo;
    private final String openIdEndpoint;

    public SteamOpenIdProperties(
            @NotBlank @Value("${auth.steam.open-id-ns}") String openIdNs,
            @NotBlank @Value("${auth.steam.identifier-select}") String identifierSelect,
            @NotBlank @Value("${auth.steam.realm}") String realm,
            @NotBlank @Value("${auth.steam.return-to}") String returnTo,
            @NotBlank @Value("${auth.steam.open-id-endpoint}") String openIdEndpoint) {
        this.openIdNs = openIdNs;
        this.identifierSelect = identifierSelect;
        this.realm = realm;
        this.returnTo = returnTo;
        this.openIdEndpoint = openIdEndpoint;
    }
}
