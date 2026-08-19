using System;
using System.Linq;
using System.Threading.Tasks;
using NinjaAssemble.Network;

namespace NinjaAssemble.Playable
{
    public sealed class PlayableGameStore
    {
        private readonly IPlayableGameService api;
        public string PlayerId { get; private set; }
        public OwnedHeroDto[] Heroes { get; private set; } = Array.Empty<OwnedHeroDto>();
        public FormationDto Formation { get; private set; }
        public CampaignStageListDto Campaign { get; private set; }
        public ResourcePveBoardDto ResourcePve { get; private set; }
        public InventoryViewDto Inventory { get; private set; }
        public ArenaStateDto Arena { get; private set; }
        public ShadowArenaStateDto ShadowArena { get; private set; }
        public CompetitiveHistoryItemDto[] ArenaHistory { get; private set; } = Array.Empty<CompetitiveHistoryItemDto>();
        public CompetitiveHistoryItemDto[] ShadowArenaHistory { get; private set; } = Array.Empty<CompetitiveHistoryItemDto>();
        public ShopViewDto Shop { get; private set; }
        public QuestBoardDto Quests { get; private set; }
        public MailboxDto Mail { get; private set; }
        public long Gold { get; private set; }
        public long Diamond { get; private set; }
        public int Energy { get; private set; }

        public CampaignStageDto RecommendedStage { get { CampaignStageDto[] stages = Campaign?.stages ?? Array.Empty<CampaignStageDto>(); return stages.FirstOrDefault(stage => stage != null && stage.unlocked && stage.clearCount == 0) ?? stages.LastOrDefault(stage => stage != null && stage.unlocked); } }
        public CampaignStageDto SweepableStage { get { CampaignStageDto[] stages = Campaign?.stages ?? Array.Empty<CampaignStageDto>(); return stages.LastOrDefault(stage => stage != null && stage.unlocked && stage.clearCount > 0 && stage.energyCost <= Energy); } }
        public ResourcePveModeDto RecommendedResourcePve => (ResourcePve?.modes ?? Array.Empty<ResourcePveModeDto>()).FirstOrDefault(mode => mode != null && mode.playable);
        public ArenaOpponentDto RecommendedArenaOpponent => Arena?.opponents?.FirstOrDefault(opponent => opponent != null);
        public ShadowArenaOpponentDto RecommendedShadowOpponent => ShadowArena?.eligible == true ? ShadowArena.opponents?.FirstOrDefault(opponent => opponent != null) : null;
        public QuestDto ClaimableQuest => Quests?.quests?.FirstOrDefault(quest => quest != null && quest.claimable);
        public MailDto ClaimableMail => Mail?.mails?.FirstOrDefault(mail => mail != null && !mail.claimed && (mail.attachments?.Length ?? 0) > 0);

        public PlayableGameStore(IPlayableGameService api) => this.api = api ?? throw new ArgumentNullException(nameof(api));

        public async Task LoginAndBootstrapAsync(string guestKey, string displayName)
        {
            PlayerDto player = await api.LoginGuestAsync(guestKey, displayName);
            if (player == null || string.IsNullOrWhiteSpace(player.id)) throw new InvalidOperationException("Login returned no player id");
            PlayerId = player.id;

            BootstrapDto bootstrap = await api.BootstrapAsync(PlayerId);
            if (bootstrap == null) throw new InvalidOperationException("Bootstrap returned no player state");
            Heroes = bootstrap.heroes ?? Array.Empty<OwnedHeroDto>();
            Gold = bootstrap.gold;
            Diamond = bootstrap.diamond;
            Energy = bootstrap.energy;

            if (Heroes.Length >= 5)
                Formation = await api.SaveFormationAsync(PlayerId, Heroes.Where(h => h != null && !string.IsNullOrWhiteSpace(h.id)).Take(5).Select(h => h.id).ToArray());

            await Task.WhenAll(RefreshCampaignAsync(), RefreshResourcePveAsync(), RefreshInventoryAsync(), RefreshArenaAsync(), RefreshShadowArenaAsync(), RefreshShopAsync(), RefreshQuestsAsync(), RefreshMailAsync());
        }

        public Task<PlayBattleDto> BattleAsync() => BattleCampaignAsync("c1-s1");
        public async Task<PlayBattleDto> BattleCampaignAsync(string stageId)
        {
            if (string.IsNullOrWhiteSpace(stageId)) throw new ArgumentException("stageId is required", nameof(stageId));
            PlayBattleDto result = await api.PlayCampaignStageAsync(PlayerId, stageId) ?? throw new InvalidOperationException("Campaign battle returned no result");
            Gold += result.goldReward; Diamond += result.diamondReward; Energy = Math.Max(0, Energy - result.energyCost);
            await Task.WhenAll(RefreshCampaignAsync(), RefreshInventoryAsync(), RefreshShopAsync(), RefreshQuestsAsync()); return result;
        }
        public async Task<CampaignSweepDto> SweepCampaignAsync(string stageId)
        {
            if (string.IsNullOrWhiteSpace(stageId)) throw new ArgumentException("stageId is required", nameof(stageId));
            CampaignSweepDto result = await api.SweepCampaignStageAsync(PlayerId, stageId, Guid.NewGuid().ToString()) ?? throw new InvalidOperationException("Campaign sweep returned no result");
            if (!result.replayed) { Gold += result.goldReward; Diamond += result.diamondReward; }
            Energy = result.energyAfter;
            await Task.WhenAll(RefreshCampaignAsync(), RefreshInventoryAsync(), RefreshShopAsync(), RefreshQuestsAsync());
            return result;
        }
        public async Task<ResourcePveBattleDto> BattleResourcePveAsync(string modeId)
        {
            if (string.IsNullOrWhiteSpace(modeId)) throw new ArgumentException("modeId is required", nameof(modeId));
            ResourcePveBattleDto result = await api.PlayResourcePveAsync(PlayerId, modeId, Guid.NewGuid().ToString()) ?? throw new InvalidOperationException("Resource PvE returned no result");
            if (!result.replayed && result.won) Gold += result.goldReward;
            Energy = result.energyAfter;
            await Task.WhenAll(RefreshResourcePveAsync(), RefreshInventoryAsync(), RefreshShopAsync(), RefreshQuestsAsync());
            return result;
        }
        public async Task RefreshCampaignAsync() { Campaign = await api.GetCampaignStagesAsync(PlayerId); if (Campaign != null) Energy = Campaign.energy; }
        public async Task RefreshResourcePveAsync() { ResourcePve = await api.GetResourcePveAsync(PlayerId); if (ResourcePve != null) Energy = ResourcePve.energy; }
        public async Task RefreshInventoryAsync() => Inventory = await api.GetInventoryAsync(PlayerId);
        public async Task RefreshArenaAsync() => Arena = await api.GetArenaAsync(PlayerId);
        public async Task RefreshShadowArenaAsync() => ShadowArena = await api.GetShadowArenaAsync(PlayerId);
        public async Task RefreshArenaHistoryAsync() => ArenaHistory = await api.GetArenaHistoryAsync(PlayerId) ?? Array.Empty<CompetitiveHistoryItemDto>();
        public async Task RefreshShadowArenaHistoryAsync() => ShadowArenaHistory = await api.GetShadowArenaHistoryAsync(PlayerId) ?? Array.Empty<CompetitiveHistoryItemDto>();
        public async Task RefreshShopAsync() => Shop = await api.GetShopAsync(PlayerId);
        public async Task RefreshQuestsAsync() => Quests = await api.GetQuestsAsync(PlayerId);
        public async Task RefreshMailAsync() => Mail = await api.GetMailAsync(PlayerId);

        public async Task<ArenaDefenseDto> SaveArenaDefenseAsync()
        {
            string[] ids = (Formation?.heroes ?? Array.Empty<OwnedHeroDto>()).Where(hero => hero != null && !string.IsNullOrWhiteSpace(hero.id)).Select(hero => hero.id).ToArray();
            if (ids.Length != 5) throw new InvalidOperationException("Arena defense requires the saved five-ninja formation");
            ArenaDefenseDto result = await api.SaveArenaDefenseAsync(PlayerId, ids) ?? throw new InvalidOperationException("Arena defense save returned no result"); await RefreshArenaAsync(); return result;
        }
        public async Task<ShadowDefenseDto> SaveShadowDefenseAsync()
        {
            if (Heroes.Length < 15) throw new InvalidOperationException("Shadow Arena defense requires 15 owned ninja");
            ShadowDefenseDto result = await api.SaveShadowDefenseAsync(PlayerId, Heroes.Where(hero => hero != null && !string.IsNullOrWhiteSpace(hero.id)).Take(15).Select(hero => hero.id).ToArray()) ?? throw new InvalidOperationException("Shadow defense save returned no result"); await RefreshShadowArenaAsync(); return result;
        }
        public async Task<ArenaBattleDto> FightArenaAsync(string opponentPlayerId)
        {
            if (string.IsNullOrWhiteSpace(opponentPlayerId)) throw new ArgumentException("opponentPlayerId is required", nameof(opponentPlayerId));
            ArenaBattleDto result = await api.FightArenaAsync(PlayerId, opponentPlayerId, Guid.NewGuid().ToString()) ?? throw new InvalidOperationException("Arena battle returned no result");
            await Task.WhenAll(RefreshArenaAsync(), RefreshArenaHistoryAsync(), RefreshShopAsync(), RefreshQuestsAsync()); return result;
        }
        public async Task<ShadowArenaBattleDto> FightShadowArenaAsync(string opponentPlayerId)
        {
            if (ShadowArena?.eligible != true) throw new InvalidOperationException("Shadow Arena requires 15 owned ninja");
            if (string.IsNullOrWhiteSpace(opponentPlayerId)) throw new ArgumentException("opponentPlayerId is required", nameof(opponentPlayerId));
            ShadowArenaBattleDto result = await api.FightShadowArenaAsync(PlayerId, opponentPlayerId, Guid.NewGuid().ToString()) ?? throw new InvalidOperationException("Shadow Arena battle returned no result");
            await Task.WhenAll(RefreshShadowArenaAsync(), RefreshShadowArenaHistoryAsync(), RefreshShopAsync()); return result;
        }
        public async Task<SeasonRewardDto> ClaimArenaSeasonAsync() { SeasonRewardDto result = await api.ClaimArenaSeasonAsync(PlayerId) ?? throw new InvalidOperationException("Arena season claim returned no result"); await Task.WhenAll(RefreshArenaAsync(), RefreshShopAsync()); return result; }
        public async Task<SeasonRewardDto> ClaimShadowSeasonAsync() { SeasonRewardDto result = await api.ClaimShadowSeasonAsync(PlayerId) ?? throw new InvalidOperationException("Shadow season claim returned no result"); await Task.WhenAll(RefreshShadowArenaAsync(), RefreshShopAsync()); return result; }

        public async Task<ShopPurchaseResultDto> PurchaseShopAsync(string shopId, string offerId)
        {
            ShopPurchaseResultDto result = await api.PurchaseShopAsync(PlayerId, shopId, offerId, Guid.NewGuid().ToString()) ?? throw new InvalidOperationException("Shop purchase returned no result");
            if (!result.replayed && string.Equals(result.currency, "GOLD", StringComparison.OrdinalIgnoreCase)) Gold = Math.Max(0, Gold - result.charged);
            await Task.WhenAll(RefreshShopAsync(), RefreshInventoryAsync()); return result;
        }
        public async Task<QuestClaimDto> ClaimQuestAsync(string questId)
        {
            QuestClaimDto result = await api.ClaimQuestAsync(PlayerId, questId) ?? throw new InvalidOperationException("Quest claim returned no result");
            if (!result.replayed) { Gold += result.gold; Diamond += result.diamond; }
            await Task.WhenAll(RefreshQuestsAsync(), RefreshInventoryAsync(), RefreshShopAsync()); return result;
        }
        public async Task<MailClaimDto> ClaimMailAsync(string mailId)
        {
            MailClaimDto result = await api.ClaimMailAsync(PlayerId, mailId) ?? throw new InvalidOperationException("Mail claim returned no result");
            if (!result.replayed)
            {
                foreach (MailGrantDto grant in result.grants ?? Array.Empty<MailGrantDto>())
                {
                    if (grant == null || !string.Equals(grant.kind, "CURRENCY", StringComparison.OrdinalIgnoreCase)) continue;
                    if (string.Equals(grant.id, "GOLD", StringComparison.OrdinalIgnoreCase)) Gold += grant.quantity;
                    if (string.Equals(grant.id, "DIAMOND", StringComparison.OrdinalIgnoreCase)) Diamond += grant.quantity;
                }
            }
            await Task.WhenAll(RefreshMailAsync(), RefreshInventoryAsync(), RefreshShopAsync()); return result;
        }
        public async Task ReadMailAsync(string mailId) { await api.ReadMailAsync(PlayerId, mailId); await RefreshMailAsync(); }

        public async Task<SummonResultDto> SummonAsync()
        {
            SummonResultDto result = await api.SummonAsync(PlayerId, Guid.NewGuid().ToString()) ?? throw new InvalidOperationException("Summon returned no result");
            Diamond -= CompleteRosterSummonCost;
            Heroes = await api.GetOwnedHeroesAsync(PlayerId) ?? Array.Empty<OwnedHeroDto>();
            await Task.WhenAll(RefreshShopAsync(), RefreshQuestsAsync(), RefreshShadowArenaAsync()); return result;
        }
        public async Task<UpgradeResultDto> LevelUpAsync(string playerHeroId)
        {
            UpgradeResultDto result = await api.LevelUpAsync(PlayerId, playerHeroId, Guid.NewGuid().ToString()) ?? throw new InvalidOperationException("Level up returned no result");
            Gold -= result.goldCost;
            Heroes = await api.GetOwnedHeroesAsync(PlayerId) ?? Array.Empty<OwnedHeroDto>();
            await Task.WhenAll(RefreshShopAsync(), RefreshQuestsAsync(), RefreshShadowArenaAsync()); return result;
        }

        public const long CompleteRosterSummonCost = 200;
    }
}
