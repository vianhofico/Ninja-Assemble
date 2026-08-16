package com.ninjaassemble.mail.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerMailTest {
    @Test
    void mailCanOnlyBeClaimedBeforeExpiryAndOnce() {
        Instant now = Instant.parse("2026-08-16T00:00:00Z");
        PlayerMail mail = new PlayerMail(UUID.randomUUID(), "mail.subject", "mail.body", List.of(new MailAttachment("CURRENCY", "GOLD", 10)), now, now.plusSeconds(60), false, false);
        PlayerMail claimed = mail.claim(now.plusSeconds(30));
        assertTrue(claimed.claimed());
        assertThrows(IllegalStateException.class, () -> claimed.claim(now.plusSeconds(40)));
        assertThrows(IllegalStateException.class, () -> mail.claim(now.plusSeconds(60)));
    }
}
