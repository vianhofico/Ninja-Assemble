package com.ninjaassemble.mail.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlayerMail(
        UUID id,
        String subjectKey,
        String bodyKey,
        List<MailAttachment> attachments,
        Instant createdAt,
        Instant expiresAt,
        boolean read,
        boolean claimed
) {
    public PlayerMail {
        if (id == null || subjectKey == null || subjectKey.isBlank() || bodyKey == null || bodyKey.isBlank() || attachments == null || createdAt == null) throw new IllegalArgumentException("invalid mail");
        attachments = List.copyOf(attachments);
        if (expiresAt != null && !expiresAt.isAfter(createdAt)) throw new IllegalArgumentException("invalid expiry");
    }

    public PlayerMail markRead() { return new PlayerMail(id, subjectKey, bodyKey, attachments, createdAt, expiresAt, true, claimed); }

    public PlayerMail claim(Instant now) {
        if (claimed) throw new IllegalStateException("mail already claimed");
        if (expiresAt != null && now != null && !now.isBefore(expiresAt)) throw new IllegalStateException("mail expired");
        return new PlayerMail(id, subjectKey, bodyKey, attachments, createdAt, expiresAt, true, true);
    }
}
