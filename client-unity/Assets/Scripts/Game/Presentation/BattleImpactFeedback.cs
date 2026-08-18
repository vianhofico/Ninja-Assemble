using System.Collections;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

namespace NinjaAssemble.Presentation
{
    /** Asset-independent battle feedback that consumes replay events without mutating simulation state. */
    public sealed class BattleImpactFeedback : MonoBehaviour
    {
        private BattleTimelinePlayer timeline;
        private RectTransform root;
        private Coroutine cinematic;
        private Coroutine shake;

        public void Bind(BattleTimelinePlayer value)
        {
            if (timeline == value) return;
            if (timeline != null) timeline.EventPresented -= OnEventPresented;
            timeline = value;
            root = transform as RectTransform;
            if (timeline != null) timeline.EventPresented += OnEventPresented;
        }

        private void OnDestroy()
        {
            if (timeline != null) timeline.EventPresented -= OnEventPresented;
        }

        private void OnEventPresented(BattlePresentationEvent item)
        {
            if (timeline == null || item == null) return;
            BattleActorView actor;
            switch (item.Type)
            {
                case "DAMAGE":
                    if (timeline.TryGetActor(item.TargetId, out actor)) StartCoroutine(ActorFlash(actor, item.Critical ? 0.20f : 0.11f));
                    if (item.Critical) StartShake(7f, 0.14f);
                    break;
                case "STATUS_TICK":
                    if (timeline.TryGetActor(item.TargetId, out actor)) StartCoroutine(ActorFlash(actor, 0.08f));
                    break;
                case "HEAL":
                    if (timeline.TryGetActor(item.TargetId, out actor)) StartCoroutine(PopLabel(actor, "+" + item.Amount, new Color(0.35f, 1f, 0.50f, 1f), 20f));
                    break;
                case "SHIELD":
                    if (timeline.TryGetActor(item.TargetId, out actor)) StartCoroutine(PopLabel(actor, "SHIELD", new Color(0.35f, 0.78f, 1f, 1f), 18f));
                    break;
                case "STATUS_APPLIED":
                    if (timeline.TryGetActor(item.TargetId, out actor) && !string.IsNullOrWhiteSpace(item.StatusId))
                        StartCoroutine(PopLabel(actor, item.StatusId, StatusColor(item.StatusId), 17f));
                    break;
                case "RAGE_FULL":
                case "RAGE_SKILL_READY":
                    if (timeline.TryGetActor(item.ActorId, out actor)) StartCoroutine(RageReadyPulse(actor));
                    break;
                case "RAGE_SKILL_CAST_START":
                    if (cinematic != null) StopCoroutine(cinematic);
                    cinematic = StartCoroutine(RageCinematic(item));
                    StartShake(10f, 0.20f);
                    break;
                case "KO":
                    if (timeline.TryGetActor(item.TargetId, out actor)) StartCoroutine(PopLabel(actor, "KO", new Color(1f, 0.28f, 0.20f, 1f), 28f));
                    StartShake(5f, 0.10f);
                    break;
            }
        }

        private IEnumerator ActorFlash(BattleActorView actor, float duration)
        {
            if (actor == null) yield break;
            RectTransform actorRect = actor.transform as RectTransform;
            if (actorRect == null) yield break;
            var overlay = new GameObject("ImpactFlash", typeof(RectTransform), typeof(CanvasRenderer), typeof(Image));
            RectTransform rect = overlay.GetComponent<RectTransform>();
            rect.SetParent(actorRect, false);
            rect.anchorMin = Vector2.zero;
            rect.anchorMax = Vector2.one;
            rect.offsetMin = Vector2.zero;
            rect.offsetMax = Vector2.zero;
            Image image = overlay.GetComponent<Image>();
            image.color = new Color(1f, 1f, 1f, 0.32f);
            image.raycastTarget = false;
            float elapsed = 0f;
            while (elapsed < duration)
            {
                elapsed += PresentationDelta();
                float alpha = 0.32f * (1f - Mathf.Clamp01(elapsed / duration));
                image.color = new Color(1f, 1f, 1f, alpha);
                yield return null;
            }
            Destroy(overlay);
        }

        private IEnumerator PopLabel(BattleActorView actor, string value, Color color, float fontSize)
        {
            if (actor == null || root == null) yield break;
            var go = new GameObject("BattleFeedback", typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI));
            RectTransform rect = go.GetComponent<RectTransform>();
            rect.SetParent(root, false);
            rect.sizeDelta = new Vector2(180f, 54f);
            Vector2 start = LocalPoint(actor.HitAnchor.position) + new Vector2(0f, 40f);
            rect.anchoredPosition = start;
            TextMeshProUGUI text = go.GetComponent<TextMeshProUGUI>();
            text.text = value ?? string.Empty;
            text.fontSize = fontSize;
            text.fontStyle = FontStyles.Bold;
            text.alignment = TextAlignmentOptions.Center;
            text.color = color;
            text.raycastTarget = false;
            text.enableWordWrapping = false;
            const float duration = 0.55f;
            float elapsed = 0f;
            while (elapsed < duration)
            {
                elapsed += PresentationDelta();
                float t = Mathf.Clamp01(elapsed / duration);
                rect.anchoredPosition = start + new Vector2(0f, 42f * t);
                text.color = new Color(color.r, color.g, color.b, 1f - t);
                yield return null;
            }
            Destroy(go);
        }

        private IEnumerator RageReadyPulse(BattleActorView actor)
        {
            if (actor == null) yield break;
            yield return PopLabel(actor, "RAGE READY", new Color(1f, 0.68f, 0.10f, 1f), 20f);
            Transform target = actor.transform;
            Vector3 original = target.localScale;
            const float duration = 0.26f;
            float elapsed = 0f;
            while (elapsed < duration)
            {
                elapsed += PresentationDelta();
                float t = Mathf.Clamp01(elapsed / duration);
                float pulse = 1f + Mathf.Sin(t * Mathf.PI) * 0.08f;
                target.localScale = original * pulse;
                yield return null;
            }
            target.localScale = original;
        }

        private IEnumerator RageCinematic(BattlePresentationEvent item)
        {
            if (root == null) yield break;
            var overlay = new GameObject("RageCinematic", typeof(RectTransform), typeof(CanvasRenderer), typeof(Image));
            RectTransform rect = overlay.GetComponent<RectTransform>();
            rect.SetParent(root, false);
            rect.anchorMin = Vector2.zero;
            rect.anchorMax = Vector2.one;
            rect.offsetMin = Vector2.zero;
            rect.offsetMax = Vector2.zero;
            rect.SetAsLastSibling();
            Image image = overlay.GetComponent<Image>();
            image.color = new Color(0.02f, 0.015f, 0.03f, 0f);
            image.raycastTarget = false;

            CreateBar(rect, "TopBar", new Vector2(0f, 0.82f), new Vector2(1f, 1f));
            CreateBar(rect, "BottomBar", new Vector2(0f, 0f), new Vector2(1f, 0.18f));
            TMP_Text title = CreateOverlayText(rect, "RAGE SKILL", 38f, new Vector2(0.08f, 0.51f), new Vector2(0.92f, 0.72f));
            TMP_Text subtitle = CreateOverlayText(rect,
                string.IsNullOrWhiteSpace(item.AbilityId) ? "SIGNATURE TECHNIQUE" : item.AbilityId.Replace('-', ' ').ToUpperInvariant(),
                20f, new Vector2(0.08f, 0.40f), new Vector2(0.92f, 0.53f));
            subtitle.color = new Color(1f, 0.75f, 0.22f, 1f);

            const float inTime = 0.10f;
            const float holdTime = 0.26f;
            const float outTime = 0.14f;
            float elapsed = 0f;
            while (elapsed < inTime)
            {
                elapsed += PresentationDelta();
                float t = Mathf.Clamp01(elapsed / inTime);
                image.color = new Color(0.02f, 0.015f, 0.03f, 0.55f * t);
                title.rectTransform.localScale = Vector3.one * Mathf.Lerp(1.18f, 1f, t);
                yield return null;
            }
            elapsed = 0f;
            while (elapsed < holdTime) { elapsed += PresentationDelta(); yield return null; }
            elapsed = 0f;
            while (elapsed < outTime)
            {
                elapsed += PresentationDelta();
                float t = Mathf.Clamp01(elapsed / outTime);
                image.color = new Color(0.02f, 0.015f, 0.03f, 0.55f * (1f - t));
                title.color = new Color(1f, 1f, 1f, 1f - t);
                subtitle.color = new Color(1f, 0.75f, 0.22f, 1f - t);
                yield return null;
            }
            Destroy(overlay);
            cinematic = null;
        }

        private void StartShake(float magnitude, float duration)
        {
            if (root == null) return;
            if (shake != null) StopCoroutine(shake);
            shake = StartCoroutine(Shake(magnitude, duration));
        }

        private IEnumerator Shake(float magnitude, float duration)
        {
            Vector2 origin = root.anchoredPosition;
            float elapsed = 0f;
            while (elapsed < duration)
            {
                elapsed += PresentationDelta();
                float falloff = 1f - Mathf.Clamp01(elapsed / duration);
                root.anchoredPosition = origin + Random.insideUnitCircle * magnitude * falloff;
                yield return null;
            }
            root.anchoredPosition = origin;
            shake = null;
        }

        private float PresentationDelta()
        {
            if (timeline == null || timeline.IsPaused) return 0f;
            return Time.unscaledDeltaTime * Mathf.Max(1, timeline.PlaybackSpeed);
        }

        private Vector2 LocalPoint(Vector3 world)
        {
            Vector2 local;
            RectTransformUtility.ScreenPointToLocalPointInRectangle(root,
                RectTransformUtility.WorldToScreenPoint(null, world), null, out local);
            return local;
        }

        private static void CreateBar(RectTransform parent, string name, Vector2 min, Vector2 max)
        {
            var go = new GameObject(name, typeof(RectTransform), typeof(CanvasRenderer), typeof(Image));
            RectTransform rect = go.GetComponent<RectTransform>();
            rect.SetParent(parent, false);
            rect.anchorMin = min;
            rect.anchorMax = max;
            rect.offsetMin = Vector2.zero;
            rect.offsetMax = Vector2.zero;
            Image image = go.GetComponent<Image>();
            image.color = new Color(0f, 0f, 0f, 0.92f);
            image.raycastTarget = false;
        }

        private static TMP_Text CreateOverlayText(RectTransform parent, string value, float size, Vector2 min, Vector2 max)
        {
            var go = new GameObject("Text", typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI));
            RectTransform rect = go.GetComponent<RectTransform>();
            rect.SetParent(parent, false);
            rect.anchorMin = min;
            rect.anchorMax = max;
            rect.offsetMin = Vector2.zero;
            rect.offsetMax = Vector2.zero;
            TextMeshProUGUI text = go.GetComponent<TextMeshProUGUI>();
            text.text = value ?? string.Empty;
            text.fontSize = size;
            text.fontStyle = FontStyles.Bold;
            text.alignment = TextAlignmentOptions.Center;
            text.color = Color.white;
            text.raycastTarget = false;
            text.enableWordWrapping = false;
            return text;
        }

        private static Color StatusColor(string statusId)
        {
            string id = (statusId ?? string.Empty).ToUpperInvariant();
            if (id.Contains("UP")) return new Color(0.45f, 0.95f, 0.55f, 1f);
            if (id == "STUN" || id == "SILENCE") return new Color(0.95f, 0.72f, 0.20f, 1f);
            return new Color(1f, 0.42f, 0.30f, 1f);
        }
    }
}
