using System;
using System.Threading.Tasks;
using NinjaAssemble.Network;
using NinjaAssemble.Playable;
using NinjaAssemble.Progression;
using NinjaAssemble.UI;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace NinjaAssemble.Bootstrap
{
    public enum MobileRuntimeMode
    {
        OfflinePlaytest = 0,
        Online = 1
    }

    public sealed class MobileGameBootstrap : MonoBehaviour
    {
        [SerializeField] private GameApiConfig apiConfig;
        [SerializeField] private MobileRuntimeMode runtimeMode = MobileRuntimeMode.OfflinePlaytest;
        [SerializeField] private string guestDisplayName = "Ninja";
        [SerializeField] private bool autoEnterHome = true;

        public static PlayableGameStore Store { get; private set; }
        public static AdvancedProgressionStore AdvancedProgression { get; private set; }
        public static MobileRuntimeMode RuntimeMode { get; private set; } = MobileRuntimeMode.OfflinePlaytest;
        public static string StartupError { get; private set; }
        public static bool IsReady => Store != null && !string.IsNullOrWhiteSpace(Store.PlayerId);
        public static bool IsOffline => RuntimeMode == MobileRuntimeMode.OfflinePlaytest;

        public void Configure(GameApiConfig config, string displayName = "Ninja", bool enterHome = true, MobileRuntimeMode mode = MobileRuntimeMode.OfflinePlaytest)
        {
            apiConfig = config;
            guestDisplayName = string.IsNullOrWhiteSpace(displayName) ? "Ninja" : displayName;
            autoEnterHome = enterHome;
            runtimeMode = mode;
        }

        private async void Awake()
        {
            DontDestroyOnLoad(gameObject);
            RuntimeMode = runtimeMode;
            StartupError = null;

            if (Store != null)
            {
                if (RuntimeMode == MobileRuntimeMode.Online && AdvancedProgression == null && apiConfig != null)
                {
                    AdvancedProgression = new AdvancedProgressionStore(new AdvancedProgressionClient(apiConfig));
                    await AdvancedProgression.InitializeAsync(Store.PlayerId);
                }
                if (autoEnterHome) SceneManager.LoadScene(MobileSceneNames.For(ScreenId.Home));
                return;
            }

            try
            {
                IPlayableGameService service;
                if (RuntimeMode == MobileRuntimeMode.OfflinePlaytest)
                {
                    service = new OfflinePlayableGameService();
                    AdvancedProgression = null;
                }
                else
                {
                    if (apiConfig == null) throw new InvalidOperationException("GameApiConfig is not assigned");
                    service = new OnlinePlayableGameService(apiConfig);
                }

                Store = new PlayableGameStore(service);
                string guestKey = LoadOrCreateGuestKey();
                await Store.LoginAndBootstrapAsync(guestKey, guestDisplayName);

                if (RuntimeMode == MobileRuntimeMode.Online)
                {
                    AdvancedProgression = new AdvancedProgressionStore(new AdvancedProgressionClient(apiConfig));
                    await AdvancedProgression.InitializeAsync(Store.PlayerId);
                }

                if (autoEnterHome) SceneManager.LoadScene(MobileSceneNames.For(ScreenId.Home));
            }
            catch (Exception exception)
            {
                StartupError = exception.Message;
                Store = null;
                Debug.LogError("Ninja Assemble bootstrap failed: " + exception);
            }
        }

        public static async Task WaitUntilReadyAsync()
        {
            while (!IsReady && string.IsNullOrWhiteSpace(StartupError)) await Task.Yield();
            if (!IsReady) throw new InvalidOperationException(StartupError ?? "Bootstrap failed");
        }

        public static void ResetStaticStateForTests()
        {
            Store = null;
            AdvancedProgression = null;
            StartupError = null;
            RuntimeMode = MobileRuntimeMode.OfflinePlaytest;
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
