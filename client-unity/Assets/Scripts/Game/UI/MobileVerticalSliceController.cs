using System;
using System.Linq;
using NinjaAssemble.Bootstrap;
using NinjaAssemble.Network;
using NinjaAssemble.Playable;
using NinjaAssemble.Presentation;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

namespace NinjaAssemble.UI
{
    public sealed class MobileVerticalSliceController : MonoBehaviour
    {
        [SerializeField] private ScreenId screenId;
        [SerializeField] private TMP_Text resourceText;
        [SerializeField] private TMP_Text bodyText;
        [SerializeField] private TMP_Text statusText;
        [SerializeField] private Button primaryAction;
        [SerializeField] private TMP_Text primaryActionLabel;
        private BattleVisualStage battleVisualStage;

        public void Configure(ScreenId id, TMP_Text resources, TMP_Text body, TMP_Text status, Button action, TMP_Text actionLabel)
        {
            screenId = id;
            resourceText = resources;
            bodyText = body;
            statusText = status;
            primaryAction = action;
            primaryActionLabel = actionLabel;
        }

        private void Start()
        {
            if (primaryAction != null) primaryAction.onClick.AddListener(OnPrimaryAction);
            Render();
        }

        private void OnDestroy()
        {
            if (primaryAction != null) primaryAction.onClick.RemoveListener(OnPrimaryAction);
        }

        private async void OnPrimaryAction()
        {
            if (!MobileGameBootstrap.IsReady)
            {
                SetStatus("Connecting...");
                return;
            }

            try
            {
                PlayableGameStore store = MobileGameBootstrap.Store;
                switch (screenId)
                {
                    case ScreenId.Battle:
                    {
                        SetStatus("Battle in progress...");
                        PlayBattleDto result = await store.BattleAsync();
                        await PresentBattle(result);
                        SetStatus(BattleStatus(result));
                        break;
                    }
                    case ScreenId.Adventure:
                    {
                        CampaignStageDto stage = store.RecommendedStage;
                        if (stage == null)
                        {
                            SetStatus("No campaign stage is currently unlocked. Check level or energy requirements.");
                            break;
                        }
                        SetStatus($"{stage.stageId} • {stage.nameEn} • battle in progress...");
                        PlayBattleDto result = await store.BattleCampaignAsync(stage.stageId);
                        await PresentBattle(result);
                        SetStatus(BattleStatus(result));
                        break;
                    }
                    case ScreenId.Summon:
                    {
                        SetStatus("Summoning...");
                        var result = await store.SummonAsync();
                        SetStatus($"{result.characterId} • {result.variant} • {result.rarity}" + (result.pityTriggered ? " • PITY" : string.Empty));
                        break;
                    }
                    case ScreenId.HeroDetail:
                    {
                        var hero = store.Heroes.FirstOrDefault();
                        if (hero == null) { SetStatus("No owned ninja"); break; }
                        SetStatus("Training...");
                        var result = await store.LevelUpAsync(hero.id);
                        SetStatus($"{result.hero.displayName} → Lv.{result.hero.level} • -{result.goldCost} Gold");
                        break;
                    }
                    default:
                        SetStatus("This screen is connected to the shared game state; feature-specific interaction is added by its dedicated controller.");
                        break;
                }
                Render();
            }
            catch (Exception exception)
            {
                Debug.LogException(exception);
                SetStatus(exception.Message);
            }
        }

        private async System.Threading.Tasks.Task PresentBattle(PlayBattleDto result)
        {
            if (battleVisualStage == null) battleVisualStage = gameObject.AddComponent<BattleVisualStage>();
            await battleVisualStage.PresentAsync(result);
        }

        private static string BattleStatus(PlayBattleDto result)
        {
            string clear = result.firstClear ? " • FIRST CLEAR" : string.Empty;
            return $"{result.stageId} • {result.battle.outcome} • {result.stars}★ • {result.battle.rounds} rounds" +
                   $" • +{result.goldReward} Gold • +{result.diamondReward} Diamond • +{result.playerExpReward} EXP{clear}";
        }

        private void Render()
        {
            if (!MobileGameBootstrap.IsReady)
            {
                if (resourceText != null) resourceText.text = "Connecting to local ninja server...";
                if (bodyText != null) bodyText.text = "Loading player state...";
                ConfigureAction();
                return;
            }

            PlayableGameStore store = MobileGameBootstrap.Store;
            if (resourceText != null)
                resourceText.text = $"Gold  {store.Gold:N0}     ◆  {store.Diamond:N0}     Energy  {store.Energy}";

            if (bodyText != null) bodyText.text = BuildBody(store);
            ConfigureAction();
        }

        private string BuildBody(PlayableGameStore store)
        {
            return screenId switch
            {
                ScreenId.Home => $"Hidden Village\n\nOwned ninja: {store.Heroes.Length}\nFormation: {(store.Formation?.heroes?.Length ?? 0)}/5\nCampaign: {store.Campaign?.stages?.Count(s => s.clearCount > 0) ?? 0}/{store.Campaign?.stages?.Length ?? 0} cleared\n\nChoose a destination from the navigation below.",
                ScreenId.NinjaRoster => "Ninja Roster\n\n" + string.Join("\n", store.Heroes.Take(18).Select(h => $"• {h.displayName}  Lv.{h.level}  [{h.currentVariant ?? "BASE"}]")),
                ScreenId.HeroDetail => store.Heroes.FirstOrDefault() is { } h ? $"{h.displayName}\nLv.{h.level} • {h.frameTier}\nVariant: {h.currentVariant ?? "BASE"}\nAwakening: {h.awakeningLevel}\n\nUse TRAIN to exercise the live progression endpoint." : "No owned ninja yet.",
                ScreenId.Formation => "Formation 5\n\n" + string.Join("\n", (store.Formation?.heroes ?? Array.Empty<OwnedHeroDto>()).Select((h, i) => $"{i + 1}. {h.displayName}  Lv.{h.level}")),
                ScreenId.Adventure => BuildAdventure(store),
                ScreenId.Battle => "Battle Debug\n\n5 vs 5 deterministic simulation\nDefault stage: c1-s1\nServer seed • structured skills/passives • authoritative rewards\n\nUse Adventure for campaign progression.",
                ScreenId.Summon => $"Complete Roster+ Summon\n\nCost: {PlayableGameStore.CompleteRosterSummonCost} Diamond\nHard pity and duplicate Hero Coin conversion are server-authoritative.",
                ScreenId.Arena => "Arena\n\n5-ninja asynchronous defense/offense foundation is available on the server domain.",
                ScreenId.ShadowArena => "Shadow Arena\n\n15 ninja • 3 squads • best-of-three foundation.",
                ScreenId.Guild => "Guild\n\nMembership • contribution • mission • boss foundations.",
                ScreenId.Shop => "Shops\n\nGold / Diamond / Arena / Hero / Guild / Shadow economy foundations.",
                ScreenId.Inventory => "Inventory & Equipment\n\nItems, equipment instances and loadouts are modeled server-side.",
                ScreenId.Quest => "Quests\n\nDaily objectives and reset cadence foundations.",
                ScreenId.Events => "Events\n\nVersioned event/objective definitions with server clock boundaries.",
                ScreenId.Mail => "Mail\n\nPlayer mail and attachment domain foundations.",
                ScreenId.Settings => "Settings\n\nLanguage: English / Tiếng Việt\nAudio, graphics and account controls attach here.",
                _ => screenId.ToString()
            };
        }

        private static string BuildAdventure(PlayableGameStore store)
        {
            CampaignStageListDto campaign = store.Campaign;
            if (campaign?.stages == null || campaign.stages.Length == 0) return "Adventure\n\nNo campaign catalog loaded.";
            string lines = string.Join("\n", campaign.stages.Select(stage =>
            {
                string gate = stage.unlocked ? (stage.clearCount > 0 ? new string('★', Mathf.Clamp(stage.bestStars, 0, 3)) : "READY") : "LOCKED";
                return $"{stage.stageId}  C{stage.chapter}-{stage.stageIndex}  {stage.difficulty}  {gate}  E{stage.energyCost}  {stage.nameEn}";
            }));
            CampaignStageDto next = store.RecommendedStage;
            string nextText = next == null ? "No stage currently playable" : $"Next: {next.stageId} • {next.nameEn} • {next.energyCost} Energy";
            return $"Adventure • Lv.{campaign.playerLevel}\n{campaign.catalogVersion}\n\n{lines}\n\n{nextText}";
        }

        private void ConfigureAction()
        {
            if (primaryAction == null || primaryActionLabel == null) return;
            string label = screenId switch
            {
                ScreenId.Battle => "FIGHT c1-s1",
                ScreenId.Adventure => MobileGameBootstrap.IsReady && MobileGameBootstrap.Store.RecommendedStage != null
                    ? "FIGHT " + MobileGameBootstrap.Store.RecommendedStage.stageId
                    : "REFRESH",
                ScreenId.Summon => "SUMMON",
                ScreenId.HeroDetail => "TRAIN",
                _ => "REFRESH"
            };
            primaryActionLabel.text = label;
        }

        private void SetStatus(string value)
        {
            if (statusText != null) statusText.text = value ?? string.Empty;
        }
    }
}
