using UnityEngine;

namespace NinjaAssemble.Localization
{
    public static class LocalizationRuntime
    {
        public static LocalizationService Current { get; private set; }

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
        private static void Bootstrap()
        {
            TextAsset csv = Resources.Load<TextAsset>("Localization/strings");
            if (csv == null)
            {
                Debug.LogError("Missing Resources/Localization/strings.csv");
                return;
            }
            Current = new LocalizationService(csv);
            string saved = PlayerPrefs.GetString("language", "en");
            Current.SetLanguage(saved == "vi" ? GameLanguage.Vietnamese : GameLanguage.English);
        }

        public static void SetLanguage(GameLanguage language)
        {
            Current?.SetLanguage(language);
            PlayerPrefs.SetString("language", language == GameLanguage.Vietnamese ? "vi" : "en");
            PlayerPrefs.Save();
        }
    }
}
