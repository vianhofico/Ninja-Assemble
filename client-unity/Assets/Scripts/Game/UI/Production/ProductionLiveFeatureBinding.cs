using System;
using System.Linq;
using NinjaAssemble.Bootstrap;
using NinjaAssemble.Network;
using NinjaAssemble.Progression;
using NinjaAssemble.Settings;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

namespace NinjaAssemble.UI.Production
{
    public sealed class ProductionLiveFeatureBinding : MonoBehaviour
    {
        private const string LanguageKey = "na.language";
        private const string SoundKey = "na.sound";
        private const string QualityKey = "na.quality";
        private ScreenId screenId;
        private TMP_Text body;
        private TMP_Text status;
        private Button action;
        private TMP_Text actionLabel;
        private Button soundAction;
        private Button graphicsAction;
        private Button textScaleAction;
        private Button motionAction;
        private Button hapticsAction;
        private bool busy;

        public void Configure(ScreenId id, TMP_Text bodyText, TMP_Text statusText, Button actionButton, TMP_Text label)
        {
            screenId = id; body = bodyText; status = statusText; action = actionButton; actionLabel = label;
            if (action != null) action.onClick.AddListener(OnAction);
            if (screenId == ScreenId.Settings) BuildSettingsActions();
            Render();
        }

        private void OnDestroy(){if(action!=null)action.onClick.RemoveListener(OnAction);}
        private void Update()=>Render();

        private async void OnAction()
        {
            if (busy || !MobileGameBootstrap.IsReady) return;
            busy = true;
            if (action != null) action.interactable = false;
            try
            {
                if (screenId == ScreenId.ResourcePve)
                {
                    var store = MobileGameBootstrap.Store;
                    ResourcePveModeDto mode = store.RecommendedResourcePve;
                    if (mode == null) { await store.RefreshResourcePveAsync(); mode = store.RecommendedResourcePve; }
                    if (mode == null) { SetStatus("No Resource PvE mode is currently playable."); return; }
                    SetStatus($"{mode.nameEn} • battle in progress...");
                    ResourcePveBattleDto result = await store.BattleResourcePveAsync(mode.modeId);
                    SetStatus($"{mode.nameEn} • {(result.won?"WIN":"LOSS")} • Energy {result.energyAfter} • +{result.goldReward} Gold" + (result.itemQuantity>0?$" • +{result.itemQuantity} {result.itemId}":string.Empty));
                }
                else if (screenId == ScreenId.Progression)
                {
                    AdvancedProgressionStore store = MobileGameBootstrap.AdvancedProgression;
                    if (store == null) { SetStatus("Advanced progression is still initializing."); return; }
                    AdvancedProgressionTrackDto track = store.RecommendedTrack;
                    if (track == null) { await store.RefreshAsync(); track = store.RecommendedTrack; }
                    if (track == null) { SetStatus("No affordable progression upgrade is currently available."); return; }
                    SetStatus($"Upgrading {track.nameEn}...");
                    AdvancedProgressionUpgradeDto result = await store.UpgradeAsync(track.trackId);
                    SetStatus($"{track.nameEn} • Lv.{result.levelBefore} → Lv.{result.levelAfter} • -{result.goldCost} Gold" + (result.itemCost>0?$" • -{result.itemCost} {result.itemId}":string.Empty));
                }
                else if (screenId == ScreenId.Settings)
                {
                    string next = PlayerPrefs.GetString(LanguageKey, "VI") == "VI" ? "EN" : "VI";
                    PlayerPrefs.SetString(LanguageKey, next); PlayerPrefs.Save();
                    SetStatus(next == "VI" ? "Đã chuyển sang Tiếng Việt." : "Language changed to English.");
                }
            }
            catch (Exception exception)
            {
                Debug.LogException(exception);
                SetStatus(exception.Message);
            }
            finally
            {
                busy = false;
                if (action != null) action.interactable = true;
                Render();
            }
        }

        private void BuildSettingsActions()
        {
            if (action == null) return;
            soundAction = CloneAction(action, "ProductionSoundAction", ToggleSound);
            graphicsAction = CloneAction(action, "ProductionGraphicsAction", CycleGraphics);
            textScaleAction = CloneAction(action, "ProductionTextScaleAction", () => { AccessibilityPreferences.CycleTextScale(); SetStatus("Text size updated."); });
            motionAction = CloneAction(action, "ProductionReduceMotionAction", () => { AccessibilityPreferences.ToggleReduceMotion(); SetStatus("Motion preference updated."); });
            hapticsAction = CloneAction(action, "ProductionHapticsAction", () => { AccessibilityPreferences.ToggleHaptics(); SetStatus("Haptics preference updated."); });
        }

        private void ToggleSound()
        {
            int enabled = PlayerPrefs.GetInt(SoundKey, 1) == 1 ? 0 : 1;
            PlayerPrefs.SetInt(SoundKey, enabled); PlayerPrefs.Save(); AudioListener.volume = enabled == 1 ? 1f : 0f;
            SetStatus(enabled == 1 ? "Sound enabled." : "Sound muted.");
        }

        private void CycleGraphics()
        {
            int count = Mathf.Max(1, QualitySettings.names.Length);
            int current = Mathf.Clamp(PlayerPrefs.GetInt(QualityKey, QualitySettings.GetQualityLevel()), 0, count - 1);
            int next = (current + 1) % count;
            PlayerPrefs.SetInt(QualityKey, next); PlayerPrefs.Save();
            if (QualitySettings.names.Length > 0) QualitySettings.SetQualityLevel(next, true);
            SetStatus("Graphics quality updated.");
        }

        private void Render()
        {
            if (!MobileGameBootstrap.IsReady)
            {
                if (body != null) body.text = "Connecting to player state...";
                if (action != null) action.interactable = false;
                return;
            }
            if (action != null && !busy) action.interactable = true;
            if (screenId == ScreenId.ResourcePve) RenderResourcePve();
            else if (screenId == ScreenId.Progression) RenderProgression();
            else if (screenId == ScreenId.Settings) RenderSettings();
        }

        private void RenderResourcePve()
        {
            ResourcePveBoardDto board = MobileGameBootstrap.Store.ResourcePve;
            ResourcePveModeDto[] modes = board?.modes ?? Array.Empty<ResourcePveModeDto>();
            string rows = modes.Length == 0 ? "No Resource PvE catalog loaded." : string.Join("\n", modes.Select(mode =>
                $"• {mode.nameEn} • {(mode.playable?"READY":mode.blockedReason)} • attempts {mode.attemptsRemaining}/{mode.dailyAttemptLimit} • E{mode.energyCost}"));
            if (body != null) body.text = $"Resource PvE\n{board?.catalogVersion ?? "loading"}\n\n{rows}";
            if (actionLabel != null) actionLabel.text = "BATTLE NEXT MODE";
        }

        private void RenderProgression()
        {
            AdvancedProgressionStore store = MobileGameBootstrap.AdvancedProgression;
            AdvancedProgressionBoardDto board = store?.Board;
            AdvancedProgressionTrackDto[] tracks = board?.tracks ?? Array.Empty<AdvancedProgressionTrackDto>();
            string rows = tracks.Length == 0 ? "No advanced progression catalog loaded." : string.Join("\n", tracks.Select(track =>
                $"• {track.nameEn} • Lv.{track.level}/{track.maxLevel} • {(track.maxed?"MAX":track.affordable?"READY":track.blockedReason)} • {track.bonusStat} +{track.cumulativeBonus}"));
            if (body != null) body.text = $"Advanced Progression\n{board?.catalogVersion ?? "loading"}\nGold {board?.gold ?? 0:N0}\n\n{rows}";
            if (actionLabel != null) actionLabel.text = "UPGRADE NEXT";
        }

        private void RenderSettings()
        {
            bool vi = PlayerPrefs.GetString(LanguageKey, "VI") == "VI";
            bool sound = PlayerPrefs.GetInt(SoundKey, 1) == 1;
            int quality = Mathf.Clamp(PlayerPrefs.GetInt(QualityKey, QualitySettings.GetQualityLevel()), 0, Mathf.Max(0, QualitySettings.names.Length - 1));
            string qualityName = QualitySettings.names.Length == 0 ? "Default" : QualitySettings.names[quality];
            if (body != null) body.text = vi
                ? $"Cài đặt\n\nNgôn ngữ: Tiếng Việt\nÂm thanh: {(sound?"Bật":"Tắt")}\nĐồ họa: {qualityName}\nCỡ chữ: {AccessibilityPreferences.TextScale:0.00}x\nGiảm chuyển động: {(AccessibilityPreferences.ReduceMotion?"Bật":"Tắt")}\nRung: {(AccessibilityPreferences.HapticsEnabled?"Bật":"Tắt")}" 
                : $"Settings\n\nLanguage: English\nSound: {(sound?"On":"Off")}\nGraphics: {qualityName}\nText size: {AccessibilityPreferences.TextScale:0.00}x\nReduce motion: {(AccessibilityPreferences.ReduceMotion?"On":"Off")}\nHaptics: {(AccessibilityPreferences.HapticsEnabled?"On":"Off")}";
            if (actionLabel != null) actionLabel.text = vi ? "ENGLISH" : "TIẾNG VIỆT";
            SetButtonLabel(soundAction, sound ? (vi?"TẮT ÂM":"MUTE") : (vi?"BẬT ÂM":"SOUND ON"));
            SetButtonLabel(graphicsAction, vi?"ĐỔI ĐỒ HỌA":"NEXT GRAPHICS");
            SetButtonLabel(textScaleAction, vi?"CỠ CHỮ":"TEXT SIZE");
            SetButtonLabel(motionAction, vi?"GIẢM CHUYỂN ĐỘNG":"REDUCE MOTION");
            SetButtonLabel(hapticsAction, vi?"RUNG":"HAPTICS");
        }

        private static Button CloneAction(Button source, string name, UnityEngine.Events.UnityAction listener)
        {
            GameObject clone = UnityEngine.Object.Instantiate(source.gameObject, source.transform.parent);
            clone.name = name; Button button = clone.GetComponent<Button>(); button.onClick.RemoveAllListeners(); button.onClick.AddListener(listener); return button;
        }
        private static void SetButtonLabel(Button button,string value){TMP_Text text=button?.transform.Find("Label")?.GetComponent<TMP_Text>();if(text!=null)text.text=value;}
        private void SetStatus(string value)
        {
            if (status == null) return;
            status.text = value ?? string.Empty;
            status.gameObject.SetActive(!string.IsNullOrWhiteSpace(status.text));
        }
    }
}
