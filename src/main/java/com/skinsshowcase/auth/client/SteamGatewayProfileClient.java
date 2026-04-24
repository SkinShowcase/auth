package com.skinsshowcase.auth.client;

import com.skinsshowcase.auth.config.AuthSteamGatewayProperties;
import com.skinsshowcase.auth.dto.SteamProfileSummaryDto;
import com.skinsshowcase.auth.metrics.AuthMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;

@Component
public class SteamGatewayProfileClient {

    private static final Logger log = LoggerFactory.getLogger(SteamGatewayProfileClient.class);
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(8);

    private final WebClient webClient;
    private final AuthMetrics authMetrics;

    public SteamGatewayProfileClient(AuthSteamGatewayProperties properties, AuthMetrics authMetrics) {
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
        this.authMetrics = authMetrics;
    }

    public Optional<SteamProfileSummaryDto> fetchPlayerSummary(String steamId64) {
        var sample = authMetrics.startProfileFetchSample();
        try {
            var dto = webClient.get()
                    .uri("/internal/v1/steam/profile/{steamId}", steamId64)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(SteamProfileSummaryDto.class)
                    .block(BLOCK_TIMEOUT);
            return authMetrics.finishProfileFetch(sample, dto);
        } catch (WebClientResponseException.NotFound e) {
            authMetrics.recordProfileFetchNotFound(sample);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Steam gateway profile request failed: {}", e.getClass().getSimpleName());
            authMetrics.recordProfileFetchError(sample);
            return Optional.empty();
        }
    }
}
