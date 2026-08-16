#if UNITY_EDITOR
using UnityEditor;
using UnityEditor.SceneManagement;

namespace NinjaAssemble.EditorTools
{
    public static class PlaytestEditor
    {
        private const string BootstrapScene = "Assets/Scenes/Mobile/Bootstrap.unity";

        [MenuItem("Ninja Assemble/Mobile/Play Development Game", priority = 1)]
        public static void PlayDevelopmentGame()
        {
            if (EditorApplication.isPlayingOrWillChangePlaymode) return;
            if (!EditorSceneManager.SaveCurrentModifiedScenesIfUserWantsTo()) return;

            MobileSceneBuilder.GenerateCompleteSceneShell();
            EditorSceneManager.OpenScene(BootstrapScene);
            EditorApplication.isPlaying = true;
        }

        [MenuItem("Ninja Assemble/Mobile/Stop Development Game", priority = 2)]
        public static void StopDevelopmentGame()
        {
            if (EditorApplication.isPlaying) EditorApplication.isPlaying = false;
        }
    }
}
#endif
