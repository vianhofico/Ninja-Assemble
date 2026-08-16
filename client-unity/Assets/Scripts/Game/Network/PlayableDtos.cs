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
    [Serializable] public sealed class CampaignItemRewardDto { public string itemId; public string nameEn; public string nameVi; public long quantity; }
    [Serializable] public sealed class CampaignRewardDto { public long playerExp; public long gold; public long diamond; public CampaignItemRewardDto[] items; }
    [Serializable] public sealed class CampaignStageDto
    {
        public string stageId; public int chapter; public int stageIndex; public string difficulty; public string nameEn; public string nameVi;
        public int energyCost; public int minPlayerLevel; public string[] prerequisiteStageIds; public int waveCount; public bool unlocked; public string[] gateMissing;
        public int clearCount; public int bestStars; public CampaignRewardDto firstClearReward; public CampaignRewardDto repeatReward;
    }
    [Serializable] public sealed class CampaignStageListDto
    {
        public string catalogVersion; public int playerLevel; public int energy; public int energyCap; public CampaignStageDto[] stages;
    }
    [Serializable] public sealed class CampaignWaveDto
    {
        public int waveIndex; public long waveSeed; public BattleParticipantDto[] participants; public BattleResultDto battle;
    }
    [Serializable] public sealed class InventoryRewardItemDto { public string itemId; public long quantity; public long balanceAfter; }
    [Serializable] public sealed class InventoryItemDto { public string itemId; public string itemType; public string nameEn; public string nameVi; public long quantity; }
    [Serializable] public sealed class InventoryViewDto { public string catalogVersion; public InventoryItemDto[] items; }
    [Serializable] public sealed class ArenaOpponentDto { public string playerId; public string displayName; public long rating; public long power; public bool training; }
    [Serializable] public sealed class ArenaStateDto { public string seasonId; public long rating; public string ratingProfileVersion; public string rewardProfileVersion; public ArenaOpponentDto[] opponents; }
    [Serializable] public sealed class ArenaBattleDto
    {
        public string battleId; public string seasonId; public bool training; public string opponentPlayerId; public string opponentDisplayName;
        public long opponentRating; public long opponentPower; public long ratingBefore; public long ratingAfter; public long ratingDelta;
        public string ratingProfileVersion; public long arenaCoinReward; public string rewardProfileVersion;
        public string combatStatsVersion; public string abilityProfileVersion; public string techniqueMappingVersion; public string passiveProfileVersion;
        public BattleParticipantDto[] participants; public BattleResultDto battle;
    }
    [Serializable] public sealed class ShopOfferDto
    {
        public string offerId; public string itemId; public string itemNameEn; public string itemNameVi; public long quantity;
        public string currency; public long price; public int? purchaseLimit; public int purchasedCount; public int? remaining;
        public bool purchasable; public string blockedReason;
    }
    [Serializable] public sealed class ShopEntryDto
    {
        public string shopId; public string nameEn; public string nameVi; public string refreshProfile; public ShopOfferDto[] offers;
    }
    [Serializable] public sealed class ShopViewDto { public string catalogVersion; public string resetKey; public ShopEntryDto[] shops; }
    [Serializable] public sealed class ShopPurchaseResultDto
    {
        public string shopId; public string offerId; public string resetKey; public bool replayed; public string currency;
        public long charged; public long itemBalanceAfter; public int purchaseCount;
    }
    [Serializable] public sealed class PlayBattleDto
    {
        public string battleId; public string stageId; public string campaignCatalogVersion; public string waveRulesVersion; public int energyCost; public int stars; public bool firstClear;
        public long playerExpReward; public long goldReward; public long diamondReward; public int accountLevelAfter; public InventoryRewardItemDto[] itemRewards;
        public string combatStatsVersion; public string abilityProfileVersion; public string techniqueMappingVersion; public string passiveProfileVersion;
        public CampaignWaveDto[] waves; public BattleParticipantDto[] participants; public BattleResultDto battle;
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
