package com.ninjaassemble.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public final class SessionTokenService {
    private static final String HMAC = "HmacSHA256";
    private final byte[] secret;
    private final Clock clock;
    private final long ttlSeconds;

    public SessionTokenService(@Value("${game.auth.session-secret:dev-only-change-me-before-production}") String secret,
                               Clock clock,
                               @Value("${game.auth.session-ttl-seconds:2592000}") long ttlSeconds) {
        if (secret == null || secret.length() < 16) throw new IllegalArgumentException("session secret must contain at least 16 characters");
        if (ttlSeconds <= 0) throw new IllegalArgumentException("session ttl must be positive");
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(UUID playerId) {
        if (playerId == null) throw new IllegalArgumentException("playerId is required");
        long expiresAt = Math.addExact(clock.instant().getEpochSecond(), ttlSeconds);
        String payload = playerId + "." + expiresAt;
        String body = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(body));
    }

    public boolean verify(String token, UUID expectedPlayerId) {
        if (token == null || token.isBlank() || expectedPlayerId == null) return false;
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) return false;
        try {
            byte[] supplied = Base64.getUrlDecoder().decode(parts[1]);
            byte[] expected = sign(parts[0]);
            if (!MessageDigest.isEqual(supplied, expected)) return false;
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            int separator = payload.lastIndexOf('.');
            if (separator <= 0 || separator == payload.length() - 1) return false;
            UUID playerId = UUID.fromString(payload.substring(0, separator));
            long expiresAt = Long.parseLong(payload.substring(separator + 1));
            return expectedPlayerId.equals(playerId) && Instant.ofEpochSecond(expiresAt).isAfter(clock.instant());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] sign(String body) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret, HMAC));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }
}
