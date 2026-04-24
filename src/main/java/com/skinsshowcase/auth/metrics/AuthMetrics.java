package com.skinsshowcase.auth.metrics;

import com.skinsshowcase.auth.dto.SteamProfileSummaryDto;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Метрики auth: вызовы steam-gateway для профиля, создание/существующий пользователь, обновление профиля.
 */
@Component
public class AuthMetrics {

    private final MeterRegistry registry;
    private final Timer profileFetchLatency;

    public AuthMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.profileFetchLatency = Timer.builder("auth.steam_gateway.profile.latency")
                .description("Latency of internal steam-gateway profile fetch")
                .register(registry);
    }

    public Timer.Sample startProfileFetchSample() {
        return Timer.start(registry);
    }

    public Optional<SteamProfileSummaryDto> finishProfileFetch(Timer.Sample sample, SteamProfileSummaryDto dto) {
        if (dto == null) {
            recordProfileFetchOutcome(sample, "empty");
            return Optional.empty();
        }
        recordProfileFetchOutcome(sample, "success");
        return Optional.of(dto);
    }

    public void recordProfileFetchNotFound(Timer.Sample sample) {
        recordProfileFetchOutcome(sample, "not_found");
    }

    public void recordProfileFetchError(Timer.Sample sample) {
        recordProfileFetchOutcome(sample, "error");
    }

    private void recordProfileFetchOutcome(Timer.Sample sample, String outcome) {
        sample.stop(profileFetchLatency);
        registry.counter("auth.steam_gateway.profile.requests",
                "outcome", outcome).increment();
    }

    public void recordUserEnsure(String outcome) {
        registry.counter("auth.users.ensure",
                "outcome", outcome).increment();
    }

    public void recordProfileRefresh(String outcome) {
        registry.counter("auth.users.profile_refresh",
                "outcome", outcome).increment();
    }
}
