using System;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using NinjaAssemble.Bootstrap;
using NinjaAssemble.Network;
using NinjaAssemble.Playable;
using NinjaAssemble.UI;
using TMPro;
using UnityEngine;
using UnityEngine.Networking;
using UnityEngine.UI;

namespace NinjaAssemble.Heroes
{
    public sealed class HeroProgressionPlayableBridge : MonoBehaviour
    {
        private MobileVerticalSliceController controller;
        private TMP_Text statusText;
        private Button primaryAction;
        private Button frameAction;
        private Button evolutionAction;
        private PlayableGameStore store;
        private GameApiClient api;
        private string baseUrl;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        private static void Install()
        {
            var root = new GameObject("HeroProgressionPlayableBridgeRuntime");
            DontDestroyOnLoad(root);
            root.AddComponent<HeroProgressionPlayableBridge>();
        }

        private void Update()
        {
            if (controller != null || !MobileGameBootstrap.IsReady) return;
            controller = FindController(ScreenId.HeroDetail);
            if (controller == null) return;
            store = MobileGameBootstrap.Store;
            api = ResolveApi(store);
            baseUrl = ResolveBaseUrl(api);
            statusText = Field<TMP_Text>(controller, "statusText");
            primaryAction = Field<Button>(controller, "primaryAction");
            frameAction = CloneAction(primaryAction, "FrameAdvanceAction", 2, "FRAME ADVANCE", AdvanceFrame);
            evolutionAction = CloneAction(primaryAction, "EvolutionAction", 3, "EVOLVE", EvolveSelected);
        }

        private OwnedHeroDto SelectedHero
        {
            get
            {
                OwnedHeroDto[] heroes = store?.Heroes ?? Array.Empty<OwnedHeroDto>();
                if (heroes.Length == 0) return null;
                FieldInfo selectedField = typeof(RosterFormationPlayableBridge).GetField("selectedIndex", BindingFlags.Static | BindingFlags.NonPublic);
                int index = selectedField?.GetValue(null) is int value ? value : 0;
                return heroes[Mathf.Clamp(index, 0, heroes.Length - 1)];
            }
        }

        private async void AdvanceFrame()
        {
            OwnedHeroDto hero = SelectedHero;
            if (hero == null) return;
            try
            {
                SetStatus("Advancing " + hero.displayName + " frame...");
                FrameAdvanceResultDto result = await PostAsync<FrameAdvanceResultDto>(
                    $"/api/v1/play/{Escape(store.PlayerId)}/progression/heroes/{Escape(hero.id)}/frame-advance",
                    JsonUtility.ToJson(new RequestDto { requestId = Guid.NewGuid().ToString() }));
                await RefreshHeroes();
                SetStatus($"{hero.displayName} • {result.frameTier} +{result.frameStep} • -{result.goldCost:N0} Gold");
            }
            catch (Exception exception) { Debug.LogException(exception); SetStatus(exception.Message); }
        }

        private async void EvolveSelected()
        {
            OwnedHeroDto hero = SelectedHero;
            if (hero == null) return;
            try
            {
                EvolutionPathDto[] paths = await GetArrayAsync<EvolutionPathDto>(
                    $"/api/v1/play/{Escape(store.PlayerId)}/progression/evolution-paths/{Escape(hero.characterId)}");
                if (paths.Length == 0) { SetStatus("No playable evolution path for this ninja."); return; }
                EvolutionPathDto target = NextPath(paths, hero.currentVariant);
                SetStatus($"Evolving {hero.displayName} → {target.targetVariant}...");
                EvolutionResultDto result = await PostAsync<EvolutionResultDto>(
                    $"/api/v1/play/{Escape(store.PlayerId)}/progression/heroes/{Escape(hero.id)}/evolve",
                    JsonUtility.ToJson(new EvolutionRequestDto { requestId = Guid.NewGuid().ToString(), targetVariant = target.targetVariant }));
                await RefreshHeroes();
                SetStatus(result.alreadyUnlocked
                    ? $"Selected unlocked variant: {result.targetVariant}"
                    : $"Evolved → {result.targetVariant} • -{result.goldCost:N0} Gold");
            }
            catch (Exception exception) { Debug.LogException(exception); SetStatus(exception.Message); }
        }

        private static EvolutionPathDto NextPath(EvolutionPathDto[] paths, string currentVariant)
        {
            string current = string.IsNullOrWhiteSpace(currentVariant) ? "BASE" : currentVariant;
            int currentIndex = Array.FindIndex(paths, path => string.Equals(path.targetVariant, current, StringComparison.OrdinalIgnoreCase));
            if (currentIndex >= 0 && currentIndex + 1 < paths.Length) return paths[currentIndex + 1];
            if (currentIndex == paths.Length - 1) return paths[currentIndex];
            EvolutionPathDto prerequisiteMatch = paths.FirstOrDefault(path => string.Equals(path.prerequisiteVariant, current, StringComparison.OrdinalIgnoreCase));
            return prerequisiteMatch ?? paths[0];
        }

        private async Task RefreshHeroes()
        {
            OwnedHeroDto[] heroes = await api.GetOwnedHeroesAsync(store.PlayerId);
            FieldInfo field = typeof(PlayableGameStore).GetField("<Heroes>k__BackingField", BindingFlags.Instance | BindingFlags.NonPublic);
            if (field == null) throw new InvalidOperationException("Heroes backing field unavailable");
            field.SetValue(store, heroes);
        }

        private static GameApiClient ResolveApi(PlayableGameStore store)
        {
            FieldInfo field = typeof(PlayableGameStore).GetField("api", BindingFlags.Instance | BindingFlags.NonPublic);
            return field?.GetValue(store) as GameApiClient ?? throw new InvalidOperationException("GameApiClient unavailable");
        }

        private static string ResolveBaseUrl(GameApiClient api)
        {
            FieldInfo field = typeof(GameApiClient).GetField("baseUrl", BindingFlags.Instance | BindingFlags.NonPublic);
            return field?.GetValue(api) as string ?? throw new InvalidOperationException("API base URL unavailable");
        }

        private async Task<T[]> GetArrayAsync<T>(string path)
        {
            using UnityWebRequest request = UnityWebRequest.Get(baseUrl + path);
            await Send(request);
            string wrapped = "{\"items\":" + request.downloadHandler.text + "}";
            return JsonUtility.FromJson<ArrayEnvelope<T>>(wrapped)?.items ?? Array.Empty<T>();
        }

        private async Task<T> PostAsync<T>(string path, string json)
        {
            using UnityWebRequest request = new UnityWebRequest(baseUrl + path, UnityWebRequest.kHttpVerbPOST);
            request.uploadHandler = new UploadHandlerRaw(Encoding.UTF8.GetBytes(json ?? "{}"));
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

        private static Button CloneAction(Button source, string name, int row, string label, UnityEngine.Events.UnityAction action)
        {
            if (source == null) return null;
            GameObject clone = Instantiate(source.gameObject, source.transform.parent);
            clone.name = name;
            Button button = clone.GetComponent<Button>();
            button.onClick.RemoveAllListeners();
            button.onClick.AddListener(action);
            RectTransform rect = clone.GetComponent<RectTransform>();
            RectTransform sourceRect = source.GetComponent<RectTransform>();
            if (rect != null && sourceRect != null) rect.anchoredPosition = sourceRect.anchoredPosition + new Vector2(0f, -68f * row);
            TMP_Text text = clone.GetComponentInChildren<TMP_Text>();
            if (text != null) text.text = label;
            return button;
        }

        private static MobileVerticalSliceController FindController(ScreenId id)
        {
            FieldInfo screenField = typeof(MobileVerticalSliceController).GetField("screenId", BindingFlags.Instance | BindingFlags.NonPublic);
            return FindObjectsOfType<MobileVerticalSliceController>(true)
                .FirstOrDefault(candidate => screenField?.GetValue(candidate) is ScreenId value && value == id);
        }

        private static T Field<T>(object target, string name) where T : class =>
            target?.GetType().GetField(name, BindingFlags.Instance | BindingFlags.NonPublic)?.GetValue(target) as T;
        private void SetStatus(string value) { if (statusText != null) statusText.text = value ?? string.Empty; }
        private static string Escape(string value) => UnityWebRequest.EscapeURL(value ?? string.Empty);

        [Serializable] private sealed class RequestDto { public string requestId; }
        [Serializable] private sealed class EvolutionRequestDto { public string requestId; public string targetVariant; }
        [Serializable] private sealed class FrameAdvanceResultDto { public string playerHeroId; public string frameTier; public int frameStep; public long goldCost; public string profileVersion; }
        [Serializable] private sealed class EvolutionResultDto { public string characterId; public string targetVariant; public long goldCost; public bool alreadyUnlocked; public string profileVersion; }
        [Serializable] private sealed class EvolutionPathDto { public string characterId; public string targetVariant; public string prerequisiteVariant; public int minLevel; public string minFrame; public long goldCost; public string status; public string profileVersion; }
        [Serializable] private sealed class ArrayEnvelope<T> { public T[] items; }
    }
}
