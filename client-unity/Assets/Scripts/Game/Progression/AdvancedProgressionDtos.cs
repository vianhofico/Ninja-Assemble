using System;
namespace NinjaAssemble.Progression
{
    [Serializable] public sealed class AdvancedProgressionTrackDto { public string trackId; public string trackType; public string nameEn; public string nameVi; public int level; public int maxLevel; public int minPlayerLevel; public bool unlocked; public bool maxed; public bool affordable; public string blockedReason; public long nextGoldCost; public string itemId; public long nextItemCost; public string bonusStat; public int bonusPerLevel; public int cumulativeBonus; }
    [Serializable] public sealed class AdvancedProgressionBoardDto { public string catalogVersion; public int playerLevel; public long gold; public AdvancedProgressionTrackDto[] tracks; }
    [Serializable] public sealed class AdvancedProgressionUpgradeDto { public string requestId; public string playerId; public string trackId; public string catalogVersion; public bool replayed; public int levelBefore; public int levelAfter; public long goldCost; public long goldAfter; public string itemId; public long itemCost; public long itemAfter; public string bonusStat; public int cumulativeBonus; }
}
