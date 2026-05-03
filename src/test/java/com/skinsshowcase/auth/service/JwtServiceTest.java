package com.skinsshowcase.auth.service;

import com.skinsshowcase.auth.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET_32 = "0123456789abcdef0123456789abcdef";

    private static JwtProperties props(long expirationMs) {
        var p = new JwtProperties();
        p.setSecret(SECRET_32);
        p.setExpirationMs(expirationMs);
        return p;
    }

    @Test
    void createToken_roundTripSubject() {
        var service = new JwtService(props(60_000L));
        var token = service.createToken("76561198000000001");

        assertThat(service.parseSubject(token)).isEqualTo("76561198000000001");
        assertThat(service.isValid(token)).isTrue();
    }

    @Test
    void isValid_falseForNullBlankAndTampered() {
        var service = new JwtService(props(60_000L));

        assertThat(service.isValid(null)).isFalse();
        assertThat(service.isValid("   ")).isFalse();
        assertThat(service.isValid("not.a.jwt")).isFalse();

        var token = service.createToken("76561198000000001");
        var tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");
        assertThat(service.isValid(tampered)).isFalse();
    }

    @Test
    void parseSubject_throwsWhenExpired() throws InterruptedException {
        var service = new JwtService(props(1L));
        var token = service.createToken("76561198000000001");
        Thread.sleep(5);

        assertThatThrownBy(() -> service.parseSubject(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void constructor_rejectsEmptyOrShortSecret() {
        var empty = new JwtProperties();
        empty.setSecret("");

        assertThatThrownBy(() -> new JwtService(empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secret");

        var shortSecret = new JwtProperties();
        shortSecret.setSecret("short");

        assertThatThrownBy(() -> new JwtService(shortSecret))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32");
    }
}
