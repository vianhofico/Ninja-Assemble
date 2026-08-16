using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;
using UnityEngine;

namespace NinjaAssemble.Presentation
{
    public sealed class HeroArtRuntimeCatalog
    {
        private const string ResourcePath = "Generated/hero-art-runtime-catalog";
        private readonly Dictionary<string, HeroArtRuntimeEntry> entries = new Dictionary<string, HeroArtRuntimeEntry>(StringComparer.OrdinalIgnoreCase);

        private HeroArtRuntimeCatalog(HeroArtRuntimePayload payload)
        {
            if (payload == null || payload.entries == null) return;
            foreach (HeroArtRuntimeEntry entry in payload.entries)
            {
                if (entry == null || string.IsNullOrWhiteSpace(entry.characterId)) continue;
                entries[Key(entry.characterId, entry.variant)] = entry;
            }
        }

        public static HeroArtRuntimeCatalog Load()
        {
            TextAsset asset = Resources.Load<TextAsset>(ResourcePath);
            if (asset == null)
            {
                Debug.LogWarning("Hero art runtime catalog is missing; all battle actors will use fallback presentation.");
                return new HeroArtRuntimeCatalog(new HeroArtRuntimePayload());
            }
            HeroArtRuntimePayload payload = JsonUtility.FromJson<HeroArtRuntimePayload>(asset.text);
            return new HeroArtRuntimeCatalog(payload);
        }

        public HeroArtRuntimeEntry Resolve(string characterId, string variant)
        {
            HeroArtRuntimeEntry exact;
            if (entries.TryGetValue(Key(characterId, variant), out exact)) return exact;
            return HeroArtRuntimeEntry.Derived(characterId, variant);
        }

        private static string Key(string characterId, string variant)
        {
            return (characterId ?? string.Empty).Trim() + "::" + (variant ?? string.Empty).Trim();
        }

        public static string Slug(string value)
        {
            if (string.IsNullOrWhiteSpace(value)) return "base";
            string normalized = value.Normalize(NormalizationForm.FormD);
            var builder = new StringBuilder();
            bool dash = false;
            foreach (char c in normalized)
            {
                if (CharUnicodeInfo.GetUnicodeCategory(c) == UnicodeCategory.NonSpacingMark) continue;
                char lower = char.ToLowerInvariant(c);
                if (char.IsLetterOrDigit(lower))
                {
                    builder.Append(lower);
                    dash = false;
                }
                else if (!dash && builder.Length > 0)
                {
                    builder.Append('-');
                    dash = true;
                }
            }
            return builder.ToString().Trim('-');
        }
    }

    [Serializable]
    public sealed class HeroArtRuntimePayload
    {
        public HeroArtRuntimeEntry[] entries = Array.Empty<HeroArtRuntimeEntry>();
    }

    [Serializable]
    public sealed class HeroArtRuntimeEntry
    {
        public string characterId;
        public string variant;
        public string portraitAddress;
        public string iconAddress;
        public string prefabAddress;
        public string animationSet;
        public string vfxSet;
        public string sfxSet;
        public string status;

        public bool IsReady => string.Equals(status, "READY", StringComparison.OrdinalIgnoreCase);

        public static HeroArtRuntimeEntry Derived(string characterId, string variant)
        {
            string character = (characterId ?? string.Empty).Trim();
            string form = HeroArtRuntimeCatalog.Slug(variant);
            string root = "heroes/" + character + "/" + form;
            return new HeroArtRuntimeEntry
            {
                characterId = character,
                variant = variant ?? string.Empty,
                portraitAddress = root + "/portrait",
                iconAddress = root + "/icon",
                prefabAddress = root + "/prefab",
                animationSet = "animations/" + character + "/" + form,
                vfxSet = "vfx/" + character + "/" + form,
                sfxSet = "sfx/" + character + "/" + form,
                status = "UNTRACKED"
            };
        }
    }
}
