using System;
using System.Linq;
using System.Reflection;
using NinjaAssemble.UI;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

namespace NinjaAssemble.Settings
{
    public sealed class SettingsPlayableBridge : MonoBehaviour
    {
        private const string LanguageKey = "na.language";
        private const string SoundKey = "na.sound";
        private const string QualityKey = "na.quality";
        private MobileVerticalSliceController controller;
        private TMP_Text bodyText;
        private TMP_Text statusText;
        private Button primaryAction;
        private TMP_Text primaryActionLabel;
        private Button soundAction;
        private Button graphicsAction;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        private static void Install()
        {
            ApplySavedPreferences();
            var root = new GameObject("SettingsPlayableBridgeRuntime");
            DontDestroyOnLoad(root);
            root.AddComponent<SettingsPlayableBridge>();
        }

        private void Update()
        {
            if (controller != null) return;
            controller = FindController(ScreenId.Settings);
            if (controller == null) return;
            bodyText = Field<TMP_Text>(controller, "bodyText");
            statusText = Field<TMP_Text>(controller, "statusText");
            primaryAction = Field<Button>(controller, "primaryAction");
            primaryActionLabel = Field<TMP_Text>(controller, "primaryActionLabel");
            primaryAction.onClick.RemoveAllListeners();
            primaryAction.onClick.AddListener(ToggleLanguage);
            soundAction = CloneAction(primaryAction, "SettingsSoundAction", 1, ToggleSound);
            graphicsAction = CloneAction(primaryAction, "SettingsGraphicsAction", 2, CycleGraphics);
            Render();
        }

        private void ToggleLanguage()
        {
            string next = PlayerPrefs.GetString(LanguageKey, "VI") == "VI" ? "EN" : "VI";
            PlayerPrefs.SetString(LanguageKey, next);
            SaveAndApply();
            SetStatus(next == "VI" ? "Đã chuyển sang Tiếng Việt." : "Language changed to English.");
            Render();
        }

        private void ToggleSound()
        {
            int enabled = PlayerPrefs.GetInt(SoundKey, 1) == 1 ? 0 : 1;
            PlayerPrefs.SetInt(SoundKey, enabled);
            SaveAndApply();
            SetStatus(enabled == 1 ? Text("Âm thanh đã bật.", "Sound enabled.") : Text("Âm thanh đã tắt.", "Sound muted."));
            Render();
        }

        private void CycleGraphics()
        {
            int count = Math.Max(1, QualitySettings.names.Length);
            int next = (PlayerPrefs.GetInt(QualityKey, Mathf.Clamp(QualitySettings.GetQualityLevel(), 0, count - 1)) + 1) % count;
            PlayerPrefs.SetInt(QualityKey, next);
            SaveAndApply();
            SetStatus(Text("Đã đổi chất lượng đồ họa.", "Graphics quality changed."));
            Render();
        }

        private void Render()
        {
            bool vi = IsVietnamese();
            bool sound = PlayerPrefs.GetInt(SoundKey, 1) == 1;
            int quality = Mathf.Clamp(PlayerPrefs.GetInt(QualityKey, QualitySettings.GetQualityLevel()), 0, Math.Max(0, QualitySettings.names.Length - 1));
            string qualityName = QualitySettings.names.Length == 0 ? "Default" : QualitySettings.names[quality];
            if (bodyText != null)
                bodyText.text = vi
                    ? $"Cài đặt\n\nNgôn ngữ: Tiếng Việt\nÂm thanh: {(sound ? "Bật" : "Tắt")}\nĐồ họa: {qualityName}\n\nCác cài đặt được lưu trên thiết bị."
                    : $"Settings\n\nLanguage: English\nSound: {(sound ? "On" : "Off")}\nGraphics: {qualityName}\n\nPreferences are saved on this device.";
            if (primaryActionLabel != null) primaryActionLabel.text = vi ? "ENGLISH" : "TIẾNG VIỆT";
            SetButtonLabel(soundAction, sound ? Text("TẮT ÂM", "MUTE") : Text("BẬT ÂM", "SOUND ON"));
            SetButtonLabel(graphicsAction, Text("ĐỔI ĐỒ HỌA", "NEXT GRAPHICS"));
        }

        private static void ApplySavedPreferences()
        {
            AudioListener.volume = PlayerPrefs.GetInt(SoundKey, 1) == 1 ? 1f : 0f;
            int count = QualitySettings.names.Length;
            if (count > 0)
            {
                int quality = Mathf.Clamp(PlayerPrefs.GetInt(QualityKey, QualitySettings.GetQualityLevel()), 0, count - 1);
                QualitySettings.SetQualityLevel(quality, true);
            }
        }

        private static void SaveAndApply() { PlayerPrefs.Save(); ApplySavedPreferences(); }
        private static bool IsVietnamese() => PlayerPrefs.GetString(LanguageKey, "VI") == "VI";
        private static string Text(string vi, string en) => IsVietnamese() ? vi : en;
        private void SetStatus(string value) { if (statusText != null) statusText.text = value ?? string.Empty; }

        private static Button CloneAction(Button source, string name, int row, UnityEngine.Events.UnityAction action)
        {
            if (source == null) return null;
            GameObject clone = Instantiate(source.gameObject, source.transform.parent);
            clone.name = name;
            Button button = clone.GetComponent<Button>();
            button.onClick.RemoveAllListeners();
            button.onClick.AddListener(action);
            RectTransform rect = clone.GetComponent<RectTransform>();
            RectTransform sourceRect = source.GetComponent<RectTransform>();
            if (rect != null && sourceRect != null) rect.anchoredPosition = sourceRect.anchoredPosition + new Vector2(0f, -68f * row);
            return button;
        }

        private static void SetButtonLabel(Button button, string label)
        {
            TMP_Text text = button?.GetComponentInChildren<TMP_Text>();
            if (text != null) text.text = label;
        }

        private static MobileVerticalSliceController FindController(ScreenId id)
        {
            FieldInfo screenField = typeof(MobileVerticalSliceController).GetField("screenId", BindingFlags.Instance | BindingFlags.NonPublic);
            return FindObjectsOfType<MobileVerticalSliceController>(true)
                .FirstOrDefault(candidate => screenField?.GetValue(candidate) is ScreenId value && value == id);
        }

        private static T Field<T>(object target, string name) where T : class =>
            target?.GetType().GetField(name, BindingFlags.Instance | BindingFlags.NonPublic)?.GetValue(target) as T;
    }
}
