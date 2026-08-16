using System;
using System.Threading.Tasks;
using NinjaAssemble.Network;
using NinjaAssemble.Playable;
using NinjaAssemble.UI;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace NinjaAssemble.Bootstrap
{
    public sealed class MobileGameBootstrap : MonoBehaviour
    {
        [SerializeField] private GameApiConfig apiConfig;
        [SerializeField] private string guestDisplayName = "Ninja";
        [SerializeField] private bool autoEnterHome = true;

        public static PlayableGameStore Store { get; private set; }
        public static bool IsReady => Store != null && !string.IsNullOrWhiteSpace(Store.PlayerId);

        public void Configure(GameApiConfig config, string displayName = "Ninja", bool enterHome = true)
        {
            apiConfig = config;
            guestDisplayName = string.IsNullOrWhiteSpace(displayName) ? "Ninja" : displayName;
            autoEnterHome = enterHome;
        }

        private async void Awake()
        {
            DontDestroyOnLoad(gameObject);
            if (Store != null)
            {
                if (autoEnterHome) SceneManager.LoadScene(MobileSceneNames.For(ScreenId.Home));
                return;
            }

            try
            {
                if (apiConfig == null) throw new InvalidOperationException("GameApiConfig is not assigned");
                var client = new GameApiClient(apiConfig.BaseUrl);
                Store = new PlayableGameStore(client);
                string guestKey = LoadOrCreateGuestKey();
                await Store.LoginAndBootstrapAsync(guestKey, guestDisplayName);
                if (autoEnterHome) SceneManager.LoadScene(MobileSceneNames.For(ScreenId.Home));
            }
            catch (Exception exception)
            {
                Debug.LogException(exception);
            }
        }

        public static async Task WaitUntilReadyAsync()
        {
            while (!IsReady) await Task.Yield();
        }

        private static string LoadOrCreateGuestKey()
        {
            const string key = "ninjaassemble.guest-key";
            string value = PlayerPrefs.GetString(key, string.Empty);
            if (!string.IsNullOrWhiteSpace(value)) return value;
            value = Guid.NewGuid().ToString("N");
            PlayerPrefs.SetString(key, value);
            PlayerPrefs.Save();
            return value;
        }
    }
}
