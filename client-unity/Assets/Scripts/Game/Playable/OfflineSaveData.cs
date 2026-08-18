using System;
using NinjaAssemble.Network;

namespace NinjaAssemble.Playable
{
    [Serializable]
    public sealed class OfflineSaveData
    {
        public int saveVersion = 1;
        public string playerId = "offline-player";
        public string displayName = "Ninja";
        public long gold = 1000000;
        public long diamond = 10000;
        public int energy = 999;
        public int energyCap = 999;
        public int summonCount;
        public int battleCount;
        public long heroCoins;
        public long ramen = 50;
        public long arenaRating = 1000;
        public long shadowRating = 1000;
        public OwnedHeroDto[] heroes = Array.Empty<OwnedHeroDto>();
        public string[] formationIds = Array.Empty<string>();
        public string[] clearedStageIds = Array.Empty<string>();
        public string[] claimedQuestIds = Array.Empty<string>();
        public string[] claimedMailIds = Array.Empty<string>();
        public string[] readMailIds = Array.Empty<string>();
    }
}
