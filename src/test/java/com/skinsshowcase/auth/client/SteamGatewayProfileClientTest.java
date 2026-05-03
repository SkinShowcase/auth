package com.skinsshowcase.auth.client;

import com.skinsshowcase.auth.config.AuthSteamGatewayProperties;
import com.skinsshowcase.auth.dto.SteamProfileSummaryDto;
import com.skinsshowcase.auth.metrics.AuthMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SteamGatewayProfileClientTest {

    @Test
    void fetchPlayerSummary_success() throws Exception {
        try (var server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse()
                    .setBody("{\"steamId\":\"76561198000000001\",\"personaName\":\"X\"}")
                    .addHeader("Content-Type", "application/json"));
            var base = server.url("/").toString().replaceAll("/$", "");
            var props = new AuthSteamGatewayProperties(base);
            var client = new SteamGatewayProfileClient(props, new AuthMetrics(new SimpleMeterRegistry()));

            var out = client.fetchPlayerSummary("76561198000000001");
            assertThat(out).isPresent();
            assertThat(out.get().personaName()).isEqualTo("X");
        }
    }

    @Test
    void fetchPlayerSummary_notFound_empty() throws Exception {
        try (var server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setResponseCode(404));
            var base = server.url("/").toString().replaceAll("/$", "");
            var props = new AuthSteamGatewayProperties(base);
            var client = new SteamGatewayProfileClient(props, new AuthMetrics(new SimpleMeterRegistry()));

            assertThat(client.fetchPlayerSummary("76561198000000001")).isEmpty();
        }
    }
}
