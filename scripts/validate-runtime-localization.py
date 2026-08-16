#!/usr/bin/env python3
"""Validate EN/VI catalog packaging and runtime localization wiring."""
from pathlib import Path
import csv

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"RUNTIME_LOCALIZATION_INVALID {path} missing={missing}")


def main() -> int:
    source = (ROOT / "game-data/localization/strings.csv").read_text(encoding="utf-8").replace("\r\n", "\n")
    packaged = (ROOT / "client-unity/Assets/Resources/Localization/strings.csv").read_text(encoding="utf-8").replace("\r\n", "\n")
    if source != packaged:
        raise SystemExit("RUNTIME_LOCALIZATION_INVALID Unity Resources strings.csv is out of sync with game-data catalog")

    rows = list(csv.DictReader(source.splitlines()))
    if len(rows) < 40 or any(not row["key"] or not row["en"] or not row["vi"] for row in rows):
        raise SystemExit(f"RUNTIME_LOCALIZATION_INVALID catalog rows={len(rows)}")
    keys = {row["key"] for row in rows}
    required_keys = {"menu.home", "menu.formation", "menu.adventure", "menu.summon", "menu.arena",
                     "menu.shadow_arena", "menu.guild", "menu.shop", "menu.inventory", "menu.quest",
                     "menu.events", "menu.settings", "hero.evolve", "hero.advance", "battle.victory",
                     "battle.defeat", "currency.gold", "currency.diamond"}
    if not required_keys.issubset(keys):
        raise SystemExit(f"RUNTIME_LOCALIZATION_INVALID missing keys={sorted(required_keys - keys)}")

    require("client-unity/Assets/Scripts/Game/Localization/BilingualRuntimeLocalizationBridge.cs",
            "RuntimeInitializeOnLoadMethod", 'Resources.Load<TextAsset>("Localization/strings")',
            'PlayerPrefs.GetString(LanguageKey, "VI")', "ToVietnamese", "ToEnglish", "FindObjectsOfType<TMP_Text>",
            "FRAME ADVANCE", "ĐỘT PHÁ KHUNG", "EVOLVE", "TIẾN HÓA", "FIGHT", "CHIẾN ĐẤU")
    require("client-unity/Assets/Scripts/Game/Settings/SettingsPlayableBridge.cs",
            'LanguageKey = "na.language"', "ToggleLanguage", "PlayerPrefs.Save")
    print(f"RUNTIME_LOCALIZATION_OK rows={len(rows)} languages=EN,VI dynamic_text=yes reverse_restore=yes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
