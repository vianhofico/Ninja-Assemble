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
        private const string GuestKeyPreference = "ninjaassemble.guest-key";
        private static MobileGameBootstrap instance;

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
            if (instance != null && instance != this)
            {
                Destroy(gameObject);
                return;
            }

            instance = this;
            DontDestroyOnLoad(gameObject);
            if (Store != null)
            {
                if (autoEnterHome) SceneManager.LoadScene(MobileSceneNames.For(ScreenId.Home));
                return;
            }

            try
            {
                if (apiConfig == null) throw new InvalidOperationException("GameApiConfig is not assigned");
                var client = new GameApiClient(apiConfig);
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

        private void OnDestroy()
        {
            if (instance == this) instance = null;
        }

        public static async Task WaitUntilReadyAsync()
        {
            while (!IsReady) await Task.Yield();
        }

        public static void ResetLocalGuestAndRestart()
        {
            Store = null;
            PlayerPrefs.DeleteKey(GuestKeyPreference);
            PlayerPrefs.Save();

            MobileGameBootstrap previous = instance;
            instance = null;
            if (previous != null) Destroy(previous.gameObject);
            SceneManager.LoadScene(MobileSceneNames.Bootstrap);
        }

        private static string LoadOrCreateGuestKey()
        {
            string value = PlayerPrefs.GetString(GuestKeyPreference, string.Empty);
            if (!string.IsNullOrWhiteSpace(value)) return value;
            value = Guid.NewGuid().ToString("N");
            PlayerPrefs.SetString(GuestKeyPreference, value);
            PlayerPrefs.Save();
            return value;
        }
    }
}
