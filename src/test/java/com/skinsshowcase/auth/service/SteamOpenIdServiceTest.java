package com.skinsshowcase.auth.service;

import com.skinsshowcase.auth.config.SteamOpenIdProperties;
import com.skinsshowcase.auth.exception.SteamAuthException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SteamOpenIdServiceTest {

    private static SteamOpenIdProperties propsForServer(MockWebServer server) {
        var base = server.url("/").toString();
        return new SteamOpenIdProperties(
                "http://specs.openid.net/auth/2.0",
                "http://specs.openid.net/auth/2.0/identifier_select",
                "https://app.example/",
                "https://app.example/auth/steam/callback",
                base);
    }

    @Test
    void buildRedirectUrl_containsOpenIdParams() {
        var props = new SteamOpenIdProperties(
                "ns", "id_sel", "realm", "return_to", "https://steam.example/openid");
        var svc = new SteamOpenIdService(props, WebClient.builder());
        var url = svc.buildRedirectUrl();
        assertThat(url).contains("https://steam.example/openid?");
        assertThat(url).contains("openid.mode=checkid_setup");
        assertThat(url).contains("openid.return_to=");
    }

    @Test
    void validateAndExtractSteamId_success() throws Exception {
        try (var server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setBody("ns:1\nis_valid:true\n"));
            var props = propsForServer(server);
            var svc = new SteamOpenIdService(props, WebClient.builder());

            var params = new HashMap<String, String>();
            params.put("openid.mode", "id_res");
            params.put("openid.claimed_id", "https://steamcommunity.com/openid/id/76561198000000001");

            assertThat(svc.validateAndExtractSteamId(params)).isEqualTo("76561198000000001");
        }
    }

    @Test
    void validateAndExtractSteamId_missingParams() {
        var props = new SteamOpenIdProperties("ns", "id", "r", "rt", "http://localhost");
        var svc = new SteamOpenIdService(props, WebClient.builder());
        assertThatThrownBy(() -> svc.validateAndExtractSteamId(Map.of()))
                .isInstanceOf(SteamAuthException.class);
    }

    @Test
    void validateAndExtractSteamId_invalidSteamResponse() throws Exception {
        try (var server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setBody("is_valid:false\n"));
            var props = propsForServer(server);
            var svc = new SteamOpenIdService(props, WebClient.builder());
            var params = Map.of(
                    "openid.claimed_id", "https://steamcommunity.com/openid/id/76561198000000001");
            assertThatThrownBy(() -> svc.validateAndExtractSteamId(params))
                    .isInstanceOf(SteamAuthException.class);
        }
    }

    @Test
    void validateAndExtractSteamId_badClaimedId() throws Exception {
        try (var server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setBody("is_valid:true\n"));
            var props = propsForServer(server);
            var svc = new SteamOpenIdService(props, WebClient.builder());
            var params = Map.of("openid.claimed_id", "https://evil.example/id/1");
            assertThatThrownBy(() -> svc.validateAndExtractSteamId(params))
                    .isInstanceOf(SteamAuthException.class);
        }
    }
}
