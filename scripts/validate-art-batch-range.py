#!/usr/bin/env python3
from __future__ import annotations
import argparse,csv,subprocess,sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
READY_FIELDS=("portrait_status","icon_status","chibi_prefab_status","animation_status","vfx_status","sfx_status","regression_capture_status","review_status")


def read(path):
    with path.open(encoding='utf-8-sig',newline='') as handle:return list(csv.DictReader(handle))


def census():
    values=set()
    for path in sorted(ROOT.glob('game-data/reference/variant-census*.csv')):
        for row in read(path):values.add((row['character_id'].strip(),row['variant'].strip()))
    values=sorted(values,key=lambda value:(value[0],value[1]))
    if len(values)!=427:raise SystemExit(f'ART_BATCH_CENSUS_INVALID expected=427 actual={len(values)}')
    return values


def batch_for(index):return f'B{index//10+1:02d}'


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--first',required=True);parser.add_argument('--last',required=True);parser.add_argument('--expected',type=int,required=True);parser.add_argument('--require-ready',action='store_true');args=parser.parse_args()
    first=int(args.first[1:]);last=int(args.last[1:])
    if first<1 or last>43 or first>last:raise SystemExit('invalid art batch range')
    selected=[(batch_for(i),key) for i,key in enumerate(census()) if first<=int(batch_for(i)[1:])<=last]
    if len(selected)!=args.expected:raise SystemExit(f'ART_BATCH_RANGE_INVALID expected={args.expected} actual={len(selected)}')
    components={(row['character_id'].strip(),row['variant'].strip()):row for row in read(ROOT/'art/manifests/hero-art-component-status.csv')}
    ready=[]
    per_batch={f'B{i:02d}':{'total':0,'ready':0} for i in range(first,last+1)}
    for batch,key in selected:
        per_batch[batch]['total']+=1
        row=components.get(key)
        is_ready=row is not None and all(row.get(field,'').strip().upper()=='READY' for field in READY_FIELDS)
        if is_ready:
            ready.append(key);per_batch[batch]['ready']+=1
    subprocess.run([sys.executable,str(ROOT/'scripts/validate-art-packages.py')],cwd=ROOT,check=True)
    summary=' '.join(f"{batch}={state['ready']}/{state['total']}" for batch,state in per_batch.items())
    if args.require_ready and len(ready)!=len(selected):raise SystemExit(f'ART_BATCH_NOT_READY range={args.first}-{args.last} ready={len(ready)}/{len(selected)} {summary}')
    print(f'ART_BATCH_RANGE_OK range={args.first}-{args.last} ready={len(ready)}/{len(selected)} {summary}')
    return 0

if __name__=='__main__':raise SystemExit(main())
