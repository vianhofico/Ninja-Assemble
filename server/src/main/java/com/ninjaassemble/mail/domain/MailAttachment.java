package com.ninjaassemble.mail.domain;

public record MailAttachment(String resourceType, String resourceId, long quantity) {
    public MailAttachment {
        if (resourceType == null || resourceType.isBlank() || resourceId == null || resourceId.isBlank() || quantity <= 0) throw new IllegalArgumentException("invalid mail attachment");
    }
}
