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
        private GameApiClient api;
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

        private static GameApiClient ResolveApi(PlayableGameStore value)
        {
            FieldInfo field = typeof(PlayableGameStore).GetField("api", BindingFlags.Instance | BindingFlags.NonPublic);
            return field?.GetValue(value) as GameApiClient ?? throw new InvalidOperationException("PlayableGameStore API client unavailable");
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
                binding.Primary.onClick.RemoveAllListeners();
                if (id == ScreenId.NinjaRoster) binding.Primary.onClick.AddListener(SelectNextHero);
                if (id == ScreenId.Formation) binding.Primary.onClick.AddListener(AddSelectedToFormation);
                if (id == ScreenId.HeroDetail)
                {
                    binding.Primary.onClick.AddListener(SelectNextVariant);
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
            SetStatus(ScreenId.NinjaRoster, "Selected " + SelectedHero.displayName);
            RenderAll();
        }

        private async void SelectNextVariant()
        {
            OwnedHeroDto hero = SelectedHero;
            if (hero == null) return;
            try
            {
                HeroVariantDto[] variants = await api.GetVariantsAsync(hero.characterId);
                string[] names = variants.Select(VariantName).Where(value => !string.IsNullOrWhiteSpace(value)).Distinct().ToArray();
                if (names.Length == 0) { SetStatus(ScreenId.HeroDetail, "No selectable variants for this ninja."); return; }
                string current = string.IsNullOrWhiteSpace(hero.currentVariant) ? "BASE" : hero.currentVariant;
                int currentIndex = Array.FindIndex(names, value => string.Equals(value, current, StringComparison.OrdinalIgnoreCase));
                string next = names[(currentIndex + 1 + names.Length) % names.Length];
                SetStatus(ScreenId.HeroDetail, "Switching variant to " + next + "...");
                await api.SelectVariantAsync(store.PlayerId, hero.id, string.Equals(next, "BASE", StringComparison.OrdinalIgnoreCase) ? null : next);
                await RefreshHeroes();
                SetStatus(ScreenId.HeroDetail, "Variant selected: " + (SelectedHero.currentVariant ?? "BASE"));
                RenderAll();
            }
            catch (Exception exception) { Debug.LogException(exception); SetStatus(ScreenId.HeroDetail, exception.Message); }
        }

        private async void TrainSelected()
        {
            OwnedHeroDto hero = SelectedHero;
            if (hero == null) return;
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
            if (selected == null) return;
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
            OwnedHeroDto[] heroes = await api.GetOwnedHeroesAsync(store.PlayerId);
            SetBackingField(store, "Heroes", heroes);
            selectedIndex = Math.Min(selectedIndex, Math.Max(0, heroes.Length - 1));
        }

        private static void SetBackingField(object target, string propertyName, object value)
        {
            FieldInfo field = target.GetType().GetField("<" + propertyName + ">k__BackingField", BindingFlags.Instance | BindingFlags.NonPublic);
            if (field == null) throw new InvalidOperationException(propertyName + " backing field unavailable");
            field.SetValue(target, value);
        }

        private static string VariantName(HeroVariantDto variant)
        {
            if (variant == null) return null;
            Type type = variant.GetType();
            foreach (string candidate in new[] { "variant", "name", "id", "variantId" })
            {
                FieldInfo field = type.GetField(candidate, BindingFlags.Instance | BindingFlags.Public | BindingFlags.IgnoreCase);
                string value = field?.GetValue(variant) as string;
                if (!string.IsNullOrWhiteSpace(value)) return value;
            }
            return null;
        }

        private void RenderAll()
        {
            OwnedHeroDto hero = SelectedHero;
            if (screens.TryGetValue(ScreenId.NinjaRoster, out ScreenBinding roster))
            {
                OwnedHeroDto[] heroes = store?.Heroes ?? Array.Empty<OwnedHeroDto>();
                string rows = string.Join("\n", heroes.Take(30).Select((value, index) => $"{(index == selectedIndex ? "▶" : "•")} {value.displayName} • Lv.{value.level} • [{value.currentVariant ?? "BASE"}]"));
                roster.Body.text = $"Ninja Roster • {heroes.Length} owned\n\n{rows}";
                roster.PrimaryLabel.text = heroes.Length > 1 ? "SELECT NEXT" : "SELECTED";
            }
            if (screens.TryGetValue(ScreenId.HeroDetail, out ScreenBinding detail))
            {
                detail.Body.text = hero == null ? "No owned ninja." : $"{hero.displayName}\nLv.{hero.level} • {hero.frameTier}\nVariant: {hero.currentVariant ?? "BASE"}\nAwakening: {hero.awakeningLevel}\n\nSelected from Ninja Roster.";
                detail.PrimaryLabel.text = "NEXT VARIANT";
            }
            if (screens.TryGetValue(ScreenId.Formation, out ScreenBinding formation))
            {
                OwnedHeroDto[] team = store?.Formation?.heroes ?? Array.Empty<OwnedHeroDto>();
                string rows = string.Join("\n", team.Select((value, index) => $"{index + 1}. {value.displayName} • Lv.{value.level}"));
                formation.Body.text = $"Formation 5\n\n{rows}\n\nSelected candidate: {hero?.displayName ?? "none"}\nADD SELECTED replaces slot 5.";
                formation.PrimaryLabel.text = "ADD SELECTED";
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
                return new ScreenBinding
                {
                    Id = id,
                    Body = Field<TMP_Text>(controller, "bodyText"),
                    Status = Field<TMP_Text>(controller, "statusText"),
                    Primary = Field<Button>(controller, "primaryAction"),
                    PrimaryLabel = Field<TMP_Text>(controller, "primaryActionLabel")
                };
            }
            private static T Field<T>(object target, string name) where T : class => target.GetType().GetField(name, BindingFlags.Instance | BindingFlags.NonPublic)?.GetValue(target) as T;
        }
    }
}
