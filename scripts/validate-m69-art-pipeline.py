#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
CONTRACT = ROOT / "art/art-production-contract.json"
BUDGETS = ROOT / "art/mobile-asset-budgets.json"
MANIFEST = ROOT / "art/manifests/hero-art-manifest.csv"
COMPONENTS = ROOT / "art/manifests/hero-art-component-status.csv"
SCHEMA = ROOT / "art/hero-art-package.schema.json"

REQUIRED_COMPONENTS = {
    "portrait", "icon", "chibiPrefab", "animationSet", "vfxSet", "sfxSet",
    "regressionCapture", "reviewEvidence"
}
READY_FIELDS = (
    "portrait_status", "icon_status", "chibi_prefab_status", "animation_status",
    "vfx_status", "sfx_status", "regression_capture_status", "review_status"
)


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def main() -> int:
    errors: list[str] = []
    for path in (CONTRACT, BUDGETS, MANIFEST, COMPONENTS, SCHEMA):
        if not path.exists():
            errors.append(f"missing required M69 input: {path.relative_to(ROOT)}")
    if errors:
        return fail(errors)

    contract = json.loads(CONTRACT.read_text(encoding="utf-8"))
    budgets = json.loads(BUDGETS.read_text(encoding="utf-8"))
    schema = json.loads(SCHEMA.read_text(encoding="utf-8"))

    if contract.get("contractVersion") != "m69-v1":
        errors.append("contractVersion must be m69-v1")
    if set(contract.get("requiredComponents", [])) != REQUIRED_COMPONENTS:
        errors.append("requiredComponents must exactly freeze the eight production gates")
    runtime = contract.get("runtimeRequirements", {})
    if runtime.get("addressableLoad") is not True or runtime.get("battleReplayRender") is not True:
        errors.append("READY packages must require Addressables load and real battle replay render")
    if runtime.get("fallbackAllowedForReady") is not False:
        errors.append("READY packages cannot permit fallback assets")

    texture = budgets.get("texture", {})
    audio = budgets.get("audio", {})
    package = budgets.get("package", {})
    if texture.get("compression") != "ASTC":
        errors.append("Android production texture compression must be frozen to ASTC")
    if not isinstance(package.get("maxInstalledBytesPerHeroVariant"), int) or package["maxInstalledBytesPerHeroVariant"] <= 0:
        errors.append("per-variant installed byte budget must be a positive integer")
    if audio.get("sampleRateHz") not in (22050, 44100, 48000):
        errors.append("audio sample rate must be a supported mobile production rate")
    if package.get("releaseRequiresRegressionCapture") is not True or package.get("releaseRequiresHumanReviewEvidence") is not True:
        errors.append("release package must require capture and human review evidence")

    required_schema_paths = set(schema.get("properties", {}).get("paths", {}).get("required", []))
    if required_schema_paths != (REQUIRED_COMPONENTS - {"reviewEvidence"}):
        errors.append("hero-art-package schema paths do not match M69 contract")
    if "reviewEvidence" not in schema.get("required", []):
        errors.append("hero-art-package schema must require reviewEvidence")

    representative = contract.get("representativePackage", {})
    key = (representative.get("characterId", ""), representative.get("variant", ""))
    manifest_rows = {(r["character_id"], r["variant"]): r for r in read_csv(MANIFEST)}
    component_rows = {(r["character_id"], r["variant"]): r for r in read_csv(COMPONENTS)}
    if key not in manifest_rows or key not in component_rows:
        errors.append(f"representative package is not tracked in both manifests: {key}")
    else:
        row = component_rows[key]
        package_ready = all(row.get(field, "").strip().upper() == "READY" for field in READY_FIELDS)
        descriptor = ROOT / contract["repositoryLayout"]["packageDescriptor"].format(
            characterId=representative["characterId"], variantSlug=representative["variantSlug"])
        if package_ready and not descriptor.exists():
            errors.append("representative package is marked READY without a repository-backed descriptor")
        if manifest_rows[key].get("status", "").strip().upper() == "READY" and not package_ready:
            errors.append("representative manifest is READY before every component gate is READY")

    if errors:
        return fail(errors)
    package_ready = key in component_rows and all(component_rows[key].get(field, "").strip().upper() == "READY" for field in READY_FIELDS)
    print(f"M69_ART_PIPELINE_OK representative={key[0]}:{key[1]} ready={str(package_ready).lower()} contract=m69-v1")
    return 0


def fail(errors: list[str]) -> int:
    print("M69 ART PIPELINE VALIDATION FAILED", file=sys.stderr)
    for error in errors:
        print(" -", error, file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
