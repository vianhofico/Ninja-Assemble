#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JOURNEY=ROOT/'game-data/release/m75-e2e-journey.json'
MATRIX=ROOT/'game-data/release/m75-reliability-matrix.json'
EVIDENCE_DIR=ROOT/'game-data/release/evidence/m75'
SCHEMA=EVIDENCE_DIR/'e2e-run.schema.json'
EXPECTED_STEPS=20
EXPECTED_CASES=10


def load(path:Path):return json.loads(path.read_text(encoding='utf-8'))

def require(path:str,*tokens:str):
    target=ROOT/path
    if not target.is_file():raise ValueError(f'missing contract file {path}')
    text=target.read_text(encoding='utf-8')
    missing=[token for token in tokens if token not in text]
    if missing:raise ValueError(f'{path} missing {missing}')

def current_commit():
    try:return subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    except Exception:return ''

def validate_structure():
    journey=load(JOURNEY);matrix=load(MATRIX);schema=load(SCHEMA)
    steps=journey.get('orderedSteps',[]);cases=matrix.get('requiredCases',[])
    if journey.get('journeyVersion')!='m75-e2e-v1' or len(steps)!=EXPECTED_STEPS:raise ValueError(f'M75 journey must contain exactly {EXPECTED_STEPS} ordered steps')
    if matrix.get('matrixVersion')!='m75-reliability-v1' or len(cases)!=EXPECTED_CASES:raise ValueError(f'M75 reliability matrix must contain exactly {EXPECTED_CASES} cases')
    step_ids=[step['id'] for step in steps];case_ids=[case['id'] for case in cases]
    if len(step_ids)!=len(set(step_ids)):raise ValueError('duplicate M75 journey step id')
    if len(case_ids)!=len(set(case_ids)):raise ValueError('duplicate M75 reliability case id')
    for step in steps:require(step['contractRef'],*step.get('requiredTokens',[]))
    if schema.get('properties',{}).get('schemaVersion',{}).get('const')!='m75-e2e-run-v1':raise ValueError('M75 evidence schema version drifted')

    # Reliability-critical implementation contracts.
    require('server/src/main/java/com/ninjaassemble/economy/application/WalletService.java','idempotencyKey','findWithLockById','ledger.existsByPlayerIdAndIdempotencyKey')
    require('server/src/main/java/com/ninjaassemble/economy/domain/WalletBalance.java','Math.addExact','after < 0','insufficient balance')
    require('server/src/main/java/com/ninjaassemble/inventory/domain/InventoryStack.java','insufficient item quantity')
    require('server/src/main/java/com/ninjaassemble/campaign/application/CampaignSweepService.java','pg_advisory_xact_lock','withReplayed(true)')
    require('server/src/main/java/com/ninjaassemble/pvp/application/ProductionArenaService.java','pg_advisory_xact_lock','competitive_battle_requests')
    require('server/src/main/java/com/ninjaassemble/progression/application/AdvancedProgressionApplicationService.java','pg_advisory_xact_lock','progression_upgrade_requests')
    require('server/src/main/java/com/ninjaassemble/battle/sim/RealtimeBattleEngine.java','PriorityQueue<ScheduledEvent>','timestampMs')

    migrations=list((ROOT/'server/src/main/resources/db/migration').glob('V*__*.sql'))
    versions=[]
    for path in migrations:
        match=re.match(r'V(\d+)__',path.name)
        if not match:raise ValueError(f'invalid Flyway migration name {path.name}')
        versions.append(int(match.group(1)))
    if len(versions)!=len(set(versions)):raise ValueError('duplicate Flyway migration version')
    if not versions or max(versions)<17:raise ValueError('M75 expected migration chain through at least V17')

    subprocess.run([sys.executable,str(ROOT/'scripts/validate-m74-parity.py')],cwd=ROOT,check=True)
    return step_ids,case_ids,len(migrations)

def validate_evidence(step_ids,case_ids):
    reports=[path for path in sorted(EVIDENCE_DIR.glob('*.json')) if path.name!='e2e-run.schema.json']
    if not reports:raise ValueError('no real M75 E2E execution report exists')
    head=current_commit();valid=[]
    for path in reports:
        report=load(path)
        if report.get('schemaVersion')!='m75-e2e-run-v1' or report.get('result')!='PASS':continue
        commit=report.get('commitSha','')
        if not re.fullmatch(r'[0-9a-f]{40}',commit):continue
        if head and commit!=head:continue
        seen_steps={row.get('id') for row in report.get('journeySteps',[]) if row.get('result')=='PASS'}
        seen_cases={row.get('id') for row in report.get('reliabilityCases',[]) if row.get('result')=='PASS'}
        artifacts=report.get('artifacts',{})
        if seen_steps!=set(step_ids) or seen_cases!=set(case_ids):continue
        if not all(str(artifacts.get(key,'')).strip() for key in ('serverLogs','testReport','databaseMigrationLog')):continue
        valid.append(path)
    if not valid:raise ValueError('no PASS M75 report matches current commit, all journey steps, reliability cases and required artifacts')
    return valid

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--enforce',action='store_true');args=parser.parse_args()
    try:
        step_ids,case_ids,migrations=validate_structure()
        reports=[]
        if args.enforce:reports=validate_evidence(step_ids,case_ids)
        print(f'M75_E2E_OK framework=1 journey={len(step_ids)} reliability={len(case_ids)} migrations={migrations} enforce={str(args.enforce).lower()} certified_reports={len(reports)}')
        return 0
    except (ValueError,subprocess.CalledProcessError,KeyError,json.JSONDecodeError) as error:
        print(f'M75_E2E_INVALID {error}',file=sys.stderr);return 1

if __name__=='__main__':raise SystemExit(main())
