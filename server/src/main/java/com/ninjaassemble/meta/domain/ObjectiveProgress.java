package com.ninjaassemble.meta.domain;

public record ObjectiveProgress(ObjectiveDefinition definition, long current, boolean claimed) {
    public ObjectiveProgress {
        if (definition == null || current < 0) throw new IllegalArgumentException("invalid objective progress");
    }

    public ObjectiveProgress add(long delta) {
        if (delta <= 0) throw new IllegalArgumentException("progress delta must be positive");
        return new ObjectiveProgress(definition, Math.min(definition.target(), Math.addExact(current, delta)), claimed);
    }

    public boolean complete() { return current >= definition.target(); }

    public ObjectiveProgress claim() {
        if (!complete()) throw new IllegalStateException("objective not complete");
        if (claimed) throw new IllegalStateException("objective already claimed");
        return new ObjectiveProgress(definition, current, true);
    }
}
