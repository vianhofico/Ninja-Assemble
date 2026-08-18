using System;
using System.Linq;
using System.Threading.Tasks;
using NinjaAssemble.Network;

namespace NinjaAssemble.Playable
{
    public sealed class OfflinePlayableGameService : IPlayableGameService
    {
        private readonly OfflineSaveRepository saves;
        private OfflineSaveData state;

        public OfflinePlayableGameService(OfflineSaveRepository repository = null)
        {
            saves = repository ?? new OfflineSaveRepository();
        }

        public Task<PlayerDto> LoginGuestAsync(string guestKey, string displayName)
        {
            EnsureState();
            if (!string.IsNullOrWhiteSpace(displayName)) state.displayName = displayName;
            saves.Save(state);
            return Task.FromResult(new PlayerDto { id = state.playerId, displayName = state.displayName, level = 20, exp = 0, sessionToken = "offline" });
        }

        public Task<BootstrapDto> BootstrapAsync(string playerId)
        {
            EnsureState();
            return Task.FromResult(new BootstrapDto { heroes = state.heroes, gold = state.gold, diamond = state.diamond, energy = state.energy, energyCap = state.energyCap });
        }

        public Task<OwnedHeroDto[]> GetOwnedHeroesAsync(string playerId) { EnsureState(); return Task.FromResult(state.heroes ?? Array.Empty<OwnedHeroDto>()); }

        public Task<FormationDto> SaveFormationAsync(string playerId, string[] heroIds)
        {
            EnsureState();
            string[] requested = heroIds ?? Array.Empty<string>();
            OwnedHeroDto[] selected = requested.Select(id => state.heroes.FirstOrDefault(hero => hero.id == id)).Where(hero => hero != null).Take(5).ToArray();
            if (selected.Length == 5)
            {
                state.formationIds = selected.Select(hero => hero.id).ToArray();
                saves.Save(state);
            }
            else
            {
                selected = state.heroes.Take(5).ToArray();
                state.formationIds = selected.Select(hero => hero.id).ToArray();
                saves.Save(state);
            }
            return Task.FromResult(new FormationDto { heroes = selected });
        }

        public Task<CampaignStageListDto> GetCampaignStagesAsync(string playerId)
        {
            EnsureState();
            return Task.FromResult(new CampaignStageListDto
            {
                catalogVersion = "offline-playtest-v1",
                playerLevel = 20,
                energy = state.energy,
                energyCap = state.energyCap,
                stages = new[]
                {
                    Stage("c1-s1",1,1,"Training Grounds",5,true),
                    Stage("c1-s2",1,2,"Forest Mission",6,state.clearedStageIds.Contains("c1-s1")),
                    Stage("c1-s3",1,3,"Bridge Clash",7,state.clearedStageIds.Contains("c1-s2"))
                }
            });
        }

        public Task<ResourcePveBoardDto> GetResourcePveAsync(string playerId)
        {
            EnsureState();
            return Task.FromResult(new ResourcePveBoardDto
            {
                catalogVersion = "offline-resource-v1", rulesetVersion = "offline-rules-v1", gameDate = DateTime.UtcNow.ToString("yyyy-MM-dd"), playerLevel = 20,
                energy = state.energy, energyCap = state.energyCap,
                modes = new[] { new ResourcePveModeDto { modeId = "gold-trial", modeType = "GOLD", nameEn = "Gold Trial", nameVi = "Thử thách Vàng", teamSize = 5, energyCost = 5, dailyAttemptLimit = 99, attemptsRemaining = 99, minPlayerLevel = 1, playable = true, rewardGold = 5000, resetPolicy = "LOCAL" } }
            });
        }

        public Task<InventoryViewDto> GetInventoryAsync(string playerId) => Task.FromResult(new InventoryViewDto { catalogVersion = "offline-items-v1", items = new[] { new InventoryItemDto { itemId = "ramen", itemType = "MATERIAL", nameEn = "Ramen", nameVi = "Mì Ramen", quantity = 50 }, new InventoryItemDto { itemId = "hero-coin", itemType = "CURRENCY", nameEn = "Hero Coin", nameVi = "Xu Ninja", quantity = 0 } } });

        public Task<ArenaStateDto> GetArenaAsync(string playerId) => Task.FromResult(new ArenaStateDto { seasonId = "offline", rating = 1000, ratingProfileVersion = "offline", rewardProfileVersion = "offline", defenseConfigured = true, opponents = new[] { new ArenaOpponentDto { playerId = "offline-bot", displayName = "Training Team", rating = 1000, power = 50000, training = true } } });

        public Task<ShadowArenaStateDto> GetShadowArenaAsync(string playerId)
        {
            EnsureState();
            bool eligible = state.heroes.Length >= 15;
            return Task.FromResult(new ShadowArenaStateDto { seasonId = "offline", eligible = eligible, ownedCount = state.heroes.Length, requiredCount = 15, missingCount = Math.Max(0, 15 - state.heroes.Length), rating = 1000, ratingProfileVersion = "offline", seriesRulesVersion = "offline", rewardProfileVersion = "offline", defenseConfigured = eligible, opponents = eligible ? new[] { new ShadowArenaOpponentDto { playerId = "offline-shadow-bot", displayName = "Shadow Training Team", rating = 1000, totalPower = 150000, training = true } } : Array.Empty<ShadowArenaOpponentDto>() });
        }

        public Task<CompetitiveHistoryItemDto[]> GetArenaHistoryAsync(string playerId, int limit = 20) => Task.FromResult(Array.Empty<CompetitiveHistoryItemDto>());
        public Task<CompetitiveHistoryItemDto[]> GetShadowArenaHistoryAsync(string playerId, int limit = 20) => Task.FromResult(Array.Empty<CompetitiveHistoryItemDto>());
        public Task<ShopViewDto> GetShopAsync(string playerId) => Task.FromResult(new ShopViewDto { catalogVersion = "offline-shop-v1", resetKey = "offline", shops = new[] { new ShopEntryDto { shopId = "offline-general", nameEn = "Offline Shop", nameVi = "Cửa hàng Offline", refreshProfile = "NONE", offers = new[] { new ShopOfferDto { offerId = "ramen-pack", itemId = "ramen", itemNameEn = "Ramen", itemNameVi = "Mì Ramen", quantity = 5, currency = "GOLD", price = 1000, purchaseLimit = -1, purchasedCount = 0, remaining = -1, purchasable = true } } } } });
        public Task<QuestBoardDto> GetQuestsAsync(string playerId) => Task.FromResult(new QuestBoardDto { catalogVersion = "offline-quest-v1", resetKey = "offline", nextResetAt = string.Empty, quests = new[] { new QuestDto { questId = "offline-battle", nameEn = "Complete a battle", nameVi = "Hoàn thành một trận", metric = "BATTLE", target = 1, currentValue = 0, claimed = false, claimable = false, rewardGold = 5000, rewardDiamond = 50 } } });
        public Task<MailboxDto> GetMailAsync(string playerId) => Task.FromResult(new MailboxDto { mailProfileVersion = "offline-mail-v1", mails = new[] { new MailDto { mailId = "offline-welcome", subject = "Offline Playtest", body = "Welcome to the standalone playtest build.", attachments = Array.Empty<MailAttachmentDto>(), read = false, claimed = false, createdAt = DateTime.UtcNow.ToString("O"), expiresAt = string.Empty } } });

        public Task<PlayBattleDto> PlayCampaignStageAsync(string playerId, string stageId) => Unsupported<PlayBattleDto>();
        public Task<CampaignSweepDto> SweepCampaignStageAsync(string playerId, string stageId, string requestId) => Unsupported<CampaignSweepDto>();
        public Task<ResourcePveBattleDto> PlayResourcePveAsync(string playerId, string modeId, string requestId) => Unsupported<ResourcePveBattleDto>();
        public Task<ArenaDefenseDto> SaveArenaDefenseAsync(string playerId, string[] heroIds) => Task.FromResult(new ArenaDefenseDto { formationId = "offline-defense", heroes = state?.heroes?.Take(5).ToArray() ?? Array.Empty<OwnedHeroDto>() });
        public Task<ArenaBattleDto> FightArenaAsync(string playerId, string opponentPlayerId, string requestId) => Unsupported<ArenaBattleDto>();
        public Task<SeasonRewardDto> ClaimArenaSeasonAsync(string playerId) => Task.FromResult(new SeasonRewardDto { seasonId = "offline", finalRating = 1000, rewardAmount = 0, claimable = false, claimed = true });
        public Task<ShadowDefenseDto> SaveShadowDefenseAsync(string playerId, string[] heroIds) => Task.FromResult(new ShadowDefenseDto { seasonId = "offline", heroes = state?.heroes?.Take(15).ToArray() ?? Array.Empty<OwnedHeroDto>() });
        public Task<ShadowArenaBattleDto> FightShadowArenaAsync(string playerId, string opponentPlayerId, string requestId) => Unsupported<ShadowArenaBattleDto>();
        public Task<SeasonRewardDto> ClaimShadowSeasonAsync(string playerId) => Task.FromResult(new SeasonRewardDto { seasonId = "offline", finalRating = 1000, rewardAmount = 0, claimable = false, claimed = true });
        public Task<ShopPurchaseResultDto> PurchaseShopAsync(string playerId, string shopId, string offerId, string requestId) => Unsupported<ShopPurchaseResultDto>();
        public Task<QuestClaimDto> ClaimQuestAsync(string playerId, string questId) => Unsupported<QuestClaimDto>();
        public Task ReadMailAsync(string playerId, string mailId) => Task.CompletedTask;
        public Task<MailClaimDto> ClaimMailAsync(string playerId, string mailId) => Task.FromResult(new MailClaimDto { mailId = mailId, replayed = true, grants = Array.Empty<MailGrantDto>() });
        public Task<SummonResultDto> SummonAsync(string playerId, string requestId) => Unsupported<SummonResultDto>();
        public Task<UpgradeResultDto> LevelUpAsync(string playerId, string playerHeroId, string requestId) => Unsupported<UpgradeResultDto>();

        private CampaignStageDto Stage(string id, int chapter, int index, string name, int energyCost, bool unlocked)
        {
            EnsureState();
            int clears = state.clearedStageIds.Contains(id) ? 1 : 0;
            return new CampaignStageDto { stageId = id, chapter = chapter, stageIndex = index, difficulty = "NORMAL", nameEn = name, nameVi = name, energyCost = energyCost, minPlayerLevel = 1, prerequisiteStageIds = Array.Empty<string>(), waveCount = 1, unlocked = unlocked, gateMissing = unlocked ? Array.Empty<string>() : new[] { "PREVIOUS_STAGE" }, clearCount = clears, bestStars = clears > 0 ? 3 : 0 };
        }

        private void EnsureState() { if (state == null) state = saves.LoadOrCreate(); }
        private static Task<T> Unsupported<T>() => Task.FromException<T>(new InvalidOperationException("This action is implemented in the offline gameplay milestone; the offline core only guarantees standalone bootstrap/read state."));
    }
}
