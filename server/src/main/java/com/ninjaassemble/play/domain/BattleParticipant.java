package com.ninjaassemble.play.domain;

import com.ninjaassemble.battle.sim.TeamSide;

/**
 * Presentation metadata attached to an authoritative battle unit.
 *
 * <p>`heroId + awakened` is the production identity. `variant` is retained only as a legacy/fallback art hint for
 * enemy content that has not yet completed the Hero Version migration.</p>
 */
public record BattleParticipant(
        String battleUnitId,
        String characterId,
        String heroId,
        boolean awakened,
        String awakeningId,
        String presentationKey,
        String displayName,
        String variant,
        int level,
        TeamSide side,
        int slot,
        long maxHp
) {
    public BattleParticipant {
        if (battleUnitId == null || battleUnitId.isBlank()) throw new IllegalArgumentException("battleUnitId required");
        if (characterId == null || characterId.isBlank()) throw new IllegalArgumentException("characterId required");
        heroId = heroId == null ? "" : heroId.trim();
        awakeningId = awakeningId == null ? "" : awakeningId.trim();
        presentationKey = presentationKey == null || presentationKey.isBlank()
                ? presentationKey(heroId.isBlank() ? characterId : heroId, awakened)
                : presentationKey;
        if (displayName == null || displayName.isBlank()) displayName = heroId.isBlank() ? characterId : heroId;
        if (level <= 0) throw new IllegalArgumentException("level must be positive");
        if (side == null) throw new IllegalArgumentException("side required");
        if (slot < 0 || slot > 4) throw new IllegalArgumentException("slot must be between 0 and 4");
        if (maxHp <= 0) throw new IllegalArgumentException("maxHp must be positive");
        variant = variant == null ? "" : variant;
        if (awakened && awakeningId.isBlank()) {
            throw new IllegalArgumentException("awakened participant requires awakeningId");
        }
    }

    /** Backward-compatible constructor for legacy enemy content. */
    public BattleParticipant(
            String battleUnitId,
            String characterId,
            String displayName,
            String variant,
            int level,
            TeamSide side,
            int slot,
            long maxHp
    ) {
        this(battleUnitId, characterId, "", false, "", presentationKey(characterId, false), displayName, variant,
                level, side, slot, maxHp);
    }

    public static BattleParticipant heroVersion(
            String battleUnitId,
            String characterId,
            String heroId,
            boolean awakened,
            String awakeningId,
            String displayName,
            int level,
            TeamSide side,
            int slot,
            long maxHp
    ) {
        return new BattleParticipant(
                battleUnitId,
                characterId,
                heroId,
                awakened,
                awakeningId,
                presentationKey(heroId, awakened),
                displayName,
                awakened ? "AWAKENED" : "BASE",
                level,
                side,
                slot,
                maxHp);
    }

    public static String presentationKey(String heroId, boolean awakened) {
        String safe = heroId == null || heroId.isBlank() ? "unknown" : heroId.trim();
        return "hero/" + safe + (awakened ? "/awakened" : "/base");
    }
}
