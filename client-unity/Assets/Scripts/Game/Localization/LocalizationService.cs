using System;
using System.Collections.Generic;
using UnityEngine;

namespace NinjaAssemble.Localization
{
    public sealed class LocalizationService
    {
        private readonly Dictionary<string, Entry> entries = new();
        public GameLanguage Language { get; private set; } = GameLanguage.English;
        public event Action LanguageChanged;

        public LocalizationService(TextAsset csv)
        {
            if (csv == null) throw new ArgumentNullException(nameof(csv));
            Parse(csv.text);
        }

        public void SetLanguage(GameLanguage language)
        {
            if (Language == language) return;
            Language = language;
            LanguageChanged?.Invoke();
        }

        public string Get(string key)
        {
            if (!entries.TryGetValue(key, out var entry)) return $"[{key}]";
            return Language == GameLanguage.Vietnamese ? entry.Vi : entry.En;
        }

        private void Parse(string csv)
        {
            var lines = csv.Replace("\r\n", "\n").Split('\n');
            for (var i = 1; i < lines.Length; i++)
            {
                if (string.IsNullOrWhiteSpace(lines[i])) continue;
                var cells = ParseCsvLine(lines[i]);
                if (cells.Count < 3) continue;
                entries[cells[0]] = new Entry(cells[1], cells[2]);
            }
        }

        private static List<string> ParseCsvLine(string line)
        {
            var values = new List<string>();
            var current = new System.Text.StringBuilder();
            var quoted = false;
            for (var i = 0; i < line.Length; i++)
            {
                var c = line[i];
                if (c == '"')
                {
                    if (quoted && i + 1 < line.Length && line[i + 1] == '"') { current.Append('"'); i++; }
                    else quoted = !quoted;
                }
                else if (c == ',' && !quoted) { values.Add(current.ToString()); current.Clear(); }
                else current.Append(c);
            }
            values.Add(current.ToString());
            return values;
        }

        private readonly struct Entry
        {
            public Entry(string en, string vi)
            {
                En = en;
                Vi = vi;
            }

            public string En { get; }
            public string Vi { get; }
        }
    }
}
