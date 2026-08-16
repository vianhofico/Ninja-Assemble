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

namespace NinjaAssemble.Equipment
{
    public sealed class EquipmentPlayableBridge : MonoBehaviour
    {
        private MobileVerticalSliceController controller;
        private TMP_Text bodyText;
        private TMP_Text statusText;
        private Button primaryAction;
        private TMP_Text primaryActionLabel;
        private string baseUrl;
        private string playerId;
        private EquipmentViewDto state;
        private bool binding;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        private static void Install()
        {
            var root = new GameObject("EquipmentPlayableBridgeRuntime");
            DontDestroyOnLoad(root);
            root.AddComponent<EquipmentPlayableBridge>();
        }

        private async void Update()
        {
            if (controller != null || binding || !MobileGameBootstrap.IsReady) return;
            binding = true;
            try
            {
                if (!TryResolveApi()) return;
                controller = FindInventoryController();
                if (controller == null) return;
                ResolveUi(controller);
                if (primaryAction != null)
                {
                    primaryAction.onClick.RemoveAllListeners();
                    primaryAction.onClick.AddListener(OnPrimaryAction);
                }
                await RefreshAsync();
            }
            catch (Exception exception)
            {
                Debug.LogException(exception);
                SetStatus(exception.Message);
            }
            finally { binding = false; }
        }

        private bool TryResolveApi()
        {
            PlayableGameStore store = MobileGameBootstrap.Store;
            if (store == null || string.IsNullOrWhiteSpace(store.PlayerId)) return false;
            playerId = store.PlayerId;
            FieldInfo apiField = typeof(PlayableGameStore).GetField("api", BindingFlags.Instance | BindingFlags.NonPublic);
            object api = apiField?.GetValue(store);
            FieldInfo baseField = api?.GetType().GetField("baseUrl", BindingFlags.Instance | BindingFlags.NonPublic);
            baseUrl = baseField?.GetValue(api) as string;
            return !string.IsNullOrWhiteSpace(baseUrl);
        }

        private static MobileVerticalSliceController FindInventoryController()
        {
            FieldInfo screenField = typeof(MobileVerticalSliceController).GetField("screenId", BindingFlags.Instance | BindingFlags.NonPublic);
            foreach (MobileVerticalSliceController candidate in FindObjectsOfType<MobileVerticalSliceController>(true))
            {
                if (screenField?.GetValue(candidate) is ScreenId id && id == ScreenId.Inventory) return candidate;
            }
            return null;
        }

        private void ResolveUi(MobileVerticalSliceController target)
        {
            bodyText = Field<TMP_Text>(target, "bodyText");
            statusText = Field<TMP_Text>(target, "statusText");
            primaryAction = Field<Button>(target, "primaryAction");
            primaryActionLabel = Field<TMP_Text>(target, "primaryActionLabel");
        }

        private static T Field<T>(object target, string name) where T : class
        {
            return target?.GetType().GetField(name, BindingFlags.Instance | BindingFlags.NonPublic)?.GetValue(target) as T;
        }

        private async void OnPrimaryAction()
        {
            try
            {
                if (state == null) { await RefreshAsync(); return; }
                EquipmentItemDto[] items = state.equipment ?? Array.Empty<EquipmentItemDto>();
                EquipmentItemDto unequipped = items.FirstOrDefault(item => string.IsNullOrWhiteSpace(item.equippedPlayerHeroId));
                OwnedHeroDto hero = MobileGameBootstrap.Store.Formation?.heroes?.FirstOrDefault()
                                    ?? MobileGameBootstrap.Store.Heroes.FirstOrDefault();
                if (unequipped != null && hero != null)
                {
                    SetStatus($"Equipping {unequipped.definitionId} to {hero.displayName}...");
                    state = await PostAsync<EquipmentViewDto>($"/api/v1/play/{Escape(playerId)}/equipment/{Escape(unequipped.equipmentId)}/equip/{Escape(hero.id)}", "{}");
                    SetStatus($"Equipped {unequipped.definitionId} • {unequipped.slot}");
                }
                else
                {
                    EquipmentItemDto target = items.FirstOrDefault(item => !string.IsNullOrWhiteSpace(item.equippedPlayerHeroId) && item.enhanceLevel < item.maxEnhanceLevel);
                    if (target == null)
                    {
                        SetStatus("All equipped starter gear is at its current max level.");
                        return;
                    }
                    SetStatus($"Enhancing {target.definitionId}...");
                    EnhanceResultDto result = await PostAsync<EnhanceResultDto>(
                        $"/api/v1/play/{Escape(playerId)}/equipment/{Escape(target.equipmentId)}/enhance",
                        JsonUtility.ToJson(new ActionRequestDto { requestId = Guid.NewGuid().ToString() }));
                    SetStatus($"{target.definitionId} → +{result.equipment.enhanceLevel} • -{result.goldCost:N0} Gold • Gold {result.goldAfter:N0}");
                }
                await RefreshAsync();
            }
            catch (Exception exception)
            {
                Debug.LogException(exception);
                SetStatus(exception.Message);
            }
        }

        private async Task RefreshAsync()
        {
            state = await GetAsync<EquipmentViewDto>($"/api/v1/play/{Escape(playerId)}/equipment");
            Render();
        }

        private void Render()
        {
            if (state == null) return;
            InventoryItemDto[] stacks = MobileGameBootstrap.Store.Inventory?.items ?? Array.Empty<InventoryItemDto>();
            string stackRows = stacks.Length == 0
                ? "No stack items yet."
                : string.Join("\n", stacks.Take(12).Select(item => $"• {item.nameEn} x{item.quantity:N0} [{item.itemType}]"));
            EquipmentItemDto[] gear = state.equipment ?? Array.Empty<EquipmentItemDto>();
            string gearRows = gear.Length == 0
                ? "No gear."
                : string.Join("\n", gear.Select(item =>
                    $"• {item.definitionId} [{item.slot}/{item.rarity}] +{item.enhanceLevel}/{item.maxEnhanceLevel} {(string.IsNullOrWhiteSpace(item.equippedPlayerHeroId) ? "UNEQUIPPED" : "EQUIPPED")}"));
            if (bodyText != null)
                bodyText.text = $"Inventory + Equipment\n{state.catalogVersion}\n{state.combatBonusVersion}\n\nSTACK ITEMS\n{stackRows}\n\nGEAR\n{gearRows}";

            EquipmentItemDto nextUnequipped = gear.FirstOrDefault(item => string.IsNullOrWhiteSpace(item.equippedPlayerHeroId));
            if (primaryActionLabel != null)
                primaryActionLabel.text = nextUnequipped != null ? "EQUIP NEXT" : "ENHANCE GEAR";
        }

        private void SetStatus(string value) { if (statusText != null) statusText.text = value ?? string.Empty; }

        private async Task<T> GetAsync<T>(string path)
        {
            using UnityWebRequest request = UnityWebRequest.Get(baseUrl + path);
            await Send(request);
            return JsonUtility.FromJson<T>(request.downloadHandler.text);
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

        private static string Escape(string value) => UnityWebRequest.EscapeURL(value ?? string.Empty);

        [Serializable] private sealed class ActionRequestDto { public string requestId; }
        [Serializable] private sealed class EquipmentViewDto { public string catalogVersion; public string enhanceProfileVersion; public string combatBonusVersion; public EquipmentItemDto[] equipment; }
        [Serializable] private sealed class EquipmentItemDto { public string equipmentId; public string definitionId; public string nameKey; public string slot; public string rarity; public int enhanceLevel; public int maxEnhanceLevel; public int refineLevel; public string equippedPlayerHeroId; public string equippedSlot; }
        [Serializable] private sealed class EnhanceResultDto { public EquipmentItemDto equipment; public long goldCost; public long goldAfter; public bool replayed; public string enhanceProfileVersion; }
    }
}
