#!/usr/bin/env python3
import argparse
import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FLAGSHIP = {
    "naruto-uzumaki", "sasuke-uchiha", "sakura-haruno", "kakashi-hatake", "itachi-uchiha",
    "madara-uchiha", "obito-uchiha", "gaara", "minato-namikaze", "hashirama-senju", "nagato", "might-guy"
}
COMPONENT_FIELDS = (
    "portrait_status", "icon_status", "chibi_prefab_status", "animation_status",
    "vfx_status", "sfx_status", "regression_capture_status", "review_status"
)


def read(path):
    with path.open(encoding="utf-8-sig", newline="") as fh:
        return list(csv.DictReader(fh))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="art/manifests/art-production-queue.generated.csv")
    args = parser.parse_args()
    variants = []
    seen = set()
    for path in sorted(ROOT.glob("game-data/reference/variant-census*.csv")):
        source_priority = 1 if path.name == "variant-census.csv" else 2
        for row in read(path):
            key = (row["character_id"].strip(), row["variant"].strip())
            if key in seen:
                continue
            seen.add(key)
            variants.append((source_priority, row))

    manifest = {(r["character_id"], r["variant"]): r for r in read(ROOT / "art/manifests/hero-art-manifest.csv")}
    components = {(r["character_id"], r["variant"]): r for r in read(ROOT / "art/manifests/hero-art-component-status.csv")}
    queue = []
    for source_priority, row in variants:
        key = (row["character_id"].strip(), row["variant"].strip())
        art = manifest.get(key)
        component = components.get(key, {})
        priority = 0 if key[0] in FLAGSHIP else source_priority
        status = art["status"] if art else "TODO"
        ready_components = sum(component.get(field, "TODO").strip().upper() == "READY" for field in COMPONENT_FIELDS)
        completion_percent = round(ready_components * 100 / len(COMPONENT_FIELDS))
        queue.append((priority, status == "READY", -completion_percent, key[0], key[1], status, ready_components, completion_percent))
    queue.sort(key=lambda x: (x[1], x[0], x[2], x[3], x[4]))

    output = ROOT / args.output
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.writer(fh)
        writer.writerow(["priority", "character_id", "variant", "art_status", "components_ready", "components_total", "completion_percent", "required_package"])
        for priority, _, _, character, variant, status, ready_components, completion_percent in queue:
            writer.writerow([priority, character, variant, status, ready_components, len(COMPONENT_FIELDS), completion_percent, "portrait|icon|chibi_prefab|animation|vfx|sfx|regression_capture|review"])
    print(f"wrote {len(queue)} variants to {output.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
