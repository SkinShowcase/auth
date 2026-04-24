package com.skinsshowcase.auth.service;

import com.skinsshowcase.auth.config.SteamOpenIdProperties;
import com.skinsshowcase.auth.exception.SteamAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Steam OpenID 2.0: построение URL редиректа и проверка ответа Steam (check_authentication).
 * SteamID никогда не принимается с клиента — только после проверки у Steam.
 */
@Service
public class SteamOpenIdService {

    private static final Logger logger = LoggerFactory.getLogger(SteamOpenIdService.class);
    private static final String MODE_CHECKID_SETUP = "checkid_setup";
    private static final String MODE_CHECK_AUTHENTICATION = "check_authentication";
    private static final String NS = "openid.ns";
    private static final String MODE = "openid.mode";
    private static final String RETURN_TO = "openid.return_to";
    private static final String REALM = "openid.realm";
    private static final String IDENTITY = "openid.identity";
    private static final String CLAIMED_ID = "openid.claimed_id";
    private static final Pattern STEAM_ID_PATTERN = Pattern.compile(
            "https?://steamcommunity\\.com/openid/id/(\\d+)");

    private final SteamOpenIdProperties properties;
    private final WebClient webClient;

    public SteamOpenIdService(SteamOpenIdProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Строит URL для редиректа пользователя на Steam OpenID.
     */
    public String buildRedirectUrl() {
        var params = new LinkedMultiValueMap<String, String>();
        params.add(NS, properties.getOpenIdNs());
        params.add(MODE, MODE_CHECKID_SETUP);
        params.add(RETURN_TO, properties.getReturnTo());
        params.add(REALM, properties.getRealm());
        params.add(IDENTITY, properties.getIdentifierSelect());
        params.add(CLAIMED_ID, properties.getIdentifierSelect());

        var query = params.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(v -> e.getKey() + "=" + encode(v)))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
        return properties.getOpenIdEndpoint() + "?" + query;
    }

    /**
     * Отправляет параметры callback в Steam для проверки (check_authentication).
     * Возвращает SteamID64 при успехе.
     *
     * @param callbackParams параметры openid.* из query callback
     * @return SteamID64 (17-значный)
     * @throws SteamAuthException если Steam вернул is_valid:false или нет claimed_id
     */
    public String validateAndExtractSteamId(Map<String, String> callbackParams) {
        if (callbackParams == null || callbackParams.isEmpty()) {
            throw new SteamAuthException("Missing OpenID callback parameters");
        }
        var formData = buildCheckAuthenticationForm(callbackParams);
        var responseBody = postCheckAuthentication(formData);
        if (!isValidResponse(responseBody)) {
            logger.warn("Steam check_authentication failed: response does not contain is_valid:true");
            throw new SteamAuthException("Steam authentication validation failed");
        }
        var claimedId = callbackParams.get(CLAIMED_ID);
        var steamId = extractSteamIdFromClaimedId(claimedId);
        if (steamId == null) {
            logger.warn("Could not extract SteamID from claimed_id: {}", claimedId);
            throw new SteamAuthException("Invalid Steam claimed_id");
        }
        return steamId;
    }

    private MultiValueMap<String, String> buildCheckAuthenticationForm(Map<String, String> callbackParams) {
        var form = new LinkedMultiValueMap<String, String>();
        for (var entry : callbackParams.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                form.add(entry.getKey(), entry.getValue());
            }
        }
        form.set(MODE, MODE_CHECK_AUTHENTICATION);
        return form;
    }

    private String postCheckAuthentication(MultiValueMap<String, String> formData) {
        try {
            return webClient.post()
                    .uri(properties.getOpenIdEndpoint())
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            logger.warn("Steam check_authentication HTTP error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SteamAuthException("Steam authentication request failed", e);
        }
    }

    private static boolean isValidResponse(String responseBody) {
        if (responseBody == null) {
            return false;
        }
        return responseBody.contains("is_valid:true");
    }

    private static String extractSteamIdFromClaimedId(String claimedId) {
        if (claimedId == null) {
            return null;
        }
        var matcher = STEAM_ID_PATTERN.matcher(claimedId);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
