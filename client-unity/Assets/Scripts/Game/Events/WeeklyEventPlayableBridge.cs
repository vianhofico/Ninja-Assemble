using System;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using NinjaAssemble.Bootstrap;
using NinjaAssemble.Playable;
using NinjaAssemble.UI;
using TMPro;
using UnityEngine;
using UnityEngine.Networking;
using UnityEngine.UI;

namespace NinjaAssemble.Events
{
    public sealed class WeeklyEventPlayableBridge : MonoBehaviour
    {
        private MobileVerticalSliceController controller;
        private TMP_Text bodyText;
        private TMP_Text statusText;
        private Button primaryAction;
        private TMP_Text primaryActionLabel;
        private string baseUrl;
        private string playerId;
        private EventBoardDto board;
        private bool binding;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        private static void Install()
        {
            var root = new GameObject("WeeklyEventPlayableBridgeRuntime");
            DontDestroyOnLoad(root);
            root.AddComponent<WeeklyEventPlayableBridge>();
        }

        private async void Update()
        {
            if (controller != null || binding || !MobileGameBootstrap.IsReady) return;
            binding = true;
            try
            {
                if (!TryResolveApi()) return;
                controller = FindController(ScreenId.Events);
                if (controller == null) return;
                bodyText = Field<TMP_Text>(controller, "bodyText");
                statusText = Field<TMP_Text>(controller, "statusText");
                primaryAction = Field<Button>(controller, "primaryAction");
                primaryActionLabel = Field<TMP_Text>(controller, "primaryActionLabel");
                primaryAction.onClick.RemoveAllListeners();
                primaryAction.onClick.AddListener(OnPrimaryAction);
                await RefreshAsync();
            }
            catch (Exception exception) { Debug.LogException(exception); SetStatus(exception.Message); }
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

        private async void OnPrimaryAction()
        {
            try
            {
                EventObjectiveDto objective = board?.objectives?.FirstOrDefault(item => item.claimable);
                if (objective == null)
                {
                    SetStatus("No completed weekly objective is waiting to be claimed.");
                    await RefreshAsync();
                    return;
                }
                SetStatus("Claiming " + objective.objectiveId + "...");
                EventClaimDto result = await PostAsync<EventClaimDto>(
                    $"/api/v1/play/{Escape(playerId)}/events/{Escape(objective.objectiveId)}/claim", "{}");
                SetStatus(result.replayed
                    ? "Weekly objective was already claimed."
                    : $"Claimed • +{result.gold:N0} Gold • +{result.diamond:N0} Diamond" +
                      (result.itemQuantity > 0 ? $" • +{result.itemQuantity} {result.itemId}" : string.Empty));
                await RefreshAsync();
            }
            catch (Exception exception) { Debug.LogException(exception); SetStatus(exception.Message); }
        }

        private async Task RefreshAsync()
        {
            board = await GetAsync<EventBoardDto>($"/api/v1/play/{Escape(playerId)}/events");
            Render();
        }

        private void Render()
        {
            if (board == null) return;
            bool vi = PlayerPrefs.GetString("na.language", "VI") == "VI";
            string name = vi ? board.nameVi : board.nameEn;
            string rows = string.Join("\n", (board.objectives ?? Array.Empty<EventObjectiveDto>()).Select(item =>
                $"{(item.claimed ? "✓" : item.claimable ? "CLAIM" : "•")} {ObjectiveName(item.metric, vi)} {item.currentValue}/{item.target} • +{item.rewardGold}G +{item.rewardDiamond}D" +
                (item.rewardItemQuantity > 0 ? $" +{item.rewardItemQuantity} {item.rewardItemId}" : string.Empty)));
            if (bodyText != null)
                bodyText.text = $"{name}\n{board.profileVersion}\n{board.startsAt} → {board.endsAt}\n\n{rows}";
            if (primaryActionLabel != null)
                primaryActionLabel.text = (board.objectives ?? Array.Empty<EventObjectiveDto>()).Any(item => item.claimable) ? (vi ? "NHẬN THƯỞNG" : "CLAIM") : (vi ? "LÀM MỚI" : "REFRESH");
        }

        private static string ObjectiveName(string metric, bool vi)
        {
            if (metric == "CLEAR_STAGE") return vi ? "Vượt ải" : "Clear stages";
            if (metric == "WIN_ARENA") return vi ? "Thắng Arena" : "Win Arena";
            if (metric == "SUMMON") return vi ? "Triệu hồi" : "Summon";
            return metric ?? string.Empty;
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

        [Serializable] private sealed class EventBoardDto { public string profileVersion; public string eventId; public string nameEn; public string nameVi; public string startsAt; public string endsAt; public EventObjectiveDto[] objectives; }
        [Serializable] private sealed class EventObjectiveDto { public int index; public string objectiveId; public string metric; public long target; public long currentValue; public bool claimed; public bool claimable; public long rewardGold; public long rewardDiamond; public string rewardItemId; public long rewardItemQuantity; }
        [Serializable] private sealed class EventClaimDto { public string eventId; public string objectiveId; public bool replayed; public long gold; public long diamond; public string itemId; public long itemQuantity; public long finalValue; }
    }
}
