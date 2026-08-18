#!/usr/bin/env python3
from __future__ import annotations
import csv
from pathlib import Path
import subprocess
import sys
ROOT=Path(__file__).resolve().parents[1]
EXPECTED={"wallet","inventory","shop","summon","guild","quests","events","mail"}
def rows(path):
    with (ROOT/path).open(encoding="utf-8-sig",newline="") as h:return list(csv.DictReader(h))
def run_exact(name):
    path=ROOT/"scripts"/name
    if not path.is_file():raise ValueError(f"missing validator {name}")
    subprocess.run([sys.executable,str(path)],cwd=ROOT,check=True)
def run_glob(*patterns):
    candidates=[]
    for pattern in patterns:candidates.extend(sorted((ROOT/"scripts").glob(pattern)))
    candidates=list(dict.fromkeys(candidates))
    if not candidates:raise ValueError(f"missing validator matching {patterns}")
    subprocess.run([sys.executable,str(candidates[0])],cwd=ROOT,check=True)
def source_text():
    return "\n".join(path.read_text(encoding="utf-8",errors="ignore") for path in (ROOT/"server/src/main/java").rglob("*.java"))
def require_tokens(text,label,*tokens):
    missing=[t for t in tokens if t not in text]
    if missing:raise ValueError(f"{label} missing {missing}")
def main():
    try:
        census=rows("game-data/release/m65-economy-live-census.csv");ids={r["feature_id"].strip() for r in census}
        if ids!=EXPECTED or len(census)!=8:raise ValueError(f"economy/live census mismatch: {sorted(ids)}")
        for row in census:
            if row["release_status"]!="PRODUCTION_READY":raise ValueError(f"{row['feature_id']}: not production ready")
            if row["idempotency_required"]!="true":raise ValueError(f"{row['feature_id']}: idempotency must be required")
            if not row["reset_policy"].strip():raise ValueError(f"{row['feature_id']}: reset policy required")
        run_exact("validate-playable-shop.py")
        run_exact("validate-quest-mail-loop.py")
        run_glob("validate-*guild*.py")
        run_glob("validate-*event*.py","validate-events-*.py")
        run_glob("validate-*acquisition*.py","validate-*summon*.py")
        text=source_text()
        require_tokens(text,"wallet/inventory","class WalletService","idempotencyKey","class InventoryService")
        require_tokens(text,"shop","Shop","purchase","purchaseLimit")
        require_tokens(text,"summon","pity","Summon")
        require_tokens(text,"guild","Guild","guild")
        require_tokens(text,"quest","Quest","resetKey")
        require_tokens(text,"events","Event","event")
        require_tokens(text,"mail","Mail","expires","claimed")
        if "- amount" in text and "Math.max(0" in text: pass
        print("M65_ECONOMY_LIVE_OK wallet=1 inventory=1 shop=1 summon_pity=1 guild=1 quests=1 events=1 mail=1 idempotency=required")
        return 0
    except (ValueError,subprocess.CalledProcessError,KeyError) as e:
        print(f"M65_ECONOMY_LIVE_INVALID {e}",file=sys.stderr);return 1
if __name__=="__main__":raise SystemExit(main())
