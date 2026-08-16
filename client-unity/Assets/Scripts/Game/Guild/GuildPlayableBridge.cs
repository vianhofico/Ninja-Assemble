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

namespace NinjaAssemble.Guild
{
    public sealed class GuildPlayableBridge : MonoBehaviour
    {
        private MobileVerticalSliceController controller;
        private TMP_Text bodyText;
        private TMP_Text statusText;
        private Button primaryAction;
        private TMP_Text primaryActionLabel;
        private string baseUrl;
        private string playerId;
        private GuildStateDto state;
        private bool binding;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        private static void Install()
        {
            var root = new GameObject("GuildPlayableBridgeRuntime");
            DontDestroyOnLoad(root);
            root.AddComponent<GuildPlayableBridge>();
        }

        private async void Update()
        {
            if (controller != null || binding || !MobileGameBootstrap.IsReady) return;
            binding = true;
            try
            {
                if (!TryResolveApi()) return;
                controller = FindGuildController();
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

        private static MobileVerticalSliceController FindGuildController()
        {
            FieldInfo screenField = typeof(MobileVerticalSliceController).GetField("screenId", BindingFlags.Instance | BindingFlags.NonPublic);
            foreach (MobileVerticalSliceController candidate in FindObjectsOfType<MobileVerticalSliceController>(true))
            {
                if (screenField?.GetValue(candidate) is ScreenId id && id == ScreenId.Guild) return candidate;
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
            if (state == null) { await RefreshAsync(); return; }
            try
            {
                if (state.guild == null)
                {
                    GuildDiscoverDto first = state.discover?.FirstOrDefault();
                    if (first != null)
                    {
                        SetStatus("Joining " + first.name + "...");
                        await PostAsync<GuildStateDto>($"/api/v1/play/{Escape(playerId)}/guild/{Escape(first.guildId)}/join", "{}");
                    }
                    else
                    {
                        string suffix = playerId.Substring(0, Math.Min(6, playerId.Length));
                        SetStatus("Creating guild...");
                        await PostAsync<GuildStateDto>($"/api/v1/play/{Escape(playerId)}/guild/create", JsonUtility.ToJson(new GuildNameRequestDto { name = "Ninja-" + suffix }));
                    }
                }
                else if (state.boss != null && !state.boss.defeated && !state.boss.playerHitToday)
                {
                    SetStatus("Attacking guild boss...");
                    GuildBossHitDto result = await PostAsync<GuildBossHitDto>($"/api/v1/play/{Escape(playerId)}/guild/boss/hit", JsonUtility.ToJson(new ActionRequestDto { requestId = Guid.NewGuid().ToString() }));
                    SetStatus($"Boss hit • {result.damage:N0} damage • HP {result.bossHpAfter:N0} • +{result.guildCoinReward} Guild Coin");
                }
                else
                {
                    SetStatus("Donating 1,000 Gold...");
                    GuildDonationDto donation = await PostAsync<GuildDonationDto>($"/api/v1/play/{Escape(playerId)}/guild/contribute", JsonUtility.ToJson(new GuildContributionRequestDto { goldAmount = 1000, requestId = Guid.NewGuid().ToString() }));
                    SetStatus($"Donation • +{donation.contributionPoints} contribution • Guild Coin {donation.guildCoinBalance:N0}");
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
            state = await GetAsync<GuildStateDto>($"/api/v1/play/{Escape(playerId)}/guild");
            Render();
        }

        private void Render()
        {
            if (state == null) return;
            if (state.guild == null)
            {
                GuildDiscoverDto[] discover = state.discover ?? Array.Empty<GuildDiscoverDto>();
                string rows = discover.Length == 0
                    ? "No guilds exist yet. Create the first one."
                    : string.Join("\n", discover.Select(g => $"• {g.name} • Lv.{g.level} • {g.members}/{g.memberCap}"));
                if (bodyText != null) bodyText.text = $"Guild\n{state.profileVersion}\n\nYou are not in a guild.\n\n{rows}";
                if (primaryActionLabel != null) primaryActionLabel.text = discover.Length == 0 ? "CREATE GUILD" : "JOIN GUILD";
                return;
            }

            string members = string.Join("\n", (state.members ?? Array.Empty<GuildMemberDto>()).Take(12)
                .Select(member => $"• {member.displayName} [{member.role}] • {member.contribution:N0}"));
            GuildBossDto boss = state.boss;
            string bossText = boss == null
                ? "No guild boss"
                : $"Boss {boss.currentHp:N0}/{boss.maxHp:N0} • {(boss.defeated ? "DEFEATED" : boss.playerHitToday ? "HIT USED" : "READY")}\n{boss.damageProfileVersion}";
            if (bodyText != null)
                bodyText.text = $"{state.guild.name} • Lv.{state.guild.level}\nRole: {state.role} • Members {state.guild.memberCount}\nGuild EXP {state.guild.exp:N0}\nToday contribution {state.todayContribution:N0}\n{bossText}\n\n{members}";
            if (primaryActionLabel != null)
                primaryActionLabel.text = boss != null && !boss.defeated && !boss.playerHitToday ? "HIT GUILD BOSS" : "DONATE 1000G";
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

        [Serializable] private sealed class GuildNameRequestDto { public string name; }
        [Serializable] private sealed class ActionRequestDto { public string requestId; }
        [Serializable] private sealed class GuildContributionRequestDto { public long goldAmount; public string requestId; }
        [Serializable] private sealed class GuildStateDto { public string profileVersion; public string role; public GuildDiscoverDto[] discover; public GuildInfoDto guild; public GuildMemberDto[] members; public long todayContribution; public GuildBossDto boss; }
        [Serializable] private sealed class GuildDiscoverDto { public string guildId; public string name; public int level; public int members; public int memberCap; }
        [Serializable] private sealed class GuildInfoDto { public string guildId; public string name; public int level; public long exp; public string notice; public int memberCount; }
        [Serializable] private sealed class GuildMemberDto { public string playerId; public string displayName; public string role; public long contribution; }
        [Serializable] private sealed class GuildBossDto { public string bossId; public string resetKey; public long maxHp; public long currentHp; public bool defeated; public bool playerHitToday; public string damageProfileVersion; }
        [Serializable] private sealed class GuildDonationDto { public bool replayed; public long goldSpent; public long contributionPoints; public long memberContribution; public long guildCoinBalance; }
        [Serializable] private sealed class GuildBossHitDto { public bool replayed; public string resetKey; public long damage; public long bossHpAfter; public bool defeated; public long guildCoinReward; public long guildCoinBalance; }
    }
}
