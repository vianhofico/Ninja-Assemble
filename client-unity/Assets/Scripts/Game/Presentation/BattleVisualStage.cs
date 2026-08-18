using System;
using System.Collections;
using System.Collections.Generic;
using System.Threading.Tasks;
using NinjaAssemble.Network;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

namespace NinjaAssemble.Presentation
{
    public sealed class BattleVisualStage : MonoBehaviour
    {
        private static readonly Vector2[] TeamAAnchors =
        {
            new Vector2(0.18f, 0.62f), new Vector2(0.29f, 0.76f), new Vector2(0.29f, 0.49f),
            new Vector2(0.18f, 0.35f), new Vector2(0.40f, 0.62f)
        };

        private static readonly Vector2[] TeamBAnchors =
        {
            new Vector2(0.82f, 0.62f), new Vector2(0.71f, 0.76f), new Vector2(0.71f, 0.49f),
            new Vector2(0.82f, 0.35f), new Vector2(0.60f, 0.62f)
        };

        private readonly Dictionary<string, BattleParticipantDto> participants = new Dictionary<string, BattleParticipantDto>();
        private readonly Dictionary<string, RectTransform> actorSlots = new Dictionary<string, RectTransform>();
        private readonly HeroAddressableLoader loader = new HeroAddressableLoader();
        private HeroArtRuntimeCatalog artCatalog;
        private RectTransform stageRoot;
        private BattleTimelinePlayer timeline;
        private PlayBattleDto currentBattle;

        public async Task PresentAsync(PlayBattleDto result)
        {
            if (result == null || result.battle == null) throw new ArgumentNullException(nameof(result));
            ClearStage();
            currentBattle = result;
            artCatalog = HeroArtRuntimeCatalog.Load();
            CreateStageRoot();
            timeline = stageRoot.gameObject.AddComponent<BattleTimelinePlayer>();
            timeline.PlaybackCompleted += OnPlaybackCompleted;

            BattleParticipantDto[] roster = result.participants ?? Array.Empty<BattleParticipantDto>();
            foreach (BattleParticipantDto participant in roster)
            {
                if (participant == null || string.IsNullOrWhiteSpace(participant.battleUnitId)) continue;
                participants[participant.battleUnitId] = participant;
                RectTransform slot = CreateSlot(participant);
                actorSlots[participant.battleUnitId] = slot;
                BattleActorView actor = await CreateActorAsync(participant, slot);
                timeline.Register(actor);
            }

            List<BattlePresentationEvent> events = BattlePresentationAdapter.From(result.battle);
            timeline.Play(events);
        }

        private void ClearStage()
        {
            participants.Clear();
            actorSlots.Clear();
            currentBattle = null;
            if (stageRoot != null)
            {
                stageRoot.gameObject.SetActive(false);
                Destroy(stageRoot.gameObject);
            }
            stageRoot = null;
            timeline = null;
        }

        private void CreateStageRoot()
        {
            var root = new GameObject("BattleVisualStageRuntime", typeof(RectTransform), typeof(CanvasRenderer), typeof(Image));
            stageRoot = root.GetComponent<RectTransform>();
            stageRoot.SetParent(transform, false);
            stageRoot.anchorMin = new Vector2(0.04f, 0.29f);
            stageRoot.anchorMax = new Vector2(0.96f, 0.86f);
            stageRoot.offsetMin = Vector2.zero;
            stageRoot.offsetMax = Vector2.zero;
            Image image = root.GetComponent<Image>();
            image.color = new Color(0.035f, 0.045f, 0.065f, 0.94f);
            image.raycastTarget = false;

            CreateText(stageRoot, "StageTitle", "5 vs 5", 24f, TextAlignmentOptions.Top, new Color(0.96f, 0.77f, 0.28f, 1f),
                new Vector2(0.42f, 0.90f), new Vector2(0.58f, 0.99f));
        }

        private RectTransform CreateSlot(BattleParticipantDto participant)
        {
            var slotObject = new GameObject("Slot_" + participant.side + "_" + participant.slot, typeof(RectTransform));
            RectTransform slot = slotObject.GetComponent<RectTransform>();
            slot.SetParent(stageRoot, false);
            Vector2 anchor = ResolveAnchor(participant.side, participant.slot);
            slot.anchorMin = anchor;
            slot.anchorMax = anchor;
            slot.pivot = new Vector2(0.5f, 0.5f);
            slot.sizeDelta = new Vector2(180f, 220f);
            slot.anchoredPosition = Vector2.zero;
            return slot;
        }

        private async Task<BattleActorView> CreateActorAsync(BattleParticipantDto participant, RectTransform slot)
        {
            HeroArtRuntimeEntry art = string.IsNullOrWhiteSpace(participant.heroId)
                ? artCatalog.Resolve(participant.characterId, participant.variant)
                : artCatalog.ResolveHeroVersion(participant.heroId, participant.awakened, participant.characterId, participant.variant);
            if (art.IsReady)
            {
                GameObject prefab = await loader.TryLoadPrefabAsync(art.prefabAddress);
                if (prefab != null)
                {
                    GameObject instance = Instantiate(prefab, slot, false);
                    RectTransform rect = instance.GetComponent<RectTransform>();
                    BattleActorView actor = instance.GetComponent<BattleActorView>();
                    if (rect != null && actor != null)
                    {
                        rect.anchorMin = Vector2.zero;
                        rect.anchorMax = Vector2.one;
                        rect.offsetMin = Vector2.zero;
                        rect.offsetMax = Vector2.zero;
                        ConfigureActor(actor, participant);
                        return actor;
                    }
                    Destroy(instance);
                    Debug.LogWarning("READY hero prefab must contain RectTransform + BattleActorView: " + art.prefabAddress);
                }
                else
                {
                    Debug.LogWarning("READY hero prefab Addressable could not be loaded: " + art.prefabAddress);
                }
            }
            return CreateFallbackActor(participant, slot);
        }

        private static void ConfigureActor(BattleActorView actor, BattleParticipantDto participant)
        {
            actor.Configure(
                participant.battleUnitId,
                participant.characterId,
                participant.heroId,
                participant.awakened,
                participant.awakeningId,
                participant.presentationKey,
                participant.variant,
                participant.displayName,
                participant.level,
                participant.maxHp);
        }

        private BattleActorView CreateFallbackActor(BattleParticipantDto participant, RectTransform slot)
        {
            var actorObject = new GameObject("Fallback_" + (string.IsNullOrWhiteSpace(participant.heroId) ? participant.characterId : participant.heroId), typeof(RectTransform), typeof(CanvasRenderer), typeof(Image));
            RectTransform rect = actorObject.GetComponent<RectTransform>();
            rect.SetParent(slot, false);
            rect.anchorMin = new Vector2(0.08f, 0.05f);
            rect.anchorMax = new Vector2(0.92f, 0.95f);
            rect.offsetMin = Vector2.zero;
            rect.offsetMax = Vector2.zero;
            Image body = actorObject.GetComponent<Image>();
            body.color = IsTeamA(participant.side)
                ? new Color(0.90f, 0.38f, 0.12f, 0.92f)
                : new Color(0.23f, 0.42f, 0.78f, 0.92f);
            body.raycastTarget = false;

            string initials = Initials(participant.displayName);
            CreateText(rect, "Initials", initials, 42f, TextAlignmentOptions.Center, Color.white,
                new Vector2(0.08f, 0.30f), new Vector2(0.92f, 0.84f));
            TMP_Text name = CreateText(rect, "Name", participant.displayName, 18f, TextAlignmentOptions.Center, Color.white,
                new Vector2(0.02f, 0.16f), new Vector2(0.98f, 0.34f));
            TMP_Text level = CreateText(rect, "Level", "Lv." + participant.level, 15f, TextAlignmentOptions.Center, new Color(1f, 0.88f, 0.5f, 1f),
                new Vector2(0.20f, 0.06f), new Vector2(0.80f, 0.18f));
            TMP_Text status = CreateText(rect, "Status", participant.awakened ? "AWAKENED" : string.Empty, 13f, TextAlignmentOptions.Center,
                participant.awakened ? new Color(1f, 0.72f, 0.20f, 1f) : Color.white,
                new Vector2(0.12f, 0.00f), new Vector2(0.88f, 0.09f));
            Slider hp = CreateHealthSlider(rect);

            BattleActorView actor = actorObject.AddComponent<BattleActorView>();
            actor.ConfigureUi(name, level, hp);
            actor.ConfigureStatusUi(status);
            ConfigureActor(actor, participant);
            return actor;
        }

        private void OnPlaybackCompleted()
        {
            if (currentBattle == null || currentBattle.battle == null) return;
            bool teamAWon = string.Equals(currentBattle.battle.outcome, "TEAM_A", StringComparison.OrdinalIgnoreCase);
            bool teamBWon = string.Equals(currentBattle.battle.outcome, "TEAM_B", StringComparison.OrdinalIgnoreCase);
            foreach (KeyValuePair<string, BattleParticipantDto> pair in participants)
            {
                bool winner = (teamAWon && IsTeamA(pair.Value.side)) || (teamBWon && !IsTeamA(pair.Value.side));
                if (!winner) continue;
                BattleActorView actor;
                if (timeline.TryGetActor(pair.Key, out actor)) actor.PlayVictory();
            }
        }

        private static Vector2 ResolveAnchor(string side, int slot)
        {
            int safeSlot = Mathf.Clamp(slot, 0, 4);
            return IsTeamA(side) ? TeamAAnchors[safeSlot] : TeamBAnchors[safeSlot];
        }

        private static bool IsTeamA(string side)
        {
            return string.Equals(side, "A", StringComparison.OrdinalIgnoreCase) ||
                   string.Equals(side, "TEAM_A", StringComparison.OrdinalIgnoreCase);
        }

        private static string Initials(string value)
        {
            if (string.IsNullOrWhiteSpace(value)) return "?";
            string[] pieces = value.Trim().Split(new[] { ' ' }, StringSplitOptions.RemoveEmptyEntries);
            if (pieces.Length == 1) return pieces[0].Substring(0, Mathf.Min(2, pieces[0].Length)).ToUpperInvariant();
            return (pieces[0].Substring(0, 1) + pieces[pieces.Length - 1].Substring(0, 1)).ToUpperInvariant();
        }

        private static TMP_Text CreateText(
            Transform parent,
            string name,
            string value,
            float fontSize,
            TextAlignmentOptions alignment,
            Color color,
            Vector2 anchorMin,
            Vector2 anchorMax)
        {
            var go = new GameObject(name, typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI));
            RectTransform rect = go.GetComponent<RectTransform>();
            rect.SetParent(parent, false);
            rect.anchorMin = anchorMin;
            rect.anchorMax = anchorMax;
            rect.offsetMin = Vector2.zero;
            rect.offsetMax = Vector2.zero;
            TextMeshProUGUI text = go.GetComponent<TextMeshProUGUI>();
            text.text = value ?? string.Empty;
            text.fontSize = fontSize;
            text.alignment = alignment;
            text.color = color;
            text.raycastTarget = false;
            text.enableWordWrapping = false;
            return text;
        }

        private static Slider CreateHealthSlider(RectTransform parent)
        {
            var sliderObject = new GameObject("HP", typeof(RectTransform), typeof(Slider));
            RectTransform rect = sliderObject.GetComponent<RectTransform>();
            rect.SetParent(parent, false);
            rect.anchorMin = new Vector2(0.08f, 0.88f);
            rect.anchorMax = new Vector2(0.92f, 0.96f);
            rect.offsetMin = Vector2.zero;
            rect.offsetMax = Vector2.zero;

            var backgroundObject = new GameObject("Background", typeof(RectTransform), typeof(CanvasRenderer), typeof(Image));
            RectTransform background = backgroundObject.GetComponent<RectTransform>();
            background.SetParent(rect, false);
            background.anchorMin = Vector2.zero;
            background.anchorMax = Vector2.one;
            background.offsetMin = Vector2.zero;
            background.offsetMax = Vector2.zero;
            Image backgroundImage = backgroundObject.GetComponent<Image>();
            backgroundImage.color = new Color(0.14f, 0.08f, 0.08f, 0.95f);
            backgroundImage.raycastTarget = false;

            var fillObject = new GameObject("Fill", typeof(RectTransform), typeof(CanvasRenderer), typeof(Image));
            RectTransform fill = fillObject.GetComponent<RectTransform>();
            fill.SetParent(rect, false);
            fill.anchorMin = Vector2.zero;
            fill.anchorMax = Vector2.one;
            fill.offsetMin = new Vector2(2f, 2f);
            fill.offsetMax = new Vector2(-2f, -2f);
            Image fillImage = fillObject.GetComponent<Image>();
            fillImage.color = new Color(0.30f, 0.90f, 0.32f, 1f);
            fillImage.raycastTarget = false;

            Slider slider = sliderObject.GetComponent<Slider>();
            slider.fillRect = fill;
            slider.direction = Slider.Direction.LeftToRight;
            slider.interactable = false;
            return slider;
        }
    }
}
