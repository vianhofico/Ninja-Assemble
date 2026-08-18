package com.ninjaassemble.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionTokenServiceTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final UUID PLAYER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void issuedTokenIsBoundToPlayerAndSignature() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-18T00:00:00Z"), ZoneOffset.UTC);
        SessionTokenService service = new SessionTokenService(SECRET, clock, 300);
        String token = service.issue(PLAYER);
        assertTrue(service.verify(token, PLAYER));
        assertFalse(service.verify(token, UUID.fromString("22222222-2222-2222-2222-222222222222")));
        assertFalse(service.verify(token + "x", PLAYER));
    }

    @Test
    void expiredTokenIsRejected() {
        Clock issuedAt = Clock.fixed(Instant.parse("2026-08-18T00:00:00Z"), ZoneOffset.UTC);
        String token = new SessionTokenService(SECRET, issuedAt, 60).issue(PLAYER);
        Clock expiredAt = Clock.fixed(Instant.parse("2026-08-18T00:01:01Z"), ZoneOffset.UTC);
        assertFalse(new SessionTokenService(SECRET, expiredAt, 60).verify(token, PLAYER));
    }
}
