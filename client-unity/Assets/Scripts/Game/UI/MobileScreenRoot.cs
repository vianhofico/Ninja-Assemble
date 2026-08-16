using System;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace NinjaAssemble.UI
{
    public sealed class MobileScreenRoot : MonoBehaviour
    {
        [SerializeField] private ScreenId screenId;
        [SerializeField] private string titleKey = string.Empty;
        [SerializeField] private RectTransform safeAreaRoot;

        public ScreenId ScreenId => screenId;
        public string TitleKey => titleKey;
        public RectTransform SafeAreaRoot => safeAreaRoot;

        public void Configure(ScreenId id, string localizedTitleKey, RectTransform safeArea)
        {
            screenId = id;
            titleKey = localizedTitleKey ?? string.Empty;
            safeAreaRoot = safeArea;
        }

        public void Open(ScreenId target)
        {
            string sceneName = MobileSceneNames.For(target);
            if (string.IsNullOrWhiteSpace(sceneName))
                throw new InvalidOperationException($"No mobile scene registered for {target}");
            SceneManager.LoadScene(sceneName);
        }
    }

    public static class MobileSceneNames
    {
        public const string Bootstrap = "Bootstrap";

        public static string For(ScreenId id) => id switch
        {
            ScreenId.Home => "Home",
            ScreenId.NinjaRoster => "NinjaRoster",
            ScreenId.HeroDetail => "HeroDetail",
            ScreenId.Formation => "Formation",
            ScreenId.Adventure => "Adventure",
            ScreenId.Battle => "Battle",
            ScreenId.Summon => "Summon",
            ScreenId.Arena => "Arena",
            ScreenId.ShadowArena => "ShadowArena",
            ScreenId.Guild => "Guild",
            ScreenId.Shop => "Shop",
            ScreenId.Inventory => "Inventory",
            ScreenId.Quest => "Quest",
            ScreenId.Events => "Events",
            ScreenId.Mail => "Mail",
            ScreenId.Settings => "Settings",
            _ => throw new ArgumentOutOfRangeException(nameof(id), id, null)
        };
    }
}
