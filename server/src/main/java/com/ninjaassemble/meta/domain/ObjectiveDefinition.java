package com.ninjaassemble.meta.domain;

public record ObjectiveDefinition(ObjectiveType type, String qualifier, long target) {
    public ObjectiveDefinition {
        if (type == null || target <= 0) throw new IllegalArgumentException("invalid objective");
    }
}
