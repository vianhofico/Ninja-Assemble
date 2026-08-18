using System.Threading.Tasks;
using NinjaAssemble.Network;

namespace NinjaAssemble.Playable
{
    public interface IPlayableGameService
    {
        Task<PlayerDto> LoginGuestAsync(string guestKey, string displayName);
        Task<BootstrapDto> BootstrapAsync(string playerId);
        Task<OwnedHeroDto[]> GetOwnedHeroesAsync(string playerId);
        Task<FormationDto> SaveFormationAsync(string playerId, string[] heroIds);
        Task<CampaignStageListDto> GetCampaignStagesAsync(string playerId);
        Task<PlayBattleDto> PlayCampaignStageAsync(string playerId, string stageId);
        Task<CampaignSweepDto> SweepCampaignStageAsync(string playerId, string stageId, string requestId);
        Task<ResourcePveBoardDto> GetResourcePveAsync(string playerId);
        Task<ResourcePveBattleDto> PlayResourcePveAsync(string playerId, string modeId, string requestId);
        Task<InventoryViewDto> GetInventoryAsync(string playerId);
        Task<ArenaStateDto> GetArenaAsync(string playerId);
        Task<ArenaDefenseDto> SaveArenaDefenseAsync(string playerId, string[] heroIds);
        Task<ArenaBattleDto> FightArenaAsync(string playerId, string opponentPlayerId, string requestId);
        Task<CompetitiveHistoryItemDto[]> GetArenaHistoryAsync(string playerId, int limit = 20);
        Task<SeasonRewardDto> ClaimArenaSeasonAsync(string playerId);
        Task<ShadowArenaStateDto> GetShadowArenaAsync(string playerId);
        Task<ShadowDefenseDto> SaveShadowDefenseAsync(string playerId, string[] heroIds);
        Task<ShadowArenaBattleDto> FightShadowArenaAsync(string playerId, string opponentPlayerId, string requestId);
        Task<CompetitiveHistoryItemDto[]> GetShadowArenaHistoryAsync(string playerId, int limit = 20);
        Task<SeasonRewardDto> ClaimShadowSeasonAsync(string playerId);
        Task<ShopViewDto> GetShopAsync(string playerId);
        Task<ShopPurchaseResultDto> PurchaseShopAsync(string playerId, string shopId, string offerId, string requestId);
        Task<QuestBoardDto> GetQuestsAsync(string playerId);
        Task<QuestClaimDto> ClaimQuestAsync(string playerId, string questId);
        Task<MailboxDto> GetMailAsync(string playerId);
        Task ReadMailAsync(string playerId, string mailId);
        Task<MailClaimDto> ClaimMailAsync(string playerId, string mailId);
        Task<SummonResultDto> SummonAsync(string playerId, string requestId);
        Task<UpgradeResultDto> LevelUpAsync(string playerId, string playerHeroId, string requestId);
    }
}
