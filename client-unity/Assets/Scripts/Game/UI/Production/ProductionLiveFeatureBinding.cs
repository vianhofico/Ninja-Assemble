using System;
using System.Linq;
using NinjaAssemble.Bootstrap;
using NinjaAssemble.Network;
using NinjaAssemble.Progression;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

namespace NinjaAssemble.UI.Production
{
    public sealed class ProductionLiveFeatureBinding : MonoBehaviour
    {
        private ScreenId screenId;
        private TMP_Text body;
        private TMP_Text status;
        private Button action;
        private TMP_Text actionLabel;
        private bool busy;

        public void Configure(ScreenId id, TMP_Text bodyText, TMP_Text statusText, Button actionButton, TMP_Text label)
        {
            screenId = id; body = bodyText; status = statusText; action = actionButton; actionLabel = label;
            if (action != null) action.onClick.AddListener(OnAction);
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

        private void SetStatus(string value)
        {
            if (status == null) return;
            status.text = value ?? string.Empty;
            status.gameObject.SetActive(!string.IsNullOrWhiteSpace(status.text));
        }
    }
}
