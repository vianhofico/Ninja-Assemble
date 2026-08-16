using System;

namespace NinjaAssemble.Network
{
    [Serializable] public sealed class OwnedHeroDto
    {
        public string id; public string characterId; public string displayName; public int level; public long exp;
        public string frameTier; public string currentVariant; public int awakeningLevel;
    }
    [Serializable] public sealed class BootstrapDto { public OwnedHeroDto[] heroes; public long gold; public long diamond; public int energy; public int energyCap; }
    [Serializable] public sealed class FormationDto { public OwnedHeroDto[] heroes; }
    [Serializable] public sealed class BattleParticipantDto
    {
        public string battleUnitId; public string characterId; public string displayName; public string variant;
        public int level; public string side; public int slot; public long maxHp;
    }
    [Serializable] public sealed class BattleEventDto
    {
        public long sequence; public string type; public int round; public string actorId; public string targetId; public long amount; public bool critical;
        public string abilityId; public string abilityKind; public string effectKey; public int energyAfter;
        public string effectType; public string statusId; public int durationTurns; public string triggerId;
    }
    [Serializable] public sealed class BattleResultDto { public long seed; public string rulesetVersion; public string outcome; public int rounds; public BattleEventDto[] events; }
    [Serializable] public sealed class PlayBattleDto
    {
        public string battleId; public int energyCost; public long goldReward; public string combatStatsVersion;
        public string abilityProfileVersion; public string techniqueMappingVersion; public string passiveProfileVersion;
        public BattleParticipantDto[] participants; public BattleResultDto battle;
    }
    [Serializable] public sealed class SummonResultDto
    {
        public string characterId; public string variant; public string rarity; public bool pityTriggered; public bool duplicate;
        public long duplicateHeroCoins; public int pullsSincePity; public string bannerVersion; public long seed;
    }
    [Serializable] public sealed class UpgradeResultDto { public OwnedHeroDto hero; public long goldCost; public string costProfileVersion; }
    [Serializable] public sealed class FormationRequestDto { public string[] playerHeroIds; }
    [Serializable] public sealed class ActionRequestDto { public string requestId; }
    [Serializable] public sealed class VariantRequestDto { public string variant; }
}
