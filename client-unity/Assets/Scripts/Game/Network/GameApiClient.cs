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

        public Task<PlayerDto> LoginGuestAsync(string guestKey, string displayName)
        {
            return PostJsonAsync<PlayerDto>("/api/v1/players/guest", JsonUtility.ToJson(new GuestLoginRequest { guestKey = guestKey, displayName = displayName }));
        }

        private async Task<T> GetAsync<T>(string path)
        {
            using UnityWebRequest request = UnityWebRequest.Get(baseUrl + path);
            await Send(request);
            return JsonUtility.FromJson<T>(request.downloadHandler.text);
        }

        private async Task<T[]> GetArrayAsync<T>(string path)
        {
            using UnityWebRequest request = UnityWebRequest.Get(baseUrl + path);
            await Send(request);
            string wrapped = "{\"items\":" + request.downloadHandler.text + "}";
            return JsonUtility.FromJson<ArrayEnvelope<T>>(wrapped)?.items ?? Array.Empty<T>();
        }

        private async Task<T> PostJsonAsync<T>(string path, string json)
        {
            using UnityWebRequest request = new UnityWebRequest(baseUrl + path, UnityWebRequest.kHttpVerbPOST);
            request.uploadHandler = new UploadHandlerRaw(Encoding.UTF8.GetBytes(json));
            request.downloadHandler = new DownloadHandlerBuffer();
            request.SetRequestHeader("Content-Type", "application/json");
            await Send(request);
            return JsonUtility.FromJson<T>(request.downloadHandler.text);
        }

        private static async Task Send(UnityWebRequest request)
        {
            UnityWebRequestAsyncOperation operation = request.SendWebRequest();
            while (!operation.isDone) await Task.Yield();
            if (request.result != UnityWebRequest.Result.Success)
                throw new InvalidOperationException($"HTTP {(long)request.responseCode}: {request.error} :: {request.downloadHandler?.text}");
        }

        private static string Escape(string value) => UnityWebRequest.EscapeURL(value ?? string.Empty);
    }
}
