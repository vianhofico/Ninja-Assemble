#!/usr/bin/env python3
from __future__ import annotations
import argparse,csv
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
TARGET=427
BATCH_SIZE=10
BATCH_COUNT=43


def read(path:Path):
    with path.open(encoding='utf-8-sig',newline='') as handle:return list(csv.DictReader(handle))


def census():
    values=set()
    for path in sorted(ROOT.glob('game-data/reference/variant-census*.csv')):
        for row in read(path):
            values.add((row['character_id'].strip(),row['variant'].strip()))
    return sorted(values,key=lambda value:(value[0],value[1]))


def plan():
    values=census()
    if len(values)!=TARGET:raise SystemExit(f'ART_BATCH_CENSUS_INVALID expected={TARGET} actual={len(values)}')
    rows=[]
    for index,(character_id,variant) in enumerate(values):
        batch=index//BATCH_SIZE+1
        rows.append({'batch_id':f'B{batch:02d}','batch_index':batch,'ordinal':index+1,'character_id':character_id,'variant':variant})
    if rows[-1]['batch_id']!='B43':raise SystemExit('ART_BATCH_PLAN_INVALID last batch is not B43')
    return rows


def validate(rows):
    sizes={f'B{i:02d}':0 for i in range(1,BATCH_COUNT+1)}
    for row in rows:sizes[row['batch_id']]+=1
    for batch,size in sizes.items():
        expected=7 if batch=='B43' else 10
        if size!=expected:raise SystemExit(f'ART_BATCH_SIZE_INVALID {batch} expected={expected} actual={size}')


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--write',action='store_true');parser.add_argument('--output',default='art/manifests/hero-art-batch-plan.csv');args=parser.parse_args()
    rows=plan();validate(rows)
    if args.write:
        path=ROOT/args.output;path.parent.mkdir(parents=True,exist_ok=True)
        with path.open('w',encoding='utf-8',newline='') as handle:
            writer=csv.DictWriter(handle,fieldnames=['batch_id','batch_index','ordinal','character_id','variant']);writer.writeheader();writer.writerows(rows)
        print(f'ART_BATCH_PLAN_WRITTEN path={path.relative_to(ROOT)} rows={len(rows)} batches={BATCH_COUNT}')
    else:
        print(f'ART_BATCH_PLAN_OK rows={len(rows)} batches={BATCH_COUNT} b01=10 b43=7')
    return 0

if __name__=='__main__':raise SystemExit(main())
