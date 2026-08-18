#!/usr/bin/env python3
"""M60 balance and presentation review gate.

Static sanity catches impossible ranges and presentation regressions. A skill is only
counted balance-reviewed when an explicit REVIEWED record points at a real committed
simulation artifact and matches the current presentation contract.
"""
from __future__ import annotations

import argparse
import csv
from pathlib import Path
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
GENERATOR = ROOT / "scripts/generate-m50-skill-design.py"
M50_VALIDATOR = ROOT / "scripts/validate-m50-skill-design.py"
M58_VALIDATOR = ROOT / "scripts/validate-m58-skill-identity.py"
M59_VALIDATOR = ROOT / "scripts/validate-m59-mechanics.py"
EFFECTS = ROOT / "game-data/skills/technique-effects.csv"
AWAKENINGS = ROOT / "game-data/skills/awakening-skills.csv"
REVIEWS = ROOT / "game-data/skills/m60-balance-presentation-reviews.csv"
EXPECTED_BASE = 970
EXPECTED_AWAKENINGS = 60
RATING_FIELDS = ("pve_rating", "pvp_rating", "burst_score", "sustain_score", "control_score", "survivability_score")
PRESENTATION_FIELDS = ("animation_key", "vfx_key", "sfx_key", "cinematic_mode")


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def integer(row: dict[str, str], field: str, label: str) -> int:
    try:
        return int((row.get(field) or "").strip())
    except ValueError as exc:
        raise ValueError(f"{label}: {field} must be integer") from exc


def validate_effect_sanity() -> None:
    for row in rows(EFFECTS):
        technique = row["technique_id"].strip(); label = f"effect/{technique}/{row['effect_index']}"
        coefficient = integer(row, "coefficient_bps", label)
        chance = integer(row, "chance_bps", label)
        duration = integer(row, "duration_ms", label)
        tick = integer(row, "tick_interval_ms", label)
        if coefficient < 0 or coefficient > 50000:
            raise ValueError(f"{label}: coefficient_bps out of sanity range: {coefficient}")
        if chance < 0 or chance > 10000:
            raise ValueError(f"{label}: chance_bps outside 0..10000")
        if duration < 0 or duration > 60000:
            raise ValueError(f"{label}: duration_ms outside 0..60000")
        if tick < 0 or tick > 10000:
            raise ValueError(f"{label}: tick_interval_ms outside 0..10000")
        if tick > 0 and duration <= 0:
            raise ValueError(f"{label}: periodic effect needs positive duration")
        if tick > duration and duration > 0:
            raise ValueError(f"{label}: tick interval cannot exceed duration")
        if row["effect_type"].strip().upper() == "RAGE":
            flat = integer(row, "flat_amount", label)
            if flat < -100 or flat > 100:
                raise ValueError(f"{label}: Rage delta outside -100..100")


def real_repo_ref(value: str, label: str) -> None:
    ref = (value or "").strip()
    if not ref:
        raise ValueError(f"{label}: simulation_ref required")
    if ref.startswith("http://") or ref.startswith("https://"):
        raise ValueError(f"{label}: simulation_ref must be a committed repository path, not remote-only evidence")
    path = (ROOT / ref).resolve()
    if ROOT.resolve() not in path.parents and path != ROOT.resolve():
        raise ValueError(f"{label}: simulation_ref escapes repository")
    if not path.is_file():
        raise ValueError(f"{label}: simulation_ref does not exist: {ref}")


def main() -> int:
    parser = argparse.ArgumentParser(); parser.add_argument("--enforce", action="store_true"); args = parser.parse_args()
    try:
        subprocess.run([sys.executable, str(M58_VALIDATOR)], cwd=ROOT, check=True)
        subprocess.run([sys.executable, str(M59_VALIDATOR)], cwd=ROOT, check=True)
        validate_effect_sanity()

        with tempfile.TemporaryDirectory(prefix="m60-balance-") as tmp:
            candidate_path = Path(tmp) / "candidate.csv"
            subprocess.run([sys.executable, str(GENERATOR), "--output", str(candidate_path)], cwd=ROOT, check=True)
            subprocess.run([sys.executable, str(M50_VALIDATOR), "--candidate", str(candidate_path)], cwd=ROOT, check=True)
            candidate = rows(candidate_path)

        awakenings = rows(AWAKENINGS)
        if len(candidate) != EXPECTED_BASE or len(awakenings) != EXPECTED_AWAKENINGS:
            raise ValueError(f"catalog cardinality mismatch base={len(candidate)} awakening={len(awakenings)}")

        base_by_key: dict[tuple[str, str], dict[str, str]] = {}
        for row in candidate:
            key = (row["hero_id"].strip(), row["slot"].strip())
            base_by_key[key] = row
            for field in ("animation_key", "vfx_key", "sfx_key"):
                if not row[field].strip():
                    raise ValueError(f"{key}: missing presentation key {field}")
            if row["slot"] == "SKILL_1" and row["cinematic_mode"].strip() != "MINI_CINEMATIC":
                raise ValueError(f"{key}: Rage Skill must retain MINI_CINEMATIC")

        awakening_by_key = {(row["hero_id"].strip(), row["awakening_id"].strip()): row for row in awakenings}
        if len(awakening_by_key) != EXPECTED_AWAKENINGS:
            raise ValueError("duplicate Awakening presentation identity")

        review_rows = rows(REVIEWS)
        review_ids: set[str] = set(); reviewed_base: set[tuple[str, str]] = set(); reviewed_awakenings: set[tuple[str, str]] = set()
        for row in review_rows:
            review_id = row["review_id"].strip(); skill_type = row["skill_type"].strip().upper()
            hero_id = row["hero_id"].strip(); skill_key = row["skill_key"].strip(); label = f"{skill_type}:{hero_id}/{skill_key}"
            if not review_id or review_id in review_ids:
                raise ValueError(f"{label}: blank/duplicate review_id")
            review_ids.add(review_id)
            if row["status"].strip().upper() != "REVIEWED":
                raise ValueError(f"{label}: only REVIEWED rows belong in M60 registry")
            if not row["evidence_ref"].strip():
                raise ValueError(f"{label}: evidence_ref required")
            real_repo_ref(row["simulation_ref"], label)
            for field in RATING_FIELDS:
                value = integer(row, field, label)
                if value < 0 or value > 100:
                    raise ValueError(f"{label}: {field} must be 0..100")
            if not row["counterplay_review"].strip() or not row["presentation_review"].strip():
                raise ValueError(f"{label}: counterplay and presentation review required")

            if skill_type == "BASE":
                source = base_by_key.get((hero_id, skill_key))
                if source is None:
                    raise ValueError(f"{label}: unknown base skill")
                for field in PRESENTATION_FIELDS:
                    if row[field].strip() != source[field].strip():
                        raise ValueError(f"{label}: stale presentation approval for {field}")
                key = (hero_id, skill_key)
                if key in reviewed_base:
                    raise ValueError(f"{label}: duplicate base review")
                reviewed_base.add(key)
            elif skill_type == "AWAKENING":
                source = awakening_by_key.get((hero_id, skill_key))
                if source is None:
                    raise ValueError(f"{label}: unknown Awakening skill")
                for field in ("animation_key", "vfx_key", "sfx_key"):
                    if row[field].strip() != source[field].strip():
                        raise ValueError(f"{label}: stale Awakening presentation approval for {field}")
                key = (hero_id, skill_key)
                if key in reviewed_awakenings:
                    raise ValueError(f"{label}: duplicate Awakening review")
                reviewed_awakenings.add(key)
            else:
                raise ValueError(f"{label}: skill_type must be BASE or AWAKENING")

        print(
            f"M60_BALANCE_PRESENTATION_AUDIT base={EXPECTED_BASE} reviewed_base={len(reviewed_base)} "
            f"awakenings={EXPECTED_AWAKENINGS} reviewed_awakenings={len(reviewed_awakenings)} "
            f"review_records={len(review_rows)} static_balance_sanity=PASS presentation_keys=PASS"
        )
        if args.enforce and (len(reviewed_base) != EXPECTED_BASE or len(reviewed_awakenings) != EXPECTED_AWAKENINGS):
            print(
                f"M60_BALANCE_PRESENTATION_BLOCKED base={len(reviewed_base)}/{EXPECTED_BASE} "
                f"awakening={len(reviewed_awakenings)}/{EXPECTED_AWAKENINGS}", file=sys.stderr,
            )
            return 1
        print("M60_BALANCE_PRESENTATION_OK truthful_review_state=1")
        return 0
    except (ValueError, subprocess.CalledProcessError, KeyError) as error:
        print(f"M60_BALANCE_PRESENTATION_INVALID {error}", file=sys.stderr); return 1


if __name__ == "__main__":
    raise SystemExit(main())
