#if UNITY_EDITOR
using System;
using System.Collections.Generic;
using System.IO;
using NinjaAssemble.Bootstrap;
using NinjaAssemble.Network;
using NinjaAssemble.UI;
using TMPro;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

namespace NinjaAssemble.EditorTools
{
    public static class MobileSceneBuilder
    {
        private const string SceneDir = "Assets/Scenes/Mobile";
        private const string ConfigDir = "Assets/Resources/Config";
        private const string ApiConfigPath = ConfigDir + "/GameApiConfig.asset";

        private readonly record struct ScreenSpec(ScreenId Id, string Title, string TitleKey);

        private static readonly ScreenSpec[] Screens =
        {
            new(ScreenId.Home, "Hidden Village", "screen.home"),
            new(ScreenId.NinjaRoster, "Ninja Roster", "screen.roster"),
            new(ScreenId.HeroDetail, "Ninja Detail", "screen.heroDetail"),
            new(ScreenId.Formation, "Formation", "screen.formation"),
            new(ScreenId.Adventure, "Adventure", "screen.adventure"),
            new(ScreenId.Battle, "Battle", "screen.battle"),
            new(ScreenId.Summon, "Summon", "screen.summon"),
            new(ScreenId.Arena, "Arena", "screen.arena"),
            new(ScreenId.ShadowArena, "Shadow Arena", "screen.shadowArena"),
            new(ScreenId.Guild, "Guild", "screen.guild"),
            new(ScreenId.Shop, "Shop", "screen.shop"),
            new(ScreenId.Inventory, "Inventory", "screen.inventory"),
            new(ScreenId.Quest, "Quest", "screen.quest"),
            new(ScreenId.Events, "Events", "screen.events"),
            new(ScreenId.Mail, "Mail", "screen.mail"),
            new(ScreenId.Settings, "Settings", "screen.settings")
        };

        [MenuItem("Ninja Assemble/Mobile/Generate Complete Scene Shell")]
        public static void GenerateCompleteSceneShell()
        {
            EnsureFolder(SceneDir);
            EnsureFolder(ConfigDir);
            GameApiConfig config = EnsureApiConfig();
            BuildBootstrap(config);
            foreach (ScreenSpec screen in Screens) BuildScreen(screen);
            ConfigureBuildSettings();
            AssetDatabase.SaveAssets();
            AssetDatabase.Refresh();
            Debug.Log($"Generated {Screens.Length + 1} mobile scenes and configured build settings.");
        }

        [MenuItem("Ninja Assemble/Mobile/Validate Scene Shell")]
        public static void ValidateSceneShell()
        {
            var missing = new List<string>();
            string bootstrap = $"{SceneDir}/{MobileSceneNames.Bootstrap}.unity";
            if (!File.Exists(bootstrap)) missing.Add(bootstrap);
            foreach (ScreenSpec screen in Screens)
            {
                string path = $"{SceneDir}/{MobileSceneNames.For(screen.Id)}.unity";
                if (!File.Exists(path)) missing.Add(path);
            }
            if (missing.Count > 0) throw new InvalidOperationException("Missing generated scenes:\n" + string.Join("\n", missing));
            Debug.Log("Mobile scene shell validation passed.");
        }

        private static GameApiConfig EnsureApiConfig()
        {
            GameApiConfig config = AssetDatabase.LoadAssetAtPath<GameApiConfig>(ApiConfigPath);
            if (config != null) return config;
            config = ScriptableObject.CreateInstance<GameApiConfig>();
            AssetDatabase.CreateAsset(config, ApiConfigPath);
            return config;
        }

        private static void BuildBootstrap(GameApiConfig config)
        {
            Scene scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
            CreateEventSystem();
            CreateCamera();
            Canvas canvas = CreateCanvas("BootstrapCanvas");
            Image background = CreateImage(canvas.transform, "Background", MobileTheme.Background);
            Stretch(background.rectTransform, 0, 0, 0, 0);
            TMP_Text title = CreateText(canvas.transform, "Title", "NINJA ASSEMBLE", 48, FontStyles.Bold, TextAlignmentOptions.Center, MobileTheme.Gold);
            Anchor(title.rectTransform, 0.2f, 0.43f, 0.8f, 0.57f);
            TMP_Text subtitle = CreateText(canvas.transform, "Subtitle", "Loading Hidden Village...", 24, FontStyles.Normal, TextAlignmentOptions.Center, MobileTheme.MutedText);
            Anchor(subtitle.rectTransform, 0.2f, 0.34f, 0.8f, 0.43f);

            var bootstrapObject = new GameObject("MobileGameBootstrap");
            MobileGameBootstrap bootstrap = bootstrapObject.AddComponent<MobileGameBootstrap>();
            bootstrap.Configure(config);
            EditorSceneManager.SaveScene(scene, $"{SceneDir}/{MobileSceneNames.Bootstrap}.unity");
        }

        private static void BuildScreen(ScreenSpec spec)
        {
            Scene scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
            CreateEventSystem();
            CreateCamera();
            Canvas canvas = CreateCanvas(spec.Id + "Canvas");

            Image background = CreateImage(canvas.transform, "Background", MobileTheme.Background);
            Stretch(background.rectTransform, 0, 0, 0, 0);

            RectTransform safeArea = CreateRect(canvas.transform, "SafeArea");
            Stretch(safeArea, 0, 0, 0, 0);
            safeArea.gameObject.AddComponent<SafeAreaFitter>();

            Image header = CreateImage(safeArea, "Header", MobileTheme.Panel);
            Anchor(header.rectTransform, 0, 1, 1, 1);
            header.rectTransform.pivot = new Vector2(0.5f, 1f);
            header.rectTransform.sizeDelta = new Vector2(0, MobileTheme.HeaderHeight);
            header.rectTransform.anchoredPosition = Vector2.zero;

            TMP_Text title = CreateText(header.transform, "Title", spec.Title, 32, FontStyles.Bold, TextAlignmentOptions.MidlineLeft, MobileTheme.Text);
            Anchor(title.rectTransform, 0.025f, 0, 0.42f, 1);
            TMP_Text resources = CreateText(header.transform, "Resources", "Connecting...", 23, FontStyles.Normal, TextAlignmentOptions.MidlineRight, MobileTheme.Gold);
            Anchor(resources.rectTransform, 0.44f, 0, 0.975f, 1);

            Image bodyPanel = CreateImage(safeArea, "BodyPanel", MobileTheme.PanelAlt);
            Anchor(bodyPanel.rectTransform, 0.025f, 0.17f, 0.975f, 0.88f);
            bodyPanel.rectTransform.offsetMin = new Vector2(0, 0);
            bodyPanel.rectTransform.offsetMax = new Vector2(0, 0);

            TMP_Text body = CreateText(bodyPanel.transform, "Body", spec.Title, 28, FontStyles.Normal, TextAlignmentOptions.TopLeft, MobileTheme.Text);
            Anchor(body.rectTransform, 0.04f, 0.22f, 0.96f, 0.94f);
            body.enableWordWrapping = true;

            TMP_Text status = CreateText(bodyPanel.transform, "Status", string.Empty, 22, FontStyles.Italic, TextAlignmentOptions.BottomLeft, MobileTheme.MutedText);
            Anchor(status.rectTransform, 0.04f, 0.035f, 0.72f, 0.2f);

            Button primary = CreateButton(bodyPanel.transform, "PrimaryAction", "ACTION", MobileTheme.Accent, out TMP_Text actionLabel);
            Anchor(primary.GetComponent<RectTransform>(), 0.76f, 0.045f, 0.95f, 0.18f);

            Image navPanel = CreateImage(safeArea, "BottomNavigation", MobileTheme.Panel);
            Anchor(navPanel.rectTransform, 0, 0, 1, 0);
            navPanel.rectTransform.pivot = new Vector2(0.5f, 0f);
            navPanel.rectTransform.sizeDelta = new Vector2(0, MobileTheme.BottomNavHeight);
            navPanel.rectTransform.anchoredPosition = Vector2.zero;
            CreateBottomNavigation(navPanel.transform);

            MobileScreenRoot root = safeArea.gameObject.AddComponent<MobileScreenRoot>();
            root.Configure(spec.Id, spec.TitleKey, safeArea);
            MobileVerticalSliceController controller = safeArea.gameObject.AddComponent<MobileVerticalSliceController>();
            controller.Configure(spec.Id, resources, body, status, primary, actionLabel);

            EditorSceneManager.SaveScene(scene, $"{SceneDir}/{MobileSceneNames.For(spec.Id)}.unity");
        }

        private static void CreateBottomNavigation(Transform parent)
        {
            (ScreenId id, string label)[] items =
            {
                (ScreenId.Home, "Village"),
                (ScreenId.NinjaRoster, "Ninja"),
                (ScreenId.Formation, "Team"),
                (ScreenId.Adventure, "Adventure"),
                (ScreenId.Summon, "Summon")
            };
            for (int i = 0; i < items.Length; i++)
            {
                float min = i / (float)items.Length;
                float max = (i + 1) / (float)items.Length;
                Button button = CreateButton(parent, "Nav_" + items[i].id, items[i].label, MobileTheme.PanelAlt, out _);
                RectTransform rect = button.GetComponent<RectTransform>();
                Anchor(rect, min + 0.007f, 0.14f, max - 0.007f, 0.86f);
                SceneNavButton nav = button.gameObject.AddComponent<SceneNavButton>();
                nav.Configure(items[i].id);
            }
        }

        private static Canvas CreateCanvas(string name)
        {
            var go = new GameObject(name, typeof(Canvas), typeof(CanvasScaler), typeof(GraphicRaycaster));
            Canvas canvas = go.GetComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            CanvasScaler scaler = go.GetComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1920, 1080);
            scaler.screenMatchMode = CanvasScaler.ScreenMatchMode.MatchWidthOrHeight;
            scaler.matchWidthOrHeight = 0.5f;
            return canvas;
        }

        private static void CreateCamera()
        {
            var go = new GameObject("Main Camera", typeof(Camera), typeof(AudioListener));
            go.tag = "MainCamera";
            Camera camera = go.GetComponent<Camera>();
            camera.clearFlags = CameraClearFlags.SolidColor;
            camera.backgroundColor = MobileTheme.Background;
            camera.orthographic = true;
        }

        private static void CreateEventSystem()
        {
            new GameObject("EventSystem", typeof(EventSystem), typeof(StandaloneInputModule));
        }

        private static Image CreateImage(Transform parent, string name, Color color)
        {
            var go = new GameObject(name, typeof(RectTransform), typeof(CanvasRenderer), typeof(Image));
            go.transform.SetParent(parent, false);
            Image image = go.GetComponent<Image>();
            image.color = color;
            return image;
        }

        private static TMP_Text CreateText(Transform parent, string name, string value, float size, FontStyles style, TextAlignmentOptions alignment, Color color)
        {
            var go = new GameObject(name, typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI));
            go.transform.SetParent(parent, false);
            var text = go.GetComponent<TextMeshProUGUI>();
            text.text = value;
            text.fontSize = size;
            text.fontStyle = style;
            text.alignment = alignment;
            text.color = color;
            text.raycastTarget = false;
            return text;
        }

        private static Button CreateButton(Transform parent, string name, string label, Color color, out TMP_Text labelText)
        {
            Image image = CreateImage(parent, name, color);
            Button button = image.gameObject.AddComponent<Button>();
            button.targetGraphic = image;
            labelText = CreateText(image.transform, "Label", label, 24, FontStyles.Bold, TextAlignmentOptions.Center, MobileTheme.Text);
            Stretch(labelText.rectTransform, 0, 0, 0, 0);
            return button;
        }

        private static RectTransform CreateRect(Transform parent, string name)
        {
            var go = new GameObject(name, typeof(RectTransform));
            go.transform.SetParent(parent, false);
            return go.GetComponent<RectTransform>();
        }

        private static void Stretch(RectTransform rect, float left, float bottom, float right, float top)
        {
            rect.anchorMin = Vector2.zero;
            rect.anchorMax = Vector2.one;
            rect.offsetMin = new Vector2(left, bottom);
            rect.offsetMax = new Vector2(-right, -top);
        }

        private static void Anchor(RectTransform rect, float minX, float minY, float maxX, float maxY)
        {
            rect.anchorMin = new Vector2(minX, minY);
            rect.anchorMax = new Vector2(maxX, maxY);
            rect.offsetMin = Vector2.zero;
            rect.offsetMax = Vector2.zero;
        }

        private static void ConfigureBuildSettings()
        {
            var scenes = new List<EditorBuildSettingsScene>
            {
                new($"{SceneDir}/{MobileSceneNames.Bootstrap}.unity", true)
            };
            foreach (ScreenSpec screen in Screens)
                scenes.Add(new EditorBuildSettingsScene($"{SceneDir}/{MobileSceneNames.For(screen.Id)}.unity", true));
            EditorBuildSettings.scenes = scenes.ToArray();
        }

        private static void EnsureFolder(string path)
        {
            string[] parts = path.Split('/');
            string current = parts[0];
            for (int i = 1; i < parts.Length; i++)
            {
                string next = current + "/" + parts[i];
                if (!AssetDatabase.IsValidFolder(next)) AssetDatabase.CreateFolder(current, parts[i]);
                current = next;
            }
        }
    }
}
#endif
