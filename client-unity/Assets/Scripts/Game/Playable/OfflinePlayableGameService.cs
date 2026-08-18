using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using NinjaAssemble.Network;

namespace NinjaAssemble.Playable
{
    public sealed class OfflinePlayableGameService : IPlayableGameService
    {
        private static readonly string[] SummonPool =
        {
            "Temari", "Kankuro", "Choji Akimichi", "Ino Yamanaka", "Sai",
            "Yamato", "Konan", "Deidara", "Kisame Hoshigaki", "Shisui Uchiha"
        };

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
            if (selected.Length != 5) selected = ResolveFormation();
            state.formationIds = selected.Select(hero => hero.id).ToArray();
            saves.Save(state);
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

        public Task<PlayBattleDto> PlayCampaignStageAsync(string playerId, string stageId)
        {
            EnsureState();
            CampaignStageDto stage = StageById(stageId);
            if (!stage.unlocked) throw new InvalidOperationException(stageId + " is locked");
            if (state.energy < stage.energyCost) throw new InvalidOperationException("Not enough Energy");

            bool firstClear = !state.clearedStageIds.Contains(stageId);
            long goldReward = firstClear ? 2500 : 1000;
            long diamondReward = firstClear ? 25 : 0;
            long seed = OfflineBattleSimulator.StableSeed(stageId, ResolveFormation().Select(hero => hero.id), state.battleCount);
            PlayBattleDto result = OfflineBattleSimulator.Simulate(stageId, ResolveFormation(), seed, stage.energyCost, goldReward, diamondReward);
            result.firstClear = firstClear;
            result.stars = 3;

            state.energy = Math.Max(0, state.energy - stage.energyCost);
            state.gold += goldReward;
            state.diamond += diamondReward;
            state.battleCount++;
            if (firstClear) state.clearedStageIds = state.clearedStageIds.Concat(new[] { stageId }).Distinct().ToArray();
            saves.Save(state);
            return Task.FromResult(result);
        }

        public Task<CampaignSweepDto> SweepCampaignStageAsync(string playerId, string stageId, string requestId)
        {
            EnsureState();
            CampaignStageDto stage = StageById(stageId);
            if (!state.clearedStageIds.Contains(stageId)) throw new InvalidOperationException("Stage must be cleared before sweep");
            if (state.energy < stage.energyCost) throw new InvalidOperationException("Not enough Energy");
            state.energy -= stage.energyCost;
            state.gold += 1000;
            state.battleCount++;
            saves.Save(state);
            return Task.FromResult(new CampaignSweepDto { requestId = requestId, playerId = state.playerId, stageId = stageId, catalogVersion = "offline-playtest-v1", replayed = false, energyCost = stage.energyCost, energyAfter = state.energy, playerExpReward = 100, goldReward = 1000, diamondReward = 0, accountLevelAfter = 20, itemRewards = Array.Empty<CampaignSweepItemDto>() });
        }

        public Task<ResourcePveBoardDto> GetResourcePveAsync(string playerId)
        {
            EnsureState();
            return Task.FromResult(new ResourcePveBoardDto
            {
                catalogVersion = "offline-resource-v1", rulesetVersion = "offline-rules-v1", gameDate = DateTime.UtcNow.ToString("yyyy-MM-dd"), playerLevel = 20,
                energy = state.energy, energyCap = state.energyCap,
                modes = new[] { new ResourcePveModeDto { modeId = "gold-trial", modeType = "GOLD", nameEn = "Gold Trial", nameVi = "Thử thách Vàng", teamSize = 5, energyCost = 5, dailyAttemptLimit = 99, attemptsRemaining = 99, minPlayerLevel = 1, clearsToday = 0, bestScore = 0, playable = state.energy >= 5, blockedReason = state.energy >= 5 ? string.Empty : "ENERGY", rewardGold = 5000, rewardItemId = "ramen", rewardItemQuantity = 2, resetPolicy = "LOCAL" } }
            });
        }

        public Task<ResourcePveBattleDto> PlayResourcePveAsync(string playerId, string modeId, string requestId)
        {
            EnsureState();
            const int energyCost = 5;
            if (state.energy < energyCost) throw new InvalidOperationException("Not enough Energy");
            long seed = OfflineBattleSimulator.StableSeed(modeId, ResolveFormation().Select(hero => hero.id), state.battleCount);
            PlayBattleDto simulated = OfflineBattleSimulator.Simulate("resource-" + modeId, ResolveFormation(), seed, energyCost, 5000, 0);
            state.energy -= energyCost;
            state.gold += 5000;
            state.ramen += 2;
            state.battleCount++;
            saves.Save(state);
            return Task.FromResult(new ResourcePveBattleDto { requestId = requestId, playerId = state.playerId, modeId = modeId, catalogVersion = "offline-resource-v1", rulesetVersion = "offline-rules-v1", gameDate = DateTime.UtcNow.ToString("yyyy-MM-dd"), replayed = false, energyCost = energyCost, energyAfter = state.energy, won = true, goldReward = 5000, itemId = "ramen", itemQuantity = 2, seed = seed, participants = simulated.participants, battle = simulated.battle });
        }

        public Task<InventoryViewDto> GetInventoryAsync(string playerId)
        {
            EnsureState();
            return Task.FromResult(new InventoryViewDto { catalogVersion = "offline-items-v1", items = new[] { new InventoryItemDto { itemId = "ramen", itemType = "MATERIAL", nameEn = "Ramen", nameVi = "Mì Ramen", quantity = state.ramen }, new InventoryItemDto { itemId = "hero-coin", itemType = "CURRENCY", nameEn = "Hero Coin", nameVi = "Xu Ninja", quantity = state.heroCoins } } });
        }

        public Task<ArenaStateDto> GetArenaAsync(string playerId) => Task.FromResult(new ArenaStateDto { seasonId = "offline", rating = 1000, ratingProfileVersion = "offline", rewardProfileVersion = "offline", defenseConfigured = true, opponents = new[] { new ArenaOpponentDto { playerId = "offline-bot", displayName = "Training Team", rating = 1000, power = 50000, training = true } } });

        public Task<ShadowArenaStateDto> GetShadowArenaAsync(string playerId)
        {
            EnsureState();
            bool eligible = state.heroes.Length >= 15;
            return Task.FromResult(new ShadowArenaStateDto { seasonId = "offline", eligible = eligible, ownedCount = state.heroes.Length, requiredCount = 15, missingCount = Math.Max(0, 15 - state.heroes.Length), rating = 1000, ratingProfileVersion = "offline", seriesRulesVersion = "offline", rewardProfileVersion = "offline", defenseConfigured = eligible, opponents = eligible ? new[] { new ShadowArenaOpponentDto { playerId = "offline-shadow-bot", displayName = "Shadow Training Team", rating = 1000, totalPower = 150000, training = true } } : Array.Empty<ShadowArenaOpponentDto>() });
        }

        public Task<CompetitiveHistoryItemDto[]> GetArenaHistoryAsync(string playerId, int limit = 20) => Task.FromResult(Array.Empty<CompetitiveHistoryItemDto>());
        public Task<CompetitiveHistoryItemDto[]> GetShadowArenaHistoryAsync(string playerId, int limit = 20) => Task.FromResult(Array.Empty<CompetitiveHistoryItemDto>());
        public Task<ShopViewDto> GetShopAsync(string playerId) => Task.FromResult(new ShopViewDto { catalogVersion = "offline-shop-v1", resetKey = "offline", shops = new[] { new ShopEntryDto { shopId = "offline-general", nameEn = "Offline Shop", nameVi = "Cửa hàng Offline", refreshProfile = "NONE", offers = new[] { new ShopOfferDto { offerId = "ramen-pack", itemId = "ramen", itemNameEn = "Ramen", itemNameVi = "Mì Ramen", quantity = 5, currency = "GOLD", price = 1000, purchaseLimit = -1, purchasedCount = 0, remaining = -1, purchasable = state == null || state.gold >= 1000 } } } } });

        public Task<QuestBoardDto> GetQuestsAsync(string playerId)
        {
            EnsureState();
            bool claimed = state.claimedQuestIds.Contains("offline-battle");
            return Task.FromResult(new QuestBoardDto { catalogVersion = "offline-quest-v1", resetKey = "offline", nextResetAt = string.Empty, quests = new[] { new QuestDto { questId = "offline-battle", nameEn = "Complete a battle", nameVi = "Hoàn thành một trận", metric = "BATTLE", target = 1, currentValue = state.battleCount, claimed = claimed, claimable = state.battleCount >= 1 && !claimed, rewardGold = 5000, rewardDiamond = 50 } } });
        }

        public Task<MailboxDto> GetMailAsync(string playerId)
        {
            EnsureState();
            bool claimed = state.claimedMailIds.Contains("offline-welcome");
            return Task.FromResult(new MailboxDto { mailProfileVersion = "offline-mail-v1", mails = new[] { new MailDto { mailId = "offline-welcome", subject = "Offline Playtest", body = "Welcome to the standalone playtest build.", attachments = new[] { new MailAttachmentDto { kind = "CURRENCY", id = "DIAMOND", quantity = 200 } }, read = claimed, claimed = claimed, createdAt = DateTime.UtcNow.ToString("O"), expiresAt = string.Empty } } });
        }

        public Task<ArenaDefenseDto> SaveArenaDefenseAsync(string playerId, string[] heroIds) => Task.FromResult(new ArenaDefenseDto { formationId = "offline-defense", heroes = ResolveFormation() });
        public Task<ArenaBattleDto> FightArenaAsync(string playerId, string opponentPlayerId, string requestId) => Unsupported<ArenaBattleDto>("Arena training is delivered in offline playtest 3/5");
        public Task<SeasonRewardDto> ClaimArenaSeasonAsync(string playerId) => Task.FromResult(new SeasonRewardDto { seasonId = "offline", finalRating = 1000, rewardAmount = 0, claimable = false, claimed = true });
        public Task<ShadowDefenseDto> SaveShadowDefenseAsync(string playerId, string[] heroIds) => Task.FromResult(new ShadowDefenseDto { seasonId = "offline", heroes = state?.heroes?.Take(15).ToArray() ?? Array.Empty<OwnedHeroDto>() });
        public Task<ShadowArenaBattleDto> FightShadowArenaAsync(string playerId, string opponentPlayerId, string requestId) => Unsupported<ShadowArenaBattleDto>("Shadow Arena training is delivered in offline playtest 3/5");
        public Task<SeasonRewardDto> ClaimShadowSeasonAsync(string playerId) => Task.FromResult(new SeasonRewardDto { seasonId = "offline", finalRating = 1000, rewardAmount = 0, claimable = false, claimed = true });
        public Task<ShopPurchaseResultDto> PurchaseShopAsync(string playerId, string shopId, string offerId, string requestId) => Unsupported<ShopPurchaseResultDto>("Offline shop actions are delivered in offline playtest 3/5");
        public Task<QuestClaimDto> ClaimQuestAsync(string playerId, string questId) => Unsupported<QuestClaimDto>("Offline quest claims are delivered in offline playtest 3/5");
        public Task ReadMailAsync(string playerId, string mailId) => Task.CompletedTask;
        public Task<MailClaimDto> ClaimMailAsync(string playerId, string mailId) => Unsupported<MailClaimDto>("Offline mail claims are delivered in offline playtest 3/5");

        public Task<SummonResultDto> SummonAsync(string playerId, string requestId)
        {
            EnsureState();
            if (state.diamond < PlayableGameStore.CompleteRosterSummonCost) throw new InvalidOperationException("Not enough Diamond");
            int index = state.summonCount % SummonPool.Length;
            string name = SummonPool[index];
            string characterId = Slug(name);
            string heroId = characterId + "-base";
            OwnedHeroDto existing = state.heroes.FirstOrDefault(hero => hero.heroId == heroId);
            bool duplicate = existing != null;
            state.diamond -= PlayableGameStore.CompleteRosterSummonCost;
            state.summonCount++;
            long duplicateCoins = duplicate ? 25 : 0;
            if (duplicate) state.heroCoins += duplicateCoins;
            else
            {
                var hero = new OwnedHeroDto { id = "offline-summon-" + state.summonCount, characterId = characterId, heroId = heroId, displayName = name, level = 1, exp = 0, frameTier = "BLUE", awakened = false, currentVariant = "BASE", awakeningLevel = 0 };
                state.heroes = state.heroes.Concat(new[] { hero }).ToArray();
            }
            saves.Save(state);
            long seed = OfflineBattleSimulator.StableSeed("summon", new[] { requestId ?? string.Empty }, state.summonCount);
            return Task.FromResult(new SummonResultDto { heroId = heroId, characterId = characterId, displayName = name, rarity = index % 5 == 0 ? "SSR" : index % 2 == 0 ? "SR" : "R", pityTriggered = state.summonCount % 50 == 0, duplicate = duplicate, duplicateHeroCoins = duplicateCoins, pullsSincePity = state.summonCount % 50, bannerVersion = "offline-banner-v1", seed = seed });
        }

        public Task<UpgradeResultDto> LevelUpAsync(string playerId, string playerHeroId, string requestId)
        {
            EnsureState();
            OwnedHeroDto hero = state.heroes.FirstOrDefault(value => value.id == playerHeroId);
            if (hero == null) throw new InvalidOperationException("Owned hero not found");
            long cost = Math.Max(500, hero.level * 250L);
            if (state.gold < cost) throw new InvalidOperationException("Not enough Gold");
            state.gold -= cost;
            hero.level++;
            hero.exp = 0;
            saves.Save(state);
            return Task.FromResult(new UpgradeResultDto { hero = hero, goldCost = cost, costProfileVersion = "offline-level-cost-v1" });
        }

        private CampaignStageDto Stage(string id, int chapter, int index, string name, int energyCost, bool unlocked)
        {
            EnsureState();
            int clears = state.clearedStageIds.Contains(id) ? 1 : 0;
            return new CampaignStageDto { stageId = id, chapter = chapter, stageIndex = index, difficulty = "NORMAL", nameEn = name, nameVi = name, energyCost = energyCost, minPlayerLevel = 1, prerequisiteStageIds = Array.Empty<string>(), waveCount = 1, unlocked = unlocked, gateMissing = unlocked ? Array.Empty<string>() : new[] { "PREVIOUS_STAGE" }, clearCount = clears, bestStars = clears > 0 ? 3 : 0 };
        }

        private CampaignStageDto StageById(string stageId)
        {
            CampaignStageDto[] stages = GetCampaignStagesAsync(state.playerId).Result.stages;
            CampaignStageDto stage = stages.FirstOrDefault(value => value.stageId == stageId);
            if (stage == null) throw new InvalidOperationException("Unknown offline stage: " + stageId);
            return stage;
        }

        private OwnedHeroDto[] ResolveFormation()
        {
            EnsureState();
            OwnedHeroDto[] selected = (state.formationIds ?? Array.Empty<string>()).Select(id => state.heroes.FirstOrDefault(hero => hero.id == id)).Where(hero => hero != null).Take(5).ToArray();
            if (selected.Length == 5) return selected;
            selected = state.heroes.Take(5).ToArray();
            state.formationIds = selected.Select(hero => hero.id).ToArray();
            saves.Save(state);
            return selected;
        }

        private static string Slug(string value) => (value ?? string.Empty).Trim().ToLowerInvariant().Replace(' ', '-');
        private void EnsureState() { if (state == null) state = saves.LoadOrCreate(); }
        private static Task<T> Unsupported<T>(string message) => Task.FromException<T>(new InvalidOperationException(message));
    }
}
