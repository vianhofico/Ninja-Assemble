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

        public Task<AwakeningViewDto> GetAwakeningAsync(string playerId, string playerHeroId)
        {
            EnsureState();
            OwnedHeroDto hero = state.heroes.FirstOrDefault(value => value.id == playerHeroId);
            if (hero == null) throw new InvalidOperationException("Owned hero not found");
            bool available = !string.IsNullOrWhiteSpace(hero.awakeningId);
            return Task.FromResult(new AwakeningViewDto { hero = hero, available = available, awakened = hero.awakened, changed = false, awakeningId = hero.awakeningId, awakeningName = hero.awakeningName, visual = available ? OfflineAwakeningVisual(hero) : null });
        }

        public Task<AwakeningViewDto> AwakenAsync(string playerId, string playerHeroId)
        {
            EnsureState();
            OwnedHeroDto hero = state.heroes.FirstOrDefault(value => value.id == playerHeroId);
            if (hero == null) throw new InvalidOperationException("Owned hero not found");
            if (string.IsNullOrWhiteSpace(hero.awakeningId)) throw new InvalidOperationException("This Hero Version has no Awakening");
            bool changed = !hero.awakened;
            hero.awakened = true;
            hero.awakeningLevel = Math.Max(1, hero.awakeningLevel);
            saves.Save(state);
            return Task.FromResult(new AwakeningViewDto { hero = hero, available = true, awakened = true, changed = changed, awakeningId = hero.awakeningId, awakeningName = hero.awakeningName, visual = OfflineAwakeningVisual(hero) });
        }

        public Task<CampaignStageListDto> GetCampaignStagesAsync(string playerId)
        {
            EnsureState();
            return Task.FromResult(new CampaignStageListDto
            {
                catalogVersion = "offline-playtest-v1", playerLevel = 20, energy = state.energy, energyCap = state.energyCap,
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
            return Task.FromResult(new ResourcePveBoardDto { catalogVersion = "offline-resource-v1", rulesetVersion = "offline-rules-v1", gameDate = DateTime.UtcNow.ToString("yyyy-MM-dd"), playerLevel = 20, energy = state.energy, energyCap = state.energyCap, modes = new[] { new ResourcePveModeDto { modeId = "gold-trial", modeType = "GOLD", nameEn = "Gold Trial", nameVi = "Thử thách Vàng", teamSize = 5, energyCost = 5, dailyAttemptLimit = 99, attemptsRemaining = 99, minPlayerLevel = 1, clearsToday = 0, bestScore = 0, playable = state.energy >= 5, blockedReason = state.energy >= 5 ? string.Empty : "ENERGY", rewardGold = 5000, rewardItemId = "ramen", rewardItemQuantity = 2, resetPolicy = "LOCAL" } } });
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

        public Task<ArenaStateDto> GetArenaAsync(string playerId)
        {
            EnsureState();
            return Task.FromResult(new ArenaStateDto { seasonId = "offline", rating = state.arenaRating, ratingProfileVersion = "offline", rewardProfileVersion = "offline", defenseConfigured = true, opponents = new[] { new ArenaOpponentDto { playerId = "offline-bot", displayName = "Training Team", rating = state.arenaRating, power = 50000, training = true } } });
        }

        public Task<ShadowArenaStateDto> GetShadowArenaAsync(string playerId)
        {
            EnsureState();
            bool eligible = state.heroes.Length >= 15;
            return Task.FromResult(new ShadowArenaStateDto { seasonId = "offline", eligible = eligible, ownedCount = state.heroes.Length, requiredCount = 15, missingCount = Math.Max(0, 15 - state.heroes.Length), rating = state.shadowRating, ratingProfileVersion = "offline", seriesRulesVersion = "offline", rewardProfileVersion = "offline", defenseConfigured = eligible, opponents = eligible ? new[] { new ShadowArenaOpponentDto { playerId = "offline-shadow-bot", displayName = "Shadow Training Team", rating = state.shadowRating, totalPower = 150000, training = true } } : Array.Empty<ShadowArenaOpponentDto>() });
        }

        public Task<CompetitiveHistoryItemDto[]> GetArenaHistoryAsync(string playerId, int limit = 20) => Task.FromResult(Array.Empty<CompetitiveHistoryItemDto>());
        public Task<CompetitiveHistoryItemDto[]> GetShadowArenaHistoryAsync(string playerId, int limit = 20) => Task.FromResult(Array.Empty<CompetitiveHistoryItemDto>());

        public Task<ShopViewDto> GetShopAsync(string playerId)
        {
            EnsureState();
            return Task.FromResult(new ShopViewDto { catalogVersion = "offline-shop-v1", resetKey = "offline", shops = new[] { new ShopEntryDto { shopId = "offline-general", nameEn = "Offline Shop", nameVi = "Cửa hàng Offline", refreshProfile = "NONE", offers = new[] { new ShopOfferDto { offerId = "ramen-pack", itemId = "ramen", itemNameEn = "Ramen", itemNameVi = "Mì Ramen", quantity = 5, currency = "GOLD", price = 1000, purchaseLimit = -1, purchasedCount = 0, remaining = -1, purchasable = state.gold >= 1000, blockedReason = state.gold >= 1000 ? string.Empty : "GOLD" } } } } });
        }

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
            bool read = claimed || state.readMailIds.Contains("offline-welcome");
            return Task.FromResult(new MailboxDto { mailProfileVersion = "offline-mail-v1", mails = new[] { new MailDto { mailId = "offline-welcome", subject = "Offline Playtest", body = "Welcome to the standalone playtest build.", attachments = new[] { new MailAttachmentDto { kind = "CURRENCY", id = "DIAMOND", quantity = 200 } }, read = read, claimed = claimed, createdAt = DateTime.UtcNow.ToString("O"), expiresAt = string.Empty } } });
        }

        public Task<ArenaDefenseDto> SaveArenaDefenseAsync(string playerId, string[] heroIds) => Task.FromResult(new ArenaDefenseDto { formationId = "offline-defense", heroes = ResolveFormation() });

        public Task<ArenaBattleDto> FightArenaAsync(string playerId, string opponentPlayerId, string requestId)
        {
            EnsureState();
            long before = state.arenaRating;
            long seed = OfflineBattleSimulator.StableSeed("arena-" + opponentPlayerId, ResolveFormation().Select(hero => hero.id), state.battleCount);
            PlayBattleDto simulated = OfflineBattleSimulator.Simulate("arena-training", ResolveFormation(), seed, 0, 0, 0);
            state.arenaRating += 10;
            state.battleCount++;
            saves.Save(state);
            return Task.FromResult(new ArenaBattleDto { requestId = requestId, battleId = simulated.battleId, playerId = state.playerId, seasonId = "offline", replayed = false, training = true, opponentPlayerId = opponentPlayerId, opponentDisplayName = "Training Team", opponentRating = before, opponentPower = 50000, ratingBefore = before, ratingAfter = state.arenaRating, ratingDelta = state.arenaRating - before, ratingProfileVersion = "offline", arenaCoinReward = 0, rewardProfileVersion = "offline", combatStatsVersion = simulated.combatStatsVersion, abilityProfileVersion = simulated.abilityProfileVersion, techniqueMappingVersion = simulated.techniqueMappingVersion, passiveProfileVersion = simulated.passiveProfileVersion, participants = simulated.participants, battle = simulated.battle });
        }

        public Task<SeasonRewardDto> ClaimArenaSeasonAsync(string playerId) { EnsureState(); return Task.FromResult(new SeasonRewardDto { seasonId = "offline", finalRating = state.arenaRating, rewardAmount = 0, claimable = false, claimed = true }); }
        public Task<ShadowDefenseDto> SaveShadowDefenseAsync(string playerId, string[] heroIds) { EnsureState(); return Task.FromResult(new ShadowDefenseDto { seasonId = "offline", heroes = state.heroes.Take(15).ToArray() }); }

        public Task<ShadowArenaBattleDto> FightShadowArenaAsync(string playerId, string opponentPlayerId, string requestId)
        {
            EnsureState();
            if (state.heroes.Length < 15) throw new InvalidOperationException("Shadow Arena requires 15 owned ninja");
            long before = state.shadowRating;
            long masterSeed = OfflineBattleSimulator.StableSeed("shadow-" + opponentPlayerId, state.heroes.Take(15).Select(hero => hero.id), state.battleCount);
            var squads = new ShadowSquadBattleDto[3];
            for (int squad = 0; squad < 3; squad++)
            {
                OwnedHeroDto[] formation = state.heroes.Skip(squad * 5).Take(5).ToArray();
                long seed = masterSeed + squad + 1;
                PlayBattleDto simulated = OfflineBattleSimulator.Simulate("shadow-training-" + squad, formation, seed, 0, 0, 0);
                squads[squad] = new ShadowSquadBattleDto { squadIndex = squad, seed = seed, playerWon = true, tiebreak = string.Empty, participants = simulated.participants, battle = simulated.battle };
            }
            state.shadowRating += 12;
            state.battleCount++;
            saves.Save(state);
            return Task.FromResult(new ShadowArenaBattleDto { requestId = requestId, battleId = "offline-shadow-" + masterSeed, playerId = state.playerId, seasonId = "offline", replayed = false, training = true, opponentPlayerId = opponentPlayerId, opponentRating = before, opponentPower = 150000, masterSeed = masterSeed, winner = state.playerId, ratingBefore = before, ratingAfter = state.shadowRating, ratingDelta = state.shadowRating - before, ratingProfileVersion = "offline", seriesRulesVersion = "offline", shadowCoinReward = 0, rewardProfileVersion = "offline", combatStatsVersion = "offline-stats-v1", abilityProfileVersion = "offline-ability-v1", techniqueMappingVersion = "offline-technique-v1", passiveProfileVersion = "offline-passive-v1", squads = squads });
        }

        public Task<SeasonRewardDto> ClaimShadowSeasonAsync(string playerId) { EnsureState(); return Task.FromResult(new SeasonRewardDto { seasonId = "offline", finalRating = state.shadowRating, rewardAmount = 0, claimable = false, claimed = true }); }

        public Task<ShopPurchaseResultDto> PurchaseShopAsync(string playerId, string shopId, string offerId, string requestId)
        {
            EnsureState();
            if (shopId != "offline-general" || offerId != "ramen-pack") throw new InvalidOperationException("Unknown offline shop offer");
            if (state.gold < 1000) throw new InvalidOperationException("Not enough Gold");
            state.gold -= 1000;
            state.ramen += 5;
            saves.Save(state);
            return Task.FromResult(new ShopPurchaseResultDto { shopId = shopId, offerId = offerId, resetKey = "offline", replayed = false, currency = "GOLD", charged = 1000, itemBalanceAfter = state.ramen, purchaseCount = 1 });
        }

        public Task<QuestClaimDto> ClaimQuestAsync(string playerId, string questId)
        {
            EnsureState();
            if (questId != "offline-battle") throw new InvalidOperationException("Unknown offline quest");
            bool already = state.claimedQuestIds.Contains(questId);
            if (!already && state.battleCount < 1) throw new InvalidOperationException("Quest is not complete");
            if (!already)
            {
                state.gold += 5000;
                state.diamond += 50;
                state.claimedQuestIds = state.claimedQuestIds.Concat(new[] { questId }).Distinct().ToArray();
                saves.Save(state);
            }
            return Task.FromResult(new QuestClaimDto { questId = questId, resetKey = "offline", replayed = already, gold = already ? 0 : 5000, diamond = already ? 0 : 50, itemId = string.Empty, itemQuantity = 0, finalValue = state.battleCount });
        }

        public Task ReadMailAsync(string playerId, string mailId)
        {
            EnsureState();
            if (!state.readMailIds.Contains(mailId)) state.readMailIds = state.readMailIds.Concat(new[] { mailId }).Distinct().ToArray();
            saves.Save(state);
            return Task.CompletedTask;
        }

        public Task<MailClaimDto> ClaimMailAsync(string playerId, string mailId)
        {
            EnsureState();
            if (mailId != "offline-welcome") throw new InvalidOperationException("Unknown offline mail");
            bool already = state.claimedMailIds.Contains(mailId);
            if (!already)
            {
                state.diamond += 200;
                state.claimedMailIds = state.claimedMailIds.Concat(new[] { mailId }).Distinct().ToArray();
                state.readMailIds = state.readMailIds.Concat(new[] { mailId }).Distinct().ToArray();
                saves.Save(state);
            }
            return Task.FromResult(new MailClaimDto { mailId = mailId, replayed = already, grants = already ? Array.Empty<MailGrantDto>() : new[] { new MailGrantDto { kind = "CURRENCY", id = "DIAMOND", quantity = 200, balanceAfter = state.diamond } } });
        }

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
            else state.heroes = state.heroes.Concat(new[] { new OwnedHeroDto { id = "offline-summon-" + state.summonCount, characterId = characterId, heroId = heroId, displayName = name, level = 1, exp = 0, frameTier = "BLUE", awakened = false, currentVariant = "BASE", awakeningLevel = 0 } }).ToArray();
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
            OwnedHeroDto[] selected = state.formationIds.Select(id => state.heroes.FirstOrDefault(hero => hero.id == id)).Where(hero => hero != null).Take(5).ToArray();
            if (selected.Length == 5) return selected;
            selected = state.heroes.Take(5).ToArray();
            state.formationIds = selected.Select(hero => hero.id).ToArray();
            saves.Save(state);
            return selected;
        }

        private static AwakeningVisualDto OfflineAwakeningVisual(OwnedHeroDto hero)
        {
            return new AwakeningVisualDto { awakeningId = hero.awakeningId, heroId = hero.heroId, transitionStart = "offline", transitionMid = "offline", transitionEnd = "offline", idleAnimation = "offline", movementAnimation = "offline", basicVfxModifier = "offline", skill1VfxModifier = "offline", skill2VfxModifier = "offline", ultimateVfxModifier = "offline", awakeningSkillVfx = "offline", cameraSequence = "offline", screenEffect = "offline", sfxDescription = "Offline playtest fallback", referenceSource = "offline-playtest", status = "OFFLINE_TEST" };
        }

        private static string Slug(string value) => (value ?? string.Empty).Trim().ToLowerInvariant().Replace(' ', '-');

        private void EnsureState()
        {
            if (state == null) state = saves.LoadOrCreate();
            if (state.heroes == null) state.heroes = Array.Empty<OwnedHeroDto>();
            if (state.formationIds == null) state.formationIds = Array.Empty<string>();
            if (state.clearedStageIds == null) state.clearedStageIds = Array.Empty<string>();
            if (state.claimedQuestIds == null) state.claimedQuestIds = Array.Empty<string>();
            if (state.claimedMailIds == null) state.claimedMailIds = Array.Empty<string>();
            if (state.readMailIds == null) state.readMailIds = Array.Empty<string>();
            if (state.arenaRating <= 0) state.arenaRating = 1000;
            if (state.shadowRating <= 0) state.shadowRating = 1000;
        }
    }
}
