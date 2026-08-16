using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using NinjaAssemble.Localization;
using NinjaAssemble.Network;

namespace NinjaAssemble.Heroes
{
    public sealed class HeroRosterStore
    {
        private readonly GameApiClient api;
        private readonly Dictionary<string, HeroCatalogDto> heroes = new();
        private readonly Dictionary<string, HeroVariantDto[]> variants = new();
        private readonly Dictionary<string, HeroKitDto> kits = new();

        public HeroRosterStore(GameApiClient api) => this.api = api ?? throw new ArgumentNullException(nameof(api));
        public IReadOnlyCollection<HeroCatalogDto> Heroes => heroes.Values;

        public async Task RefreshAsync()
        {
            HeroCatalogDto[] catalog = await api.GetHeroCatalogAsync();
            heroes.Clear();
            foreach (HeroCatalogDto hero in catalog) heroes[hero.id] = hero;
        }

        public async Task<HeroVariantDto[]> VariantsAsync(string characterId)
        {
            if (!variants.TryGetValue(characterId, out HeroVariantDto[] value))
            {
                value = await api.GetVariantsAsync(characterId);
                variants[characterId] = value;
            }
            return value;
        }

        public async Task<HeroKitDto> KitAsync(string characterId, string variant = null)
        {
            string key = characterId + "::" + (variant ?? string.Empty);
            if (!kits.TryGetValue(key, out HeroKitDto value))
            {
                value = await api.GetKitAsync(characterId, variant);
                kits[key] = value;
            }
            return value;
        }

        public static string TechniqueName(TechniqueDto technique)
        {
            return LocalizationRuntime.Current != null && LocalizationRuntime.Current.Language == GameLanguage.Vietnamese
                ? technique.nameVi : technique.nameEn;
        }

        public static string TechniqueDescription(TechniqueDto technique)
        {
            return LocalizationRuntime.Current != null && LocalizationRuntime.Current.Language == GameLanguage.Vietnamese
                ? technique.descriptionVi : technique.descriptionEn;
        }

        public IEnumerable<HeroCatalogDto> Filter(string group, string search)
        {
            IEnumerable<HeroCatalogDto> query = heroes.Values;
            if (!string.IsNullOrWhiteSpace(group)) query = query.Where(h => string.Equals(h.group, group, StringComparison.OrdinalIgnoreCase));
            if (!string.IsNullOrWhiteSpace(search)) query = query.Where(h => h.character.IndexOf(search, StringComparison.OrdinalIgnoreCase) >= 0);
            return query.OrderBy(h => h.character);
        }
    }
}
