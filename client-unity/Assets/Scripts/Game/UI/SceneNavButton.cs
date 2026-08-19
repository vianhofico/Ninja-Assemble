using NinjaAssemble.Bootstrap;
using UnityEngine;
using UnityEngine.UI;

namespace NinjaAssemble.UI
{
    [RequireComponent(typeof(Button))]
    public sealed class SceneNavButton : MonoBehaviour
    {
        [SerializeField] private ScreenId target;
        private Button button;

        public void Configure(ScreenId targetScreen) => target = targetScreen;

        private void Awake()
        {
            button = GetComponent<Button>();
            if (button == null)
            {
                Debug.LogError("SceneNavButton requires a Button component", this);
                enabled = false;
                return;
            }
            button.onClick.AddListener(Navigate);
        }

        private void OnDestroy()
        {
            if (button != null) button.onClick.RemoveListener(Navigate);
        }

        private void Navigate()
        {
            if (!MobileGameBootstrap.IsReady) return;
            string sceneName = MobileSceneNames.For(target);
            if (!string.IsNullOrWhiteSpace(sceneName)) UnityEngine.SceneManagement.SceneManager.LoadScene(sceneName);
        }
    }
}
