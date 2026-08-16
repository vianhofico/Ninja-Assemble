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
            button.onClick.AddListener(Navigate);
        }

        private void OnDestroy()
        {
            if (button != null) button.onClick.RemoveListener(Navigate);
        }

        private void Navigate()
        {
            string sceneName = MobileSceneNames.For(target);
            UnityEngine.SceneManagement.SceneManager.LoadScene(sceneName);
        }
    }
}
