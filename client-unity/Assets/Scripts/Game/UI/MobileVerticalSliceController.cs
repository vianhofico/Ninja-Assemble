using System;
using System.Linq;
using System.Threading.Tasks;
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
            screenId = id; resourceText = resources; bodyText = body; statusText = status; primaryAction = action; primaryActionLabel = actionLabel;
        }

        private void Start() { if (primaryAction != null) primaryAction.onClick.AddListener(OnPrimaryAction); Render(); }
        private void OnDestroy() { if (primaryAction != null) primaryAction.onClick.RemoveListener(OnPrimaryAction); }

        private async void OnPrimaryAction()
        {
            if (!MobileGameBootstrap.IsReady) { SetStatus("Connecting..."); return; }
            try
            {
                PlayableGameStore store = MobileGameBootstrap.Store;
                switch (screenId)
                {
                    case ScreenId.Battle:
                    {
                        SetStatus("Battle in progress...");
                        PlayBattleDto result = await store.BattleAsync();
                        await PresentBattle(result); SetStatus(BattleStatus(result)); break;
                    }
                    case ScreenId.Adventure:
                    {
                        CampaignStageDto stage = store.RecommendedStage;
                        if (stage == null) { SetStatus("No campaign stage is currently unlocked. Check level or energy requirements."); break; }
                        SetStatus($"{stage.stageId} • {stage.nameEn} • {stage.waveCount} wave(s) • battle in progress...");
                        PlayBattleDto result = await store.BattleCampaignAsync(stage.stageId);
                        await PresentBattle(result); SetStatus(BattleStatus(result)); break;
                    }
                    case ScreenId.Arena:
                    {
                        ArenaOpponentDto opponent = store.RecommendedArenaOpponent;
                        if (opponent == null) { await store.RefreshArenaAsync(); opponent = store.RecommendedArenaOpponent; }
                        if (opponent == null) { SetStatus("No Arena opponent is available yet. Save a five-ninja formation first."); break; }
                        SetStatus((opponent.training ? "Training mirror" : opponent.displayName) + " • Arena battle in progress...");
                        ArenaBattleDto result = await store.FightArenaAsync(opponent.playerId);
                        await PresentArenaBattle(result); SetStatus(ArenaBattleStatus(result)); break;
                    }
                    case ScreenId.Shop:
                    {
                        ShopEntryDto shop; ShopOfferDto offer;
                        if (!TryFirstPurchasable(store, out shop, out offer))
                        {
                            await store.RefreshShopAsync();
                            if (!TryFirstPurchasable(store, out shop, out offer)) { SetStatus("No shop offer is currently purchasable."); break; }
                        }
                        SetStatus($"Buying {offer.itemNameEn} from {shop.nameEn}...");
                        ShopPurchaseResultDto result = await store.PurchaseShopAsync(shop.shopId, offer.offerId);
                        SetStatus($"Purchased {offer.quantity}× {offer.itemNameEn} • {result.currency} -{result.charged:N0} • owned {result.itemBalanceAfter:N0}");
                        break;
                    }
                    case ScreenId.Summon:
                    {
                        SetStatus("Summoning..."); var result = await store.SummonAsync();
                        SetStatus($"{result.characterId} • {result.variant} • {result.rarity}" + (result.pityTriggered ? " • PITY" : string.Empty)); break;
                    }
                    case ScreenId.HeroDetail:
                    {
                        var hero = store.Heroes.FirstOrDefault(); if (hero == null) { SetStatus("No owned ninja"); break; }
                        SetStatus("Training..."); var result = await store.LevelUpAsync(hero.id);
                        SetStatus($"{result.hero.displayName} → Lv.{result.hero.level} • -{result.goldCost} Gold"); break;
                    }
                    case ScreenId.Inventory:
                        SetStatus("Refreshing inventory..."); await store.RefreshInventoryAsync(); SetStatus($"Inventory synced • {store.Inventory?.items?.Length ?? 0} stack(s)"); break;
                    default:
                        SetStatus("This screen is connected to the shared game state; feature-specific interaction is added by its dedicated controller."); break;
                }
                Render();
            }
            catch (Exception exception) { Debug.LogException(exception); SetStatus(exception.Message); }
        }

        private static bool TryFirstPurchasable(PlayableGameStore store, out ShopEntryDto selectedShop, out ShopOfferDto selectedOffer)
        {
            foreach (ShopEntryDto shop in store.Shop?.shops ?? Array.Empty<ShopEntryDto>())
            {
                ShopOfferDto offer = (shop.offers ?? Array.Empty<ShopOfferDto>()).FirstOrDefault(item => item.purchasable);
                if (offer != null) { selectedShop = shop; selectedOffer = offer; return true; }
            }
            selectedShop = null; selectedOffer = null; return false;
        }

        private async Task PresentBattle(PlayBattleDto result)
        {
            if (result == null) throw new ArgumentNullException(nameof(result));
            if (battleVisualStage == null) battleVisualStage = gameObject.AddComponent<BattleVisualStage>();
            CampaignWaveDto[] waves = result.waves ?? Array.Empty<CampaignWaveDto>();
            if (waves.Length == 0) { await battleVisualStage.PresentAsync(result); await WaitForReplayAsync(); return; }
            foreach (CampaignWaveDto wave in waves.OrderBy(wave => wave.waveIndex))
            {
                SetStatus($"{result.stageId} • Wave {wave.waveIndex}/{waves.Length} • replay running...");
                var waveResult = new PlayBattleDto { battleId = result.battleId, stageId = result.stageId, campaignCatalogVersion = result.campaignCatalogVersion,
                    waveRulesVersion = result.waveRulesVersion, combatStatsVersion = result.combatStatsVersion, abilityProfileVersion = result.abilityProfileVersion,
                    techniqueMappingVersion = result.techniqueMappingVersion, passiveProfileVersion = result.passiveProfileVersion, participants = wave.participants, battle = wave.battle };
                await battleVisualStage.PresentAsync(waveResult); await WaitForReplayAsync();
            }
        }

        private async Task PresentArenaBattle(ArenaBattleDto result)
        {
            await PresentBattle(new PlayBattleDto { battleId = result.battleId, stageId = "arena", combatStatsVersion = result.combatStatsVersion,
                abilityProfileVersion = result.abilityProfileVersion, techniqueMappingVersion = result.techniqueMappingVersion,
                passiveProfileVersion = result.passiveProfileVersion, participants = result.participants, battle = result.battle });
        }

        private async Task WaitForReplayAsync()
        {
            while (GetComponentsInChildren<BattleTimelinePlayer>(true).Any(player => player != null && player.IsPlaying)) await Task.Yield();
        }

        private static string BattleStatus(PlayBattleDto result)
        {
            string clear = result.firstClear ? " • FIRST CLEAR" : string.Empty;
            CampaignWaveDto[] waves = result.waves ?? Array.Empty<CampaignWaveDto>();
            int waveCount = waves.Length == 0 ? 1 : waves.Length;
            int rounds = waves.Length == 0 ? result.battle?.rounds ?? 0 : waves.Sum(wave => wave.battle?.rounds ?? 0);
            string itemText = result.itemRewards == null || result.itemRewards.Length == 0 ? string.Empty : " • " + string.Join(", ", result.itemRewards.Select(item => $"+{item.quantity} {item.itemId}"));
            return $"{result.stageId} • {result.battle?.outcome} • {result.stars}★ • {waveCount} wave(s) • {rounds} rounds • +{result.goldReward} Gold • +{result.diamondReward} Diamond • +{result.playerExpReward} EXP{itemText}{clear}";
        }

        private static string ArenaBattleStatus(ArenaBattleDto result)
        {
            if (result.training) return $"TRAINING • {result.battle?.outcome} • {result.battle?.rounds ?? 0} rounds • rating/reward unchanged";
            string delta = result.ratingDelta >= 0 ? "+" + result.ratingDelta : result.ratingDelta.ToString();
            return $"ARENA • {result.battle?.outcome} • Rating {result.ratingBefore:N0} → {result.ratingAfter:N0} ({delta}) • +{result.arenaCoinReward} Arena Coin";
        }

        private void Render()
        {
            if (!MobileGameBootstrap.IsReady) { if (resourceText != null) resourceText.text = "Connecting to local ninja server..."; if (bodyText != null) bodyText.text = "Loading player state..."; ConfigureAction(); return; }
            PlayableGameStore store = MobileGameBootstrap.Store;
            if (resourceText != null) resourceText.text = $"Gold  {store.Gold:N0}     ◆  {store.Diamond:N0}     Energy  {store.Energy}";
            if (bodyText != null) bodyText.text = BuildBody(store); ConfigureAction();
        }

        private string BuildBody(PlayableGameStore store)
        {
            return screenId switch
            {
                ScreenId.Home => $"Hidden Village\n\nOwned ninja: {store.Heroes.Length}\nFormation: {(store.Formation?.heroes?.Length ?? 0)}/5\nCampaign: {store.Campaign?.stages?.Count(s => s.clearCount > 0) ?? 0}/{store.Campaign?.stages?.Length ?? 0} cleared\nInventory stacks: {store.Inventory?.items?.Length ?? 0}\nArena rating: {store.Arena?.rating ?? 0:N0}\n\nChoose a destination from the navigation below.",
                ScreenId.NinjaRoster => "Ninja Roster\n\n" + string.Join("\n", store.Heroes.Take(18).Select(h => $"• {h.displayName}  Lv.{h.level}  [{h.currentVariant ?? "BASE"}]")),
                ScreenId.HeroDetail => store.Heroes.FirstOrDefault() is { } h ? $"{h.displayName}\nLv.{h.level} • {h.frameTier}\nVariant: {h.currentVariant ?? "BASE"}\nAwakening: {h.awakeningLevel}\n\nUse TRAIN to exercise the live progression endpoint." : "No owned ninja yet.",
                ScreenId.Formation => "Formation 5\n\n" + string.Join("\n", (store.Formation?.heroes ?? Array.Empty<OwnedHeroDto>()).Select((h, i) => $"{i + 1}. {h.displayName}  Lv.{h.level}")),
                ScreenId.Adventure => BuildAdventure(store),
                ScreenId.Battle => "Battle Debug\n\n5 vs 5 deterministic simulation\nDefault stage: c1-s1\nServer seed • structured skills/passives • authoritative rewards\n\nUse Adventure for campaign progression and boss multi-wave battles.",
                ScreenId.Summon => $"Complete Roster+ Summon\n\nCost: {PlayableGameStore.CompleteRosterSummonCost} Diamond\nHard pity and duplicate Hero Coin conversion are server-authoritative.",
                ScreenId.Arena => BuildArena(store),
                ScreenId.ShadowArena => "Shadow Arena\n\n15 ninja • 3 squads • best-of-three foundation.",
                ScreenId.Guild => "Guild\n\nMembership • contribution • mission • boss foundations.",
                ScreenId.Shop => BuildShop(store),
                ScreenId.Inventory => BuildInventory(store),
                ScreenId.Quest => "Quests\n\nDaily objectives and reset cadence foundations.",
                ScreenId.Events => "Events\n\nVersioned event/objective definitions with server clock boundaries.",
                ScreenId.Mail => "Mail\n\nPlayer mail and attachment domain foundations.",
                ScreenId.Settings => "Settings\n\nLanguage: English / Tiếng Việt\nAudio, graphics and account controls attach here.",
                _ => screenId.ToString()
            };
        }

        private static string BuildAdventure(PlayableGameStore store)
        {
            CampaignStageListDto campaign = store.Campaign; if (campaign?.stages == null || campaign.stages.Length == 0) return "Adventure\n\nNo campaign catalog loaded.";
            string lines = string.Join("\n", campaign.stages.Select(stage => { string gate = stage.unlocked ? (stage.clearCount > 0 ? new string('★', Mathf.Clamp(stage.bestStars, 0, 3)) : "READY") : "LOCKED"; return $"{stage.stageId}  C{stage.chapter}-{stage.stageIndex}  {stage.difficulty}  {gate}  E{stage.energyCost}  W{stage.waveCount}  {stage.nameEn}"; }));
            CampaignStageDto next = store.RecommendedStage; string nextText = next == null ? "No stage currently playable" : $"Next: {next.stageId} • {next.nameEn} • {next.waveCount} wave(s) • {next.energyCost} Energy";
            return $"Adventure • Lv.{campaign.playerLevel}\n{campaign.catalogVersion}\n\n{lines}\n\n{nextText}";
        }

        private static string BuildArena(PlayableGameStore store)
        {
            ArenaStateDto arena = store.Arena; if (arena == null) return "Arena\n\nArena state is loading...";
            ArenaOpponentDto[] opponents = arena.opponents ?? Array.Empty<ArenaOpponentDto>();
            string lines = opponents.Length == 0 ? "No five-ninja opponents available." : string.Join("\n", opponents.Select((opponent, index) => $"{index + 1}. {(opponent.training ? "[TRAINING] " : string.Empty)}{opponent.displayName} • R{opponent.rating:N0} • P{opponent.power:N0}"));
            return $"Arena • {arena.seasonId}\nRating {arena.rating:N0}\n{arena.ratingProfileVersion}\n\n{lines}";
        }

        private static string BuildShop(PlayableGameStore store)
        {
            ShopViewDto view = store.Shop; if (view == null) return "Shops\n\nShop state is loading...";
            string lines = string.Join("\n", (view.shops ?? Array.Empty<ShopEntryDto>()).SelectMany(shop => (shop.offers ?? Array.Empty<ShopOfferDto>()).Select(offer =>
                $"{shop.nameEn} • {offer.itemNameEn} x{offer.quantity} • {offer.price:N0} {offer.currency} • {(offer.purchasable ? "BUY" : offer.blockedReason ?? "LOCKED")} • {(offer.remaining.HasValue ? offer.remaining.Value + " left" : "∞")}")));
            return $"Shops • reset {view.resetKey}\n{view.catalogVersion}\n\n{lines}";
        }

        private static string BuildInventory(PlayableGameStore store)
        {
            InventoryViewDto inventory = store.Inventory; InventoryItemDto[] items = inventory?.items ?? Array.Empty<InventoryItemDto>();
            if (items.Length == 0) return $"Inventory\n{inventory?.catalogVersion ?? "item-catalog-v1"}\n\nNo stack items yet. Clear campaign stages to earn materials and summon tickets.";
            string lines = string.Join("\n", items.Take(24).Select(item => $"• {item.nameEn}  x{item.quantity:N0}  [{item.itemType}]")); return $"Inventory\n{inventory.catalogVersion}\n\n{lines}";
        }

        private void ConfigureAction()
        {
            if (primaryAction == null || primaryActionLabel == null) return;
            string label = screenId switch
            {
                ScreenId.Battle => "FIGHT c1-s1",
                ScreenId.Adventure => MobileGameBootstrap.IsReady && MobileGameBootstrap.Store.RecommendedStage != null ? "FIGHT " + MobileGameBootstrap.Store.RecommendedStage.stageId : "REFRESH",
                ScreenId.Arena => MobileGameBootstrap.IsReady && MobileGameBootstrap.Store.RecommendedArenaOpponent is { } opponent ? opponent.training ? "TRAIN MIRROR" : "FIGHT " + opponent.displayName : "REFRESH ARENA",
                ScreenId.Shop => "BUY NEXT OFFER",
                ScreenId.Summon => "SUMMON", ScreenId.HeroDetail => "TRAIN", ScreenId.Inventory => "REFRESH INVENTORY", _ => "REFRESH"
            };
            primaryActionLabel.text = label;
        }

        private void SetStatus(string value) { if (statusText != null) statusText.text = value ?? string.Empty; }
    }
}
