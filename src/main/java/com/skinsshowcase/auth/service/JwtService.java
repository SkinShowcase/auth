package com.skinsshowcase.auth.service;

import com.skinsshowcase.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Создание и валидация JWT с subject = SteamID64.
 * Секрет из конфига (env), не хардкодится.
 */
@Slf4j
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES_HS256 = 32;

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = secretKeyFromString(properties.getSecret());
    }

    /**
     * Создаёт JWT с subject = steamId, expiration из конфига.
     */
    public String createToken(String steamId) {
        var now = new Date();
        var exp = new Date(now.getTime() + properties.getExpirationMs());
        return Jwts.builder()
                .subject(steamId)
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Парсит JWT и возвращает subject (SteamID64).
     *
     * @return SteamID64 при валидном токене
     * @throws JwtException при истёкшем или невалидном токене
     */
    public String parseSubject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Проверяет, что токен валиден (подпись и срок). Не бросает — возвращает true/false.
     */
    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.debug("JWT validation failed");
            return false;
        }
    }

    private static SecretKey secretKeyFromString(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be empty");
        }
        var bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES_HS256) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least " + MIN_SECRET_BYTES_HS256 + " bytes for HS256");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
