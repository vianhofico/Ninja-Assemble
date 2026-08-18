using TMPro;
using UnityEngine;

namespace NinjaAssemble.Settings
{
    public static class AccessibilityPreferences
    {
        public const string TextScaleKey = "na.accessibility.text-scale";
        public const string ReduceMotionKey = "na.accessibility.reduce-motion";
        public const string HapticsKey = "na.accessibility.haptics";

        public static float TextScale => Mathf.Clamp(PlayerPrefs.GetFloat(TextScaleKey, 1f), 0.9f, 1.3f);
        public static bool ReduceMotion => PlayerPrefs.GetInt(ReduceMotionKey, 0) == 1;
        public static bool HapticsEnabled => PlayerPrefs.GetInt(HapticsKey, 1) == 1;

        public static void CycleTextScale()
        {
            float current = TextScale;
            float next = current < 1.05f ? 1.15f : current < 1.2f ? 1.3f : 1f;
            PlayerPrefs.SetFloat(TextScaleKey, next); PlayerPrefs.Save(); ApplyTextScale();
        }

        public static void ToggleReduceMotion()
        {
            PlayerPrefs.SetInt(ReduceMotionKey, ReduceMotion ? 0 : 1); PlayerPrefs.Save();
        }

        public static void ToggleHaptics()
        {
            PlayerPrefs.SetInt(HapticsKey, HapticsEnabled ? 0 : 1); PlayerPrefs.Save();
        }

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        public static void ApplyTextScale()
        {
            float scale = TextScale;
            foreach (TMP_Text text in Object.FindObjectsOfType<TMP_Text>(true))
            {
                if (text == null) continue;
                var marker = text.GetComponent<AccessibilityBaseFontSize>();
                if (marker == null)
                {
                    marker = text.gameObject.AddComponent<AccessibilityBaseFontSize>();
                    marker.BaseSize = text.fontSize;
                }
                text.fontSize = marker.BaseSize * scale;
            }
        }
    }

    public sealed class AccessibilityBaseFontSize : MonoBehaviour
    {
        public float BaseSize { get; set; }
    }
}
