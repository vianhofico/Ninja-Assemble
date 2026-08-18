using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Threading.Tasks;
using NinjaAssemble.Bootstrap;
using NinjaAssemble.Network;
using NinjaAssemble.Playable;
using NinjaAssemble.UI;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

namespace NinjaAssemble.Heroes
{
    public sealed class RosterFormationPlayableBridge : MonoBehaviour
    {
        private static int selectedIndex;
        private readonly Dictionary<ScreenId, ScreenBinding> screens = new Dictionary<ScreenId, ScreenBinding>();
        private PlayableGameStore store;
        private IPlayableGameService api;
        private bool binding;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        private static void Install()
        {
            var root = new GameObject("RosterFormationPlayableBridgeRuntime");
            DontDestroyOnLoad(root);
            root.AddComponent<RosterFormationPlayableBridge>();
        }

        private void Update()
        {
            if (binding || !MobileGameBootstrap.IsReady) return;
            if (screens.Count == 3) return;
            binding = true;
            try
            {
                store = MobileGameBootstrap.Store;
                api = ResolveApi(store);
                BindMissingScreens();
                RenderAll();
            }
            catch (Exception exception) { Debug.LogException(exception); }
            finally { binding = false; }
        }

        private static IPlayableGameService ResolveApi(PlayableGameStore value)
        {
            FieldInfo field = typeof(PlayableGameStore).GetField("api", BindingFlags.Instance | BindingFlags.NonPublic);
            return field?.GetValue(value) as IPlayableGameService ?? throw new InvalidOperationException("PlayableGameStore gameplay service unavailable");
        }

        private void BindMissingScreens()
        {
            FieldInfo screenField = typeof(MobileVerticalSliceController).GetField("screenId", BindingFlags.Instance | BindingFlags.NonPublic);
            foreach (MobileVerticalSliceController controller in FindObjectsOfType<MobileVerticalSliceController>(true))
            {
                if (!(screenField?.GetValue(controller) is ScreenId id)) continue;
                if (id != ScreenId.NinjaRoster && id != ScreenId.HeroDetail && id != ScreenId.Formation) continue;
                if (screens.ContainsKey(id)) continue;
                var binding = ScreenBinding.From(controller, id);
                screens[id] = binding;
                if (binding.Primary == null) continue;
                binding.Primary.onClick.RemoveAllListeners();
                if (id == ScreenId.NinjaRoster) binding.Primary.onClick.AddListener(SelectNextHero);
                if (id == ScreenId.Formation) binding.Primary.onClick.AddListener(AddSelectedToFormation);
                if (id == ScreenId.HeroDetail)
                {
                    binding.Primary.onClick.AddListener(AwakenSelected);
                    binding.Secondary = CreateSecondaryButton(binding, "TRAIN", TrainSelected);
                }
            }
        }

        private OwnedHeroDto SelectedHero
        {
            get
            {
                OwnedHeroDto[] heroes = store?.Heroes ?? Array.Empty<OwnedHeroDto>();
                if (heroes.Length == 0) return null;
                selectedIndex = Mathf.Clamp(selectedIndex, 0, heroes.Length - 1);
                return heroes[selectedIndex];
            }
        }

        private void SelectNextHero()
        {
            OwnedHeroDto[] heroes = store?.Heroes ?? Array.Empty<OwnedHeroDto>();
            if (heroes.Length == 0) return;
            selectedIndex = (selectedIndex + 1) % heroes.Length;
            SetStatus(ScreenId.NinjaRoster, "Selected " + (SelectedHero?.displayName ?? "ninja"));
            RenderAll();
        }

        private async void AwakenSelected()
        {
            OwnedHeroDto hero = SelectedHero;
            if (hero == null || api == null || store == null) return;
            try
            {
                AwakeningViewDto preview = await api.GetAwakeningAsync(store.PlayerId, hero.id);
                if (preview == null || !preview.available)
                {
                    SetStatus(ScreenId.HeroDetail, "This Hero Version has no Awakening.");
                    RenderAll();
                    return;
                }
                if (preview.awakened)
                {
                    SetStatus(ScreenId.HeroDetail, (preview.awakeningName ?? "Awakening") + " is already active.");
                    RenderAll();
                    return;
                }

                SetStatus(ScreenId.HeroDetail, "Awakening " + hero.displayName + "...");
                AwakeningViewDto result = await api.AwakenAsync(store.PlayerId, hero.id);
                await RefreshHeroes();
                string visualState = result?.visual == null ? string.Empty : " • Visual: " + result.visual.status;
                SetStatus(ScreenId.HeroDetail, "AWAKENED → " + (result?.awakeningName ?? hero.displayName) + visualState);
                RenderAll();
            }
            catch (Exception exception) { Debug.LogException(exception); SetStatus(ScreenId.HeroDetail, exception.Message); }
        }

        private async void TrainSelected()
        {
            OwnedHeroDto hero = SelectedHero;
            if (hero == null || store == null) return;
            try
            {
                SetStatus(ScreenId.HeroDetail, "Training " + hero.displayName + "...");
                UpgradeResultDto result = await store.LevelUpAsync(hero.id);
                SetStatus(ScreenId.HeroDetail, $"{result.hero.displayName} → Lv.{result.hero.level} • -{result.goldCost} Gold");
                RenderAll();
            }
            catch (Exception exception) { Debug.LogException(exception); SetStatus(ScreenId.HeroDetail, exception.Message); }
        }

        private async void AddSelectedToFormation()
        {
            OwnedHeroDto selected = SelectedHero;
            if (selected == null || store == null || api == null) return;
            OwnedHeroDto[] current = store.Formation?.heroes ?? Array.Empty<OwnedHeroDto>();
            if (current.Any(hero => hero.id == selected.id)) { SetStatus(ScreenId.Formation, selected.displayName + " is already in the formation."); return; }
            if (current.Length != 5) { SetStatus(ScreenId.Formation, "Formation must contain five ninja before replacement."); return; }
            string[] ids = current.Select(hero => hero.id).ToArray();
            ids[4] = selected.id;
            try
            {
                SetStatus(ScreenId.Formation, "Replacing slot 5 with " + selected.displayName + "...");
                FormationDto formation = await api.SaveFormationAsync(store.PlayerId, ids);
                SetBackingField(store, "Formation", formation);
                SetStatus(ScreenId.Formation, selected.displayName + " saved in slot 5.");
                RenderAll();
            }
            catch (Exception exception) { Debug.LogException(exception); SetStatus(ScreenId.Formation, exception.Message); }
        }

        private async Task RefreshHeroes()
        {
            if (api == null || store == null) return;
            OwnedHeroDto[] heroes = await api.GetOwnedHeroesAsync(store.PlayerId) ?? Array.Empty<OwnedHeroDto>();
            SetBackingField(store, "Heroes", heroes);
            selectedIndex = Math.Min(selectedIndex, Math.Max(0, heroes.Length - 1));
        }

        private static void SetBackingField(object target, string propertyName, object value)
        {
            if (target == null) return;
            FieldInfo field = target.GetType().GetField("<" + propertyName + ">k__BackingField", BindingFlags.Instance | BindingFlags.NonPublic);
            if (field == null) throw new InvalidOperationException(propertyName + " backing field unavailable");
            field.SetValue(target, value);
        }

        private void RenderAll()
        {
            OwnedHeroDto hero = SelectedHero;
            if (screens.TryGetValue(ScreenId.NinjaRoster, out ScreenBinding roster) && roster.Body != null)
            {
                OwnedHeroDto[] heroes = store?.Heroes ?? Array.Empty<OwnedHeroDto>();
                string rows = string.Join("\n", heroes.Take(30).Select((value, index) => $"{(index == selectedIndex ? "▶" : "•")} {value.displayName} • Lv.{value.level} • {value.heroId} {(value.awakened ? "• AWAKENED" : string.Empty)}"));
                roster.Body.text = $"Ninja Roster • {heroes.Length} owned\n\n{rows}";
                if (roster.PrimaryLabel != null) roster.PrimaryLabel.text = heroes.Length > 1 ? "SELECT NEXT" : "SELECTED";
            }
            if (screens.TryGetValue(ScreenId.HeroDetail, out ScreenBinding detail) && detail.Body != null)
            {
                if (hero == null)
                {
                    detail.Body.text = "No owned ninja.";
                    if (detail.PrimaryLabel != null) detail.PrimaryLabel.text = "AWAKEN";
                }
                else
                {
                    string awakening = string.IsNullOrWhiteSpace(hero.awakeningId) ? "Not available" : hero.awakened ? (hero.awakeningName ?? hero.awakeningId) + " • ACTIVE" : (hero.awakeningName ?? hero.awakeningId) + " • READY";
                    detail.Body.text = $"{hero.displayName}\nHero Version: {hero.heroId}\nLv.{hero.level} • {hero.frameTier}\nAwakening: {awakening}\n\nSelected from Ninja Roster.";
                    if (detail.PrimaryLabel != null) detail.PrimaryLabel.text = hero.awakened ? "AWAKENED" : string.IsNullOrWhiteSpace(hero.awakeningId) ? "NO AWAKENING" : "AWAKEN";
                    if (detail.Primary != null) detail.Primary.interactable = !hero.awakened && !string.IsNullOrWhiteSpace(hero.awakeningId);
                }
            }
            if (screens.TryGetValue(ScreenId.Formation, out ScreenBinding formation) && formation.Body != null)
            {
                OwnedHeroDto[] team = store?.Formation?.heroes ?? Array.Empty<OwnedHeroDto>();
                string rows = string.Join("\n", team.Select((value, index) => $"{index + 1}. {value.displayName} • {value.heroId} • Lv.{value.level}{(value.awakened ? " • AWAKENED" : string.Empty)}"));
                formation.Body.text = $"Formation 5\n\n{rows}\n\nSelected candidate: {hero?.displayName ?? "none"}\nADD SELECTED replaces slot 5.";
                if (formation.PrimaryLabel != null) formation.PrimaryLabel.text = "ADD SELECTED";
            }
        }

        private void SetStatus(ScreenId id, string value)
        {
            if (screens.TryGetValue(id, out ScreenBinding binding) && binding.Status != null) binding.Status.text = value ?? string.Empty;
        }

        private static Button CreateSecondaryButton(ScreenBinding binding, string label, UnityEngine.Events.UnityAction action)
        {
            if (binding.Primary == null) return null;
            GameObject clone = Instantiate(binding.Primary.gameObject, binding.Primary.transform.parent);
            clone.name = "SecondaryTrainAction";
            Button button = clone.GetComponent<Button>();
            if (button == null) return null;
            button.onClick.RemoveAllListeners();
            button.onClick.AddListener(action);
            RectTransform rect = clone.GetComponent<RectTransform>();
            RectTransform source = binding.Primary.GetComponent<RectTransform>();
            if (rect != null && source != null) rect.anchoredPosition = source.anchoredPosition + new Vector2(0f, -68f);
            TMP_Text text = clone.GetComponentInChildren<TMP_Text>();
            if (text != null) text.text = label;
            return button;
        }

        private sealed class ScreenBinding
        {
            public ScreenId Id; public TMP_Text Body; public TMP_Text Status; public Button Primary; public TMP_Text PrimaryLabel; public Button Secondary;
            public static ScreenBinding From(MobileVerticalSliceController controller, ScreenId id)
            {
                return new ScreenBinding { Id = id, Body = Field<TMP_Text>(controller, "bodyText"), Status = Field<TMP_Text>(controller, "statusText"), Primary = Field<Button>(controller, "primaryAction"), PrimaryLabel = Field<TMP_Text>(controller, "primaryActionLabel") };
            }
            private static T Field<T>(object target, string name) where T : class => target?.GetType().GetField(name, BindingFlags.Instance | BindingFlags.NonPublic)?.GetValue(target) as T;
        }
    }
}
