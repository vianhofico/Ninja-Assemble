using TMPro;
using UnityEngine;

namespace NinjaAssemble.Localization
{
    [RequireComponent(typeof(TMP_Text))]
    public sealed class LocalizedText : MonoBehaviour
    {
        [SerializeField] private string key;
        private TMP_Text label;

        private void Awake() => label = GetComponent<TMP_Text>();

        private void OnEnable()
        {
            if (LocalizationRuntime.Current != null)
                LocalizationRuntime.Current.LanguageChanged += Refresh;
            Refresh();
        }

        private void OnDisable()
        {
            if (LocalizationRuntime.Current != null)
                LocalizationRuntime.Current.LanguageChanged -= Refresh;
        }

        public void SetKey(string value) { key = value; Refresh(); }

        private void Refresh()
        {
            if (label == null) label = GetComponent<TMP_Text>();
            if (LocalizationRuntime.Current != null && !string.IsNullOrWhiteSpace(key))
                label.text = LocalizationRuntime.Current.Get(key);
        }
    }
}
