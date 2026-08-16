using System;
using System.Threading.Tasks;
using NinjaAssemble.Bootstrap;
using NinjaAssemble.Network;
using NinjaAssemble.Playable;
using TMPro;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

namespace NinjaAssemble.UI
{
    public sealed class DevMenuController : MonoBehaviour
    {
        private GameObject panel;
        private TMP_Text status;
        private bool busy;

        private void Start()
        {
#if UNITY_EDITOR || DEVELOPMENT_BUILD
            BuildUi();
#else
            enabled = false;
#endif
        }

        private void BuildUi()
        {
            var canvasObject = new GameObject("PlaytestDevCanvas", typeof(Canvas), typeof(CanvasScaler), typeof(GraphicRaycaster));
            canvasObject.transform.SetParent(transform, false);
            Canvas canvas = canvasObject.GetComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            canvas.sortingOrder = 10000;

            CanvasScaler scaler = canvasObject.GetComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1920, 1080);
            scaler.screenMatchMode = CanvasScaler.ScreenMatchMode.MatchWidthOrHeight;
            scaler.matchWidthOrHeight = 0.5f;

            Button toggle = CreateButton(canvasObject.transform, "DevToggle", "DEV", new Color(0.66f, 0.18f, 0.12f, 0.96f));
            RectTransform toggleRect = toggle.GetComponent<RectTransform>();
            toggleRect.anchorMin = toggleRect.anchorMax = new Vector2(1f, 1f);
            toggleRect.pivot = new Vector2(1f, 1f);
            toggleRect.sizeDelta = new Vector2(130f, 64f);
            toggleRect.anchoredPosition = new Vector2(-20f, -20f);
            toggle.onClick.AddListener(TogglePanel);

            panel = CreatePanel(canvasObject.transform, "DevPanel", new Color(0.04f, 0.05f, 0.07f, 0.96f));
            RectTransform panelRect = panel.GetComponent<RectTransform>();
            panelRect.anchorMin = panelRect.anchorMax = new Vector2(1f, 1f);
            panelRect.pivot = new Vector2(1f, 1f);
            panelRect.sizeDelta = new Vector2(520f, 640f);
            panelRect.anchoredPosition = new Vector2(-20f, -98f);

            TMP_Text title = CreateText(panel.transform, "Title", "PLAYTEST DEV MENU", 30, FontStyles.Bold, TextAlignmentOptions.Center);
            SetRect(title.rectTransform, 24f, -24f, -24f, 72f);

            TMP_Text hint = CreateText(panel.transform, "Hint", "Requires server GAME_DEV_ENABLED=true", 18, FontStyles.Italic, TextAlignmentOptions.Center);
            SetRect(hint.rectTransform, 24f, -72f, -24f, 42f);
            hint.color = new Color(0.75f, 0.75f, 0.78f, 1f);

            AddActionButton("GrantPack", "+1M GOLD / +10K DIAMOND", 116f, GrantPack);
            AddActionButton("UnlockAll", "UNLOCK ALL HEROES", 188f, UnlockAllHeroes);
            AddActionButton("RefillEnergy", "REFILL ENERGY", 260f, RefillEnergy);
            AddActionButton("BattleTest", "OPEN BATTLE TEST", 332f, () => OpenScreen(ScreenId.Battle));
            AddActionButton("SummonTest", "OPEN SUMMON TEST", 404f, () => OpenScreen(ScreenId.Summon));
            AddActionButton("ResetGuest", "RESET LOCAL GUEST", 476f, ResetGuest);

            status = CreateText(panel.transform, "Status", "Ready", 18, FontStyles.Normal, TextAlignmentOptions.TopLeft);
            RectTransform statusRect = status.rectTransform;
            statusRect.anchorMin = new Vector2(0f, 1f);
            statusRect.anchorMax = new Vector2(1f, 1f);
            statusRect.pivot = new Vector2(0.5f, 1f);
            statusRect.offsetMin = new Vector2(24f, -620f);
            statusRect.offsetMax = new Vector2(-24f, -548f);
            status.enableWordWrapping = true;

            panel.SetActive(false);
        }

        private void AddActionButton(string name, string label, float top, Action action)
        {
            Button button = CreateButton(panel.transform, name, label, new Color(0.13f, 0.18f, 0.24f, 1f));
            RectTransform rect = button.GetComponent<RectTransform>();
            rect.anchorMin = new Vector2(0f, 1f);
            rect.anchorMax = new Vector2(1f, 1f);
            rect.pivot = new Vector2(0.5f, 1f);
            rect.offsetMin = new Vector2(24f, -(top + 56f));
            rect.offsetMax = new Vector2(-24f, -top);
            button.onClick.AddListener(() => action());
        }

        private void TogglePanel()
        {
            if (panel != null) panel.SetActive(!panel.activeSelf);
        }

        private async void GrantPack()
        {
            await RunAsync(async store =>
            {
                DevStateDto result = await store.GrantDevStandardPackAsync();
                return $"Granted dev pack • Gold {result.gold:N0} • Diamond {result.diamond:N0}";
            });
        }

        private async void UnlockAllHeroes()
        {
            await RunAsync(async store =>
            {
                DevRosterResultDto result = await store.UnlockAllHeroesDevAsync();
                return $"Roster ready • +{result.newlyGranted} new • {result.ownedHeroes} owned";
            });
        }

        private async void RefillEnergy()
        {
            await RunAsync(async store =>
            {
                DevStateDto result = await store.RefillEnergyDevAsync();
                return $"Energy refilled • {result.energy}";
            });
        }

        private async Task RunAsync(Func<PlayableGameStore, Task<string>> action)
        {
            if (busy) return;
            if (!MobileGameBootstrap.IsReady)
            {
                SetStatus("Game is still connecting to the server.");
                return;
            }

            busy = true;
            SetStatus("Working...");
            try
            {
                string message = await action(MobileGameBootstrap.Store);
                SetStatus(message);
                RefreshCurrentScreen();
            }
            catch (Exception exception)
            {
                Debug.LogException(exception);
                SetStatus(exception.Message);
            }
            finally
            {
                busy = false;
            }
        }

        private static void OpenScreen(ScreenId id)
        {
            if (!MobileGameBootstrap.IsReady) return;
            SceneManager.LoadScene(MobileSceneNames.For(id));
        }

        private void ResetGuest()
        {
            if (busy) return;
            MobileGameBootstrap.ResetLocalGuestAndRestart();
        }

        private static void RefreshCurrentScreen()
        {
            MobileVerticalSliceController[] controllers = FindObjectsByType<MobileVerticalSliceController>(FindObjectsInactive.Include, FindObjectsSortMode.None);
            foreach (MobileVerticalSliceController controller in controllers) controller.RefreshView();
        }

        private void SetStatus(string value)
        {
            if (status != null) status.text = value ?? string.Empty;
        }

        private static GameObject CreatePanel(Transform parent, string name, Color color)
        {
            var go = new GameObject(name, typeof(RectTransform), typeof(CanvasRenderer), typeof(Image));
            go.transform.SetParent(parent, false);
            go.GetComponent<Image>().color = color;
            return go;
        }

        private static Button CreateButton(Transform parent, string name, string label, Color color)
        {
            GameObject go = CreatePanel(parent, name, color);
            Button button = go.AddComponent<Button>();
            button.targetGraphic = go.GetComponent<Image>();
            TMP_Text text = CreateText(go.transform, "Label", label, 22, FontStyles.Bold, TextAlignmentOptions.Center);
            RectTransform rect = text.rectTransform;
            rect.anchorMin = Vector2.zero;
            rect.anchorMax = Vector2.one;
            rect.offsetMin = Vector2.zero;
            rect.offsetMax = Vector2.zero;
            return button;
        }

        private static TMP_Text CreateText(Transform parent, string name, string value, float size, FontStyles style, TextAlignmentOptions alignment)
        {
            var go = new GameObject(name, typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI));
            go.transform.SetParent(parent, false);
            var text = go.GetComponent<TextMeshProUGUI>();
            text.text = value;
            text.fontSize = size;
            text.fontStyle = style;
            text.alignment = alignment;
            text.color = Color.white;
            text.raycastTarget = false;
            return text;
        }

        private static void SetRect(RectTransform rect, float left, float top, float right, float height)
        {
            rect.anchorMin = new Vector2(0f, 1f);
            rect.anchorMax = new Vector2(1f, 1f);
            rect.pivot = new Vector2(0.5f, 1f);
            rect.offsetMin = new Vector2(left, -(top + height));
            rect.offsetMax = new Vector2(right, -top);
        }
    }
}
