using System.Threading.Tasks;
using NinjaAssemble.Network;

namespace NinjaAssemble.Playable
{
    public sealed class OnlinePlayableGameService : IPlayableGameService
    {
        private readonly GameApiClient client;
        public OnlinePlayableGameService(GameApiConfig config) => client = new GameApiClient(config);
        public Task<PlayerDto> LoginGuestAsync(string guestKey, string displayName) => client.LoginGuestAsync(guestKey, displayName);
        public Task<BootstrapDto> BootstrapAsync(string playerId) => client.BootstrapAsync(playerId);
        public Task<OwnedHeroDto[]> GetOwnedHeroesAsync(string playerId) => client.GetOwnedHeroesAsync(playerId);
        public Task<FormationDto> SaveFormationAsync(string playerId, string[] heroIds) => client.SaveFormationAsync(playerId, heroIds);
        public Task<CampaignStageListDto> GetCampaignStagesAsync(string playerId) => client.GetCampaignStagesAsync(playerId);
        public Task<PlayBattleDto> PlayCampaignStageAsync(string playerId, string stageId) => client.PlayCampaignStageAsync(playerId, stageId);
        public Task<CampaignSweepDto> SweepCampaignStageAsync(string playerId, string stageId, string requestId) => client.SweepCampaignStageAsync(playerId, stageId, requestId);
        public Task<ResourcePveBoardDto> GetResourcePveAsync(string playerId) => client.GetResourcePveAsync(playerId);
        public Task<ResourcePveBattleDto> PlayResourcePveAsync(string playerId, string modeId, string requestId) => client.PlayResourcePveAsync(playerId, modeId, requestId);
        public Task<InventoryViewDto> GetInventoryAsync(string playerId) => client.GetInventoryAsync(playerId);
        public Task<ArenaStateDto> GetArenaAsync(string playerId) => client.GetArenaAsync(playerId);
        public Task<ArenaDefenseDto> SaveArenaDefenseAsync(string playerId, string[] heroIds) => client.SaveArenaDefenseAsync(playerId, heroIds);
        public Task<ArenaBattleDto> FightArenaAsync(string playerId, string opponentPlayerId, string requestId) => client.FightArenaAsync(playerId, opponentPlayerId, requestId);
        public Task<CompetitiveHistoryItemDto[]> GetArenaHistoryAsync(string playerId, int limit = 20) => client.GetArenaHistoryAsync(playerId, limit);
        public Task<SeasonRewardDto> ClaimArenaSeasonAsync(string playerId) => client.ClaimArenaSeasonAsync(playerId);
        public Task<ShadowArenaStateDto> GetShadowArenaAsync(string playerId) => client.GetShadowArenaAsync(playerId);
        public Task<ShadowDefenseDto> SaveShadowDefenseAsync(string playerId, string[] heroIds) => client.SaveShadowDefenseAsync(playerId, heroIds);
        public Task<ShadowArenaBattleDto> FightShadowArenaAsync(string playerId, string opponentPlayerId, string requestId) => client.FightShadowArenaAsync(playerId, opponentPlayerId, requestId);
        public Task<CompetitiveHistoryItemDto[]> GetShadowArenaHistoryAsync(string playerId, int limit = 20) => client.GetShadowArenaHistoryAsync(playerId, limit);
        public Task<SeasonRewardDto> ClaimShadowSeasonAsync(string playerId) => client.ClaimShadowSeasonAsync(playerId);
        public Task<ShopViewDto> GetShopAsync(string playerId) => client.GetShopAsync(playerId);
        public Task<ShopPurchaseResultDto> PurchaseShopAsync(string playerId, string shopId, string offerId, string requestId) => client.PurchaseShopAsync(playerId, shopId, offerId, requestId);
        public Task<QuestBoardDto> GetQuestsAsync(string playerId) => client.GetQuestsAsync(playerId);
        public Task<QuestClaimDto> ClaimQuestAsync(string playerId, string questId) => client.ClaimQuestAsync(playerId, questId);
        public Task<MailboxDto> GetMailAsync(string playerId) => client.GetMailAsync(playerId);
        public Task ReadMailAsync(string playerId, string mailId) => client.ReadMailAsync(playerId, mailId);
        public Task<MailClaimDto> ClaimMailAsync(string playerId, string mailId) => client.ClaimMailAsync(playerId, mailId);
        public Task<SummonResultDto> SummonAsync(string playerId, string requestId) => client.SummonAsync(playerId, requestId);
        public Task<UpgradeResultDto> LevelUpAsync(string playerId, string playerHeroId, string requestId) => client.LevelUpAsync(playerId, playerHeroId, requestId);
    }
}
