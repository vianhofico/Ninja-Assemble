using System;
using System.Collections.Generic;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

namespace NinjaAssemble.Presentation
{
    /** Runtime battle playback controls sized for touch and independent from authoritative simulation. */
    public sealed class BattlePlaybackHud : MonoBehaviour
    {
        private readonly Dictionary<int, Image> speedBackgrounds = new Dictionary<int, Image>();
        private BattleTimelinePlayer timeline;
        private RectTransform root;
        private TMP_Text pauseLabel;
        private TMP_Text stateLabel;
        private bool built;

        public void Bind(BattleTimelinePlayer value)
        {
            if (timeline == value && built) { Refresh(); return; }
            if (timeline != null)
            {
                timeline.EventPresented -= OnEventPresented;
                timeline.PlaybackCompleted -= OnPlaybackCompleted;
            }
            timeline = value;
            if (!built) Build();
            if (timeline != null)
            {
                timeline.EventPresented += OnEventPresented;
                timeline.PlaybackCompleted += OnPlaybackCompleted;
            }
            Refresh();
        }

        private void OnDestroy()
        {
            if (timeline == null) return;
            timeline.EventPresented -= OnEventPresented;
            timeline.PlaybackCompleted -= OnPlaybackCompleted;
        }

        private void Build()
        {
            built = true;
            var panel = new GameObject("PlaybackHud", typeof(RectTransform), typeof(CanvasRenderer), typeof(Image), typeof(HorizontalLayoutGroup));
            root = panel.GetComponent<RectTransform>();
            root.SetParent(transform, false);
            root.anchorMin = new Vector2(0.54f, 0.90f);
            root.anchorMax = new Vector2(0.98f, 0.985f);
            root.offsetMin = Vector2.zero;
            root.offsetMax = Vector2.zero;

            Image background = panel.GetComponent<Image>();
            background.color = new Color(0.02f, 0.025f, 0.04f, 0.86f);
            background.raycastTarget = true;

            HorizontalLayoutGroup layout = panel.GetComponent<HorizontalLayoutGroup>();
            layout.padding = new RectOffset(8, 8, 6, 6);
            layout.spacing = 6f;
            layout.childAlignment = TextAnchor.MiddleRight;
            layout.childControlHeight = true;
            layout.childControlWidth = false;
            layout.childForceExpandHeight = true;
            layout.childForceExpandWidth = false;

            stateLabel = CreateLabel(panel.transform, "State", "AUTO", 13f, 104f);
            CreateButton(panel.transform, "Pause", "PAUSE", 76f, TogglePause, out _, out pauseLabel);
            CreateSpeedButton(panel.transform, 1);
            CreateSpeedButton(panel.transform, 2);
            CreateSpeedButton(panel.transform, 4);
        }

        private void TogglePause()
        {
            if (timeline == null) return;
            timeline.SetPaused(!timeline.IsPaused);
            Refresh();
        }

        private void CreateSpeedButton(Transform parent, int speed)
        {
            Image background;
            TMP_Text ignored;
            CreateButton(parent, "Speed" + speed, speed + "x", 54f, () =>
            {
                if (timeline == null) return;
                timeline.SetPlaybackSpeed(speed);
                Refresh();
            }, out background, out ignored);
            speedBackgrounds[speed] = background;
        }

        private void OnEventPresented(BattlePresentationEvent item)
        {
            if (stateLabel == null || item == null) return;
            string seconds = (item.TimestampMs / 1000.0).ToString("0.0");
            string action = string.IsNullOrWhiteSpace(item.AbilityId) ? item.Type : item.AbilityId;
            stateLabel.text = seconds + "s  " + Compact(action, 13);
        }

        private void OnPlaybackCompleted()
        {
            if (stateLabel != null) stateLabel.text = "COMPLETE";
            Refresh();
        }

        private void Refresh()
        {
            if (timeline == null) return;
            if (pauseLabel != null) pauseLabel.text = timeline.IsPaused ? "RESUME" : "PAUSE";
            foreach (KeyValuePair<int, Image> pair in speedBackgrounds)
            {
                pair.Value.color = pair.Key == timeline.PlaybackSpeed
                    ? new Color(0.92f, 0.55f, 0.12f, 0.96f)
                    : new Color(0.13f, 0.16f, 0.23f, 0.96f);
            }
        }

        private static TMP_Text CreateLabel(Transform parent, string name, string value, float fontSize, float width)
        {
            var go = new GameObject(name, typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI), typeof(LayoutElement));
            go.transform.SetParent(parent, false);
            LayoutElement layout = go.GetComponent<LayoutElement>();
            layout.preferredWidth = width;
            layout.minWidth = width;
            TextMeshProUGUI text = go.GetComponent<TextMeshProUGUI>();
            text.text = value;
            text.fontSize = fontSize;
            text.alignment = TextAlignmentOptions.Center;
            text.color = Color.white;
            text.enableWordWrapping = false;
            text.raycastTarget = false;
            return text;
        }

        private static void CreateButton(Transform parent, string name, string label, float width, UnityEngine.Events.UnityAction action,
            out Image background, out TMP_Text text)
        {
            var buttonObject = new GameObject(name, typeof(RectTransform), typeof(CanvasRenderer), typeof(Image), typeof(Button), typeof(LayoutElement));
            buttonObject.transform.SetParent(parent, false);
            LayoutElement layout = buttonObject.GetComponent<LayoutElement>();
            layout.preferredWidth = width;
            layout.minWidth = width;
            background = buttonObject.GetComponent<Image>();
            background.color = new Color(0.13f, 0.16f, 0.23f, 0.96f);
            Button button = buttonObject.GetComponent<Button>();
            button.targetGraphic = background;
            button.onClick.AddListener(action);

            var labelObject = new GameObject("Label", typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI));
            RectTransform rect = labelObject.GetComponent<RectTransform>();
            rect.SetParent(buttonObject.transform, false);
            rect.anchorMin = Vector2.zero;
            rect.anchorMax = Vector2.one;
            rect.offsetMin = Vector2.zero;
            rect.offsetMax = Vector2.zero;
            text = labelObject.GetComponent<TextMeshProUGUI>();
            text.text = label;
            text.fontSize = 13f;
            text.fontStyle = FontStyles.Bold;
            text.alignment = TextAlignmentOptions.Center;
            text.color = Color.white;
            text.raycastTarget = false;
        }

        private static string Compact(string value, int max)
        {
            if (string.IsNullOrWhiteSpace(value)) return "AUTO";
            string clean = value.Replace('_', ' ').ToUpperInvariant();
            return clean.Length <= max ? clean : clean.Substring(0, max - 1) + "…";
        }
    }
}
