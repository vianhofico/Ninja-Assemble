using System;
using System.Linq;
using NinjaAssemble.Heroes;
using NinjaAssemble.Network;
using NinjaAssemble.Runtime;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

namespace NinjaAssemble.UI
{
    public sealed class HeroProgressionPanelController : MonoBehaviour
    {
        [SerializeField] private GameApiConfig apiConfig;
        [SerializeField] private TMP_Text frameLabel;
        [SerializeField] private TMP_Text selectedPathLabel;
        [SerializeField] private TMP_Text requirementLabel;
        [SerializeField] private TMP_Text resultLabel;
        [SerializeField] private TMP_Text evolvedKitLabel;
        [SerializeField] private TMP_Dropdown evolutionDropdown;
        [SerializeField] private Button frameAdvanceButton;
        [SerializeField] private Button evolveButton;

        private ProgressionApiClient progression;
        private OwnedHeroDto owned;
        private EvolutionPathDto[] paths = Array.Empty<EvolutionPathDto>();

        private async void OnEnable()
        {
            try
            {
                if (!GameRuntime.Ready || string.IsNullOrWhiteSpace(GameRuntime.Selection.PlayerHeroId))
                {
                    SetInteractable(false);
                    return;
                }
                progression ??= new ProgressionApiClient(apiConfig);
                await RefreshAsync();
            }
            catch (Exception ex)
            {
                ShowError(ex);
            }
        }

        private async System.Threading.Tasks.Task RefreshAsync()
        {
            OwnedHeroDto[] heroes = await GameRuntime.Api.GetOwnedHeroesAsync(GameRuntime.Play.PlayerId);
            owned = heroes.FirstOrDefault(hero => hero.id == GameRuntime.Selection.PlayerHeroId);
            if (owned == null)
            {
                SetInteractable(false);
                return;
            }

            if (frameLabel != null) frameLabel.text = $"{owned.frameTier} · Lv.{owned.level}";
            paths = await progression.GetEvolutionPathsAsync(GameRuntime.Play.PlayerId, owned.characterId);
            if (evolutionDropdown != null)
            {
                evolutionDropdown.ClearOptions();
                evolutionDropdown.AddOptions(paths.Select(path => path.targetVariant).ToList());
                evolutionDropdown.value = 0;
            }
            SetInteractable(true);
            RenderSelectedPath();
            await RenderCurrentKitAsync();
        }

        public void RenderSelectedPath()
        {
            if (paths.Length == 0 || evolutionDropdown == null)
            {
                if (selectedPathLabel != null) selectedPathLabel.text = "No evolution path";
                if (requirementLabel != null) requirementLabel.text = string.Empty;
                if (evolveButton != null) evolveButton.interactable = false;
                return;
            }
            EvolutionPathDto path = paths[Math.Clamp(evolutionDropdown.value, 0, paths.Length - 1)];
            if (selectedPathLabel != null) selectedPathLabel.text = path.targetVariant;
            if (requirementLabel != null)
                requirementLabel.text = $"Lv.{path.minLevel} · {path.minFrame} · {path.goldCost:N0} Gold · after {path.prerequisiteVariant}";
        }

        public async void AdvanceFrame()
        {
            if (owned == null) return;
            try
            {
                FrameAdvanceResultDto result = await progression.FrameAdvanceAsync(GameRuntime.Play.PlayerId, owned.id);
                if (resultLabel != null) resultLabel.text = $"Frame → {result.frameTier}  ({result.goldCost:N0} Gold)";
                await RefreshAsync();
            }
            catch (Exception ex) { ShowError(ex); }
        }

        public async void Evolve()
        {
            if (owned == null || paths.Length == 0 || evolutionDropdown == null) return;
            try
            {
                EvolutionPathDto path = paths[Math.Clamp(evolutionDropdown.value, 0, paths.Length - 1)];
                EvolutionResultDto result = await progression.EvolveAsync(GameRuntime.Play.PlayerId, owned.id, path.targetVariant);
                GameRuntime.Selection.Variant = result.targetVariant;
                if (resultLabel != null)
                    resultLabel.text = result.alreadyUnlocked
                        ? $"Selected {result.targetVariant}"
                        : $"Evolved → {result.targetVariant}  ({result.goldCost:N0} Gold)";
                await RefreshAsync();
            }
            catch (Exception ex) { ShowError(ex); }
        }

        private async System.Threading.Tasks.Task RenderCurrentKitAsync()
        {
            if (evolvedKitLabel == null || owned == null) return;
            HeroKitDto kit = await GameRuntime.Catalog.KitAsync(owned.characterId, owned.currentVariant);
            evolvedKitLabel.text = string.Join("\n", kit.techniques.Select(t => "• " + HeroRosterStore.TechniqueName(t)));
        }

        private void SetInteractable(bool value)
        {
            if (frameAdvanceButton != null) frameAdvanceButton.interactable = value;
            if (evolveButton != null) evolveButton.interactable = value && paths.Length > 0;
        }

        private void ShowError(Exception ex)
        {
            Debug.LogException(ex);
            if (resultLabel != null) resultLabel.text = ex.Message;
        }
    }
}
