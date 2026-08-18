using System;
using System.Text;
using System.Threading.Tasks;
using UnityEngine;
using UnityEngine.Networking;

namespace NinjaAssemble.Network
{
    public sealed class GameApiClient
    {
        private readonly string baseUrl;
        public GameApiClient(GameApiConfig config)
        {
            if (config == null || string.IsNullOrWhiteSpace(config.BaseUrl)) throw new ArgumentException("API config/base URL required", nameof(config));
            baseUrl = config.BaseUrl;
        }

        public Task<HeroCatalogDto[]> GetHeroCatalogAsync() => GetArrayAsync<HeroCatalogDto>("/api/v1/heroes/catalog");
        public Task<HeroVariantDto[]> GetVariantsAsync(string characterId) => GetArrayAsync<HeroVariantDto>($"/api/v1/heroes/{Escape(characterId)}/variants");
        public Task<HeroKitDto> GetKitAsync(string characterId, string variant = null)
        {
            string path = $"/api/v1/heroes/{Escape(characterId)}/kit";
            if (!string.IsNullOrWhiteSpace(variant)) path += "?variant=" + Escape(variant);
            return GetAsync<HeroKitDto>(path);
        }
        public Task<PlayerDto> LoginGuestAsync(string guestKey, string displayName) => PostJsonAsync<PlayerDto>("/api/v1/players/guest", JsonUtility.ToJson(new GuestLoginRequest { guestKey = guestKey, displayName = displayName }));
        public Task<BootstrapDto> BootstrapAsync(string playerId) => PostJsonAsync<BootstrapDto>($"/api/v1/play/{Escape(playerId)}/bootstrap", "{}");
        public Task<OwnedHeroDto[]> GetOwnedHeroesAsync(string playerId) => GetArrayAsync<OwnedHeroDto>($"/api/v1/play/{Escape(playerId)}/heroes");
        public Task<FormationDto> SaveFormationAsync(string playerId, string[] heroIds) => PutJsonAsync<FormationDto>($"/api/v1/play/{Escape(playerId)}/formation", JsonUtility.ToJson(new FormationRequestDto { playerHeroIds = heroIds }));
        public Task<CampaignStageListDto> GetCampaignStagesAsync(string playerId) => GetAsync<CampaignStageListDto>($"/api/v1/play/{Escape(playerId)}/campaign/stages");
        public Task<PlayBattleDto> PlayCampaignStageAsync(string playerId, string stageId) => PostJsonAsync<PlayBattleDto>($"/api/v1/play/{Escape(playerId)}/campaign/stages/{Escape(stageId)}/battle", "{}");
        public Task<CampaignSweepDto> SweepCampaignStageAsync(string playerId, string stageId, string requestId) => PostJsonAsync<CampaignSweepDto>($"/api/v1/play/{Escape(playerId)}/campaign/stages/{Escape(stageId)}/sweep", JsonUtility.ToJson(new ActionRequestDto { requestId = requestId }));
        public Task<ResourcePveBoardDto> GetResourcePveAsync(string playerId) => GetAsync<ResourcePveBoardDto>($"/api/v1/play/{Escape(playerId)}/resource-pve");
        public Task<ResourcePveBattleDto> PlayResourcePveAsync(string playerId, string modeId, string requestId) => PostJsonAsync<ResourcePveBattleDto>($"/api/v1/play/{Escape(playerId)}/resource-pve/{Escape(modeId)}/battle", JsonUtility.ToJson(new ActionRequestDto { requestId = requestId }));
        public Task<InventoryViewDto> GetInventoryAsync(string playerId) => GetAsync<InventoryViewDto>($"/api/v1/play/{Escape(playerId)}/inventory");
        public Task<ArenaStateDto> GetArenaAsync(string playerId) => GetAsync<ArenaStateDto>($"/api/v1/play/{Escape(playerId)}/arena");
        public Task<ArenaBattleDto> FightArenaAsync(string playerId, string opponentPlayerId) => PostJsonAsync<ArenaBattleDto>($"/api/v1/play/{Escape(playerId)}/arena/{Escape(opponentPlayerId)}/battle", "{}");
        public Task<ShadowArenaStateDto> GetShadowArenaAsync(string playerId) => GetAsync<ShadowArenaStateDto>($"/api/v1/play/{Escape(playerId)}/shadow-arena");
        public Task<ShadowArenaBattleDto> FightShadowArenaAsync(string playerId, string opponentPlayerId) => PostJsonAsync<ShadowArenaBattleDto>($"/api/v1/play/{Escape(playerId)}/shadow-arena/{Escape(opponentPlayerId)}/battle", "{}");
        public Task<ShopViewDto> GetShopAsync(string playerId) => GetAsync<ShopViewDto>($"/api/v1/play/{Escape(playerId)}/shop");
        public Task<ShopPurchaseResultDto> PurchaseShopAsync(string playerId, string shopId, string offerId, string requestId) => PostJsonAsync<ShopPurchaseResultDto>($"/api/v1/play/{Escape(playerId)}/shop/{Escape(shopId)}/{Escape(offerId)}/purchase", JsonUtility.ToJson(new ActionRequestDto { requestId = requestId }));
        public Task<QuestBoardDto> GetQuestsAsync(string playerId) => GetAsync<QuestBoardDto>($"/api/v1/play/{Escape(playerId)}/quests");
        public Task<QuestClaimDto> ClaimQuestAsync(string playerId, string questId) => PostJsonAsync<QuestClaimDto>($"/api/v1/play/{Escape(playerId)}/quests/{Escape(questId)}/claim", "{}");
        public Task<MailboxDto> GetMailAsync(string playerId) => GetAsync<MailboxDto>($"/api/v1/play/{Escape(playerId)}/mail");
        public async Task ReadMailAsync(string playerId, string mailId) { using UnityWebRequest request = new UnityWebRequest(baseUrl + $"/api/v1/play/{Escape(playerId)}/mail/{Escape(mailId)}/read", UnityWebRequest.kHttpVerbPOST); request.uploadHandler = new UploadHandlerRaw(Array.Empty<byte>()); request.downloadHandler = new DownloadHandlerBuffer(); await Send(request); }
        public Task<MailClaimDto> ClaimMailAsync(string playerId, string mailId) => PostJsonAsync<MailClaimDto>($"/api/v1/play/{Escape(playerId)}/mail/{Escape(mailId)}/claim", "{}");
        public Task<PlayBattleDto> PlayBattleAsync(string playerId) => PostJsonAsync<PlayBattleDto>($"/api/v1/play/{Escape(playerId)}/battle", "{}");
        public Task<SummonResultDto> SummonAsync(string playerId, string requestId) => PostJsonAsync<SummonResultDto>($"/api/v1/play/{Escape(playerId)}/summon", JsonUtility.ToJson(new ActionRequestDto { requestId = requestId }));
        public Task<UpgradeResultDto> LevelUpAsync(string playerId, string playerHeroId, string requestId) => PostJsonAsync<UpgradeResultDto>($"/api/v1/play/{Escape(playerId)}/heroes/{Escape(playerHeroId)}/level-up", JsonUtility.ToJson(new ActionRequestDto { requestId = requestId }));
        public Task<AwakeningViewDto> GetAwakeningAsync(string playerId, string playerHeroId) => GetAsync<AwakeningViewDto>($"/api/v1/play/{Escape(playerId)}/heroes/{Escape(playerHeroId)}/awakening");
        public Task<AwakeningViewDto> AwakenAsync(string playerId, string playerHeroId) => PostJsonAsync<AwakeningViewDto>($"/api/v1/play/{Escape(playerId)}/heroes/{Escape(playerHeroId)}/awakening", "{}");

        [Obsolete("Legacy variant selection is compatibility-only. Use Hero Version ownership + AwakenAsync.")]
        public Task<OwnedHeroDto> SelectVariantAsync(string playerId, string playerHeroId, string variant) => PutJsonAsync<OwnedHeroDto>($"/api/v1/play/{Escape(playerId)}/heroes/{Escape(playerHeroId)}/variant", JsonUtility.ToJson(new VariantRequestDto { variant = variant }));

        private async Task<T> GetAsync<T>(string path) { using UnityWebRequest request = UnityWebRequest.Get(baseUrl + path); await Send(request); return JsonUtility.FromJson<T>(request.downloadHandler.text); }
        private async Task<T[]> GetArrayAsync<T>(string path) { using UnityWebRequest request = UnityWebRequest.Get(baseUrl + path); await Send(request); string wrapped = "{\"items\":" + request.downloadHandler.text + "}"; return JsonUtility.FromJson<ArrayEnvelope<T>>(wrapped)?.items ?? Array.Empty<T>(); }
        private Task<T> PostJsonAsync<T>(string path, string json) => SendJsonAsync<T>(path, UnityWebRequest.kHttpVerbPOST, json);
        private Task<T> PutJsonAsync<T>(string path, string json) => SendJsonAsync<T>(path, UnityWebRequest.kHttpVerbPUT, json);
        private async Task<T> SendJsonAsync<T>(string path, string method, string json) { using UnityWebRequest request = new UnityWebRequest(baseUrl + path, method); request.uploadHandler = new UploadHandlerRaw(Encoding.UTF8.GetBytes(json)); request.downloadHandler = new DownloadHandlerBuffer(); request.SetRequestHeader("Content-Type", "application/json"); await Send(request); return JsonUtility.FromJson<T>(request.downloadHandler.text); }
        private static async Task Send(UnityWebRequest request) { UnityWebRequestAsyncOperation operation = request.SendWebRequest(); while (!operation.isDone) await Task.Yield(); if (request.result != UnityWebRequest.Result.Success) throw new InvalidOperationException($"HTTP {(long)request.responseCode}: {request.error} :: {request.downloadHandler?.text}"); }
        private static string Escape(string value) => UnityWebRequest.EscapeURL(value ?? string.Empty);
    }
}
