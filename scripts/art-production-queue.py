#!/usr/bin/env python3
import argparse
import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FLAGSHIP = {
    "naruto-uzumaki", "sasuke-uchiha", "sakura-haruno", "kakashi-hatake", "itachi-uchiha",
    "madara-uchiha", "obito-uchiha", "gaara", "minato-namikaze", "hashirama-senju", "nagato", "might-guy"
}


def read(path):
    with path.open(encoding="utf-8-sig", newline="") as fh: return list(csv.DictReader(fh))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="art/manifests/art-production-queue.generated.csv")
    args = parser.parse_args()
    variants = []
    for path in sorted(ROOT.glob("game-data/reference/variant-census*.csv")):
        source_priority = 1 if path.name == "variant-census.csv" else 2
        for row in read(path): variants.append((source_priority, row))
    existing = {(r["character_id"], r["variant"]): r for r in read(ROOT / "art/manifests/hero-art-manifest.csv")}
    queue = []
    for source_priority, row in variants:
        key = (row["character_id"], row["variant"])
        art = existing.get(key)
        priority = 0 if key[0] in FLAGSHIP else source_priority
        status = art["status"] if art else "TODO"
        queue.append((priority, status == "READY", key[0], key[1], status))
    queue.sort(key=lambda x: (x[1], x[0], x[2], x[3]))
    output = ROOT / args.output
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.writer(fh)
        writer.writerow(["priority", "character_id", "variant", "art_status", "required_package"])
        for priority, _, character, variant, status in queue:
            writer.writerow([priority, character, variant, status, "portrait|icon|chibi_prefab|animation|vfx|sfx|regression_capture"])
    print(f"wrote {len(queue)} variants to {output.relative_to(ROOT)}")


if __name__ == "__main__": main()
