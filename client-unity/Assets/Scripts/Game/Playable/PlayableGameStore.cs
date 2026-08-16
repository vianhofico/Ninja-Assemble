using System;
using System.Linq;
using System.Threading.Tasks;
using NinjaAssemble.Network;

namespace NinjaAssemble.Playable
{
    public sealed class PlayableGameStore
    {
        private readonly GameApiClient api;
        public string PlayerId { get; private set; }
        public OwnedHeroDto[] Heroes { get; private set; } = Array.Empty<OwnedHeroDto>();
        public FormationDto Formation { get; private set; }
        public CampaignStageListDto Campaign { get; private set; }
        public InventoryViewDto Inventory { get; private set; }
        public long Gold { get; private set; }
        public long Diamond { get; private set; }
        public int Energy { get; private set; }

        public CampaignStageDto RecommendedStage
        {
            get
            {
                CampaignStageDto[] stages = Campaign?.stages ?? Array.Empty<CampaignStageDto>();
                return stages.FirstOrDefault(stage => stage.unlocked && stage.clearCount == 0)
                       ?? stages.LastOrDefault(stage => stage.unlocked);
            }
        }

        public PlayableGameStore(GameApiClient api) => this.api = api ?? throw new ArgumentNullException(nameof(api));

        public async Task LoginAndBootstrapAsync(string guestKey, string displayName)
        {
            PlayerDto player = await api.LoginGuestAsync(guestKey, displayName);
            PlayerId = player.id;
            BootstrapDto bootstrap = await api.BootstrapAsync(PlayerId);
            Heroes = bootstrap.heroes ?? Array.Empty<OwnedHeroDto>();
            Gold = bootstrap.gold;
            Diamond = bootstrap.diamond;
            Energy = bootstrap.energy;
            if (Heroes.Length >= 5)
                Formation = await api.SaveFormationAsync(PlayerId, Heroes.Take(5).Select(h => h.id).ToArray());
            await Task.WhenAll(RefreshCampaignAsync(), RefreshInventoryAsync());
        }

        public Task<PlayBattleDto> BattleAsync() => BattleCampaignAsync("c1-s1");

        public async Task<PlayBattleDto> BattleCampaignAsync(string stageId)
        {
            if (string.IsNullOrWhiteSpace(stageId)) throw new ArgumentException("stageId is required", nameof(stageId));
            PlayBattleDto result = await api.PlayCampaignStageAsync(PlayerId, stageId);
            Gold += result.goldReward;
            Diamond += result.diamondReward;
            Energy = Math.Max(0, Energy - result.energyCost);
            await Task.WhenAll(RefreshCampaignAsync(), RefreshInventoryAsync());
            return result;
        }

        public async Task RefreshCampaignAsync()
        {
            Campaign = await api.GetCampaignStagesAsync(PlayerId);
            if (Campaign != null) Energy = Campaign.energy;
        }

        public async Task RefreshInventoryAsync()
        {
            Inventory = await api.GetInventoryAsync(PlayerId);
        }

        public async Task<SummonResultDto> SummonAsync()
        {
            SummonResultDto result = await api.SummonAsync(PlayerId, Guid.NewGuid().ToString());
            Diamond -= CompleteRosterSummonCost;
            Heroes = await api.GetOwnedHeroesAsync(PlayerId);
            return result;
        }

        public async Task<UpgradeResultDto> LevelUpAsync(string playerHeroId)
        {
            UpgradeResultDto result = await api.LevelUpAsync(PlayerId, playerHeroId, Guid.NewGuid().ToString());
            Gold -= result.goldCost;
            Heroes = await api.GetOwnedHeroesAsync(PlayerId);
            return result;
        }

        public const long CompleteRosterSummonCost = 200;
    }
}
