#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
CONTRACT = ROOT / "art/pipeline/art-package-contract-v2.json"
BUDGETS = ROOT / "art/pipeline/performance-budgets.csv"
ADDRESSES = ROOT / "art/pipeline/addressables-layout.csv"
REVIEW_SCHEMA = ROOT / "art/pipeline/review-evidence.schema.json"
PREFAB_HIERARCHY = ROOT / "art/pipeline/PREFAB-HIERARCHY.md"
MANIFEST = ROOT / "art/manifests/hero-art-manifest.csv"
COMPONENTS = ROOT / "art/manifests/hero-art-component-status.csv"
PACKAGE_SCHEMA = ROOT / "art/hero-art-package.schema.json"
REPRESENTATIVE = ("naruto-uzumaki", "Sage Mode", "sage-mode")
REQUIRED_COMPONENTS = {"portrait","icon","chibiPrefab","animationSet","vfxSet","sfxSet","regressionCapture","reviewEvidence"}
READY_FIELDS = ("portrait_status","icon_status","chibi_prefab_status","animation_status","vfx_status","sfx_status","regression_capture_status","review_status")


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def main() -> int:
    errors: list[str] = []
    for path in (CONTRACT,BUDGETS,ADDRESSES,REVIEW_SCHEMA,PREFAB_HIERARCHY,MANIFEST,COMPONENTS,PACKAGE_SCHEMA):
        if not path.exists(): errors.append(f"missing required M69 input: {path.relative_to(ROOT)}")
    if errors: return fail(errors)

    contract=json.loads(CONTRACT.read_text(encoding="utf-8"))
    review=json.loads(REVIEW_SCHEMA.read_text(encoding="utf-8"))
    package_schema=json.loads(PACKAGE_SCHEMA.read_text(encoding="utf-8"))
    if contract.get("contractVersion")!="m69-art-package-v2": errors.append("unexpected M69 contract version")
    if contract.get("censusTarget")!=427 or contract.get("batchCount")!=43: errors.append("M70-M73 rollout census must remain 427 packages / 43 batches")
    if set(contract.get("requiredComponents",[]))!=REQUIRED_COMPONENTS: errors.append("requiredComponents must exactly freeze the eight production gates")
    if "READY is valid only" not in contract.get("readyRule",""): errors.append("READY anti-fabrication rule missing")

    addresses={row["component"]:row for row in read_csv(ADDRESSES)}
    if set(addresses)!=REQUIRED_COMPONENTS-{"reviewEvidence"}: errors.append("Addressables layout must cover every runtime/file component")
    budgets={row["metric"]:row for row in read_csv(BUDGETS)}
    for metric in ("portrait_texture_max_px","icon_texture_max_px","chibi_texture_max_px","runtime_materials_max","bones_max","vfx_peak_particles_max","audio_clip_seconds_max"):
        if metric not in budgets or int(budgets[metric]["limit"])<=0: errors.append(f"missing/invalid performance budget: {metric}")

    if set(package_schema.get("properties",{}).get("paths",{}).get("required",[]))!=REQUIRED_COMPONENTS-{"reviewEvidence"}: errors.append("package schema paths do not match M69 component contract")
    if "reviewEvidence" not in package_schema.get("required",[]): errors.append("package schema must require reviewEvidence")
    required_review={"characterId","variant","reviewer","reviewedAt","captures","addressablesAudit","performanceAudit","result"}
    if not required_review.issubset(set(review.get("required",[]))): errors.append("review evidence schema missing mandatory audit fields")

    character_id,variant,slug=REPRESENTATIVE
    key=(character_id,variant)
    manifest={(r["character_id"],r["variant"]):r for r in read_csv(MANIFEST)}
    components={(r["character_id"],r["variant"]):r for r in read_csv(COMPONENTS)}
    if key not in manifest or key not in components: errors.append(f"representative package missing from manifests: {key}")
    else:
        package_ready=all(components[key].get(field,"").strip().upper()=="READY" for field in READY_FIELDS)
        descriptor=ROOT/f"art/packages/{character_id}/{slug}/package.json"
        if package_ready and not descriptor.exists(): errors.append("representative package marked READY without repository-backed descriptor")
        if manifest[key].get("status","").strip().upper()=="READY" and not package_ready: errors.append("manifest READY before all component gates READY")

    if errors: return fail(errors)
    package_ready=key in components and all(components[key].get(field,"").strip().upper()=="READY" for field in READY_FIELDS)
    print(f"M69_ART_PIPELINE_OK representative={character_id}:{variant} ready={str(package_ready).lower()} contract=m69-art-package-v2")
    return 0


def fail(errors:list[str])->int:
    print("M69 ART PIPELINE VALIDATION FAILED",file=sys.stderr)
    for error in errors: print(" -",error,file=sys.stderr)
    return 1

if __name__=="__main__": raise SystemExit(main())
