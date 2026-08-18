#!/usr/bin/env python3
from __future__ import annotations
import argparse,csv,subprocess,sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PROFILES=ROOT/'game-data/reference/balance-profiles.csv'
CENSUS=ROOT/'game-data/release/m74-parity-census.csv'
EXPECTED_CATEGORIES={'COMBAT_STATS','DAMAGE_FORMULA','SUMMON_PROFILE','LEVEL_COST','ABILITY_CYCLE','STRUCTURED_EFFECTS','TECHNIQUE_MAPPING','PASSIVE_LIFECYCLE','REALTIME_TIMING','RAGE_RULES'}
FINAL_PROFILE_STATUS='VERIFIED'
FINAL_FEATURE_STATUS='PARITY_PASS'


def rows(path):
    with path.open(encoding='utf-8-sig',newline='') as handle:return list(csv.DictReader(handle))


def run(path,*args):
    subprocess.run([sys.executable,str(ROOT/path),*args],cwd=ROOT,check=True)


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--enforce',action='store_true');args=parser.parse_args()
    profiles=rows(PROFILES);features=rows(CENSUS)
    categories={row['category'].strip() for row in profiles}
    if categories!=EXPECTED_CATEGORIES or len(profiles)!=10:raise SystemExit(f'M74_REFERENCE_PROFILE_SET_INVALID count={len(profiles)} categories={sorted(categories)}')
    if len({row['profile_id'] for row in profiles})!=10:raise SystemExit('M74 duplicate reference profile ids')
    if len(features)!=9 or len({row['feature_id'] for row in features})!=9:raise SystemExit('M74 feature census must contain exactly nine unique release families')
    for row in features:
        validator=ROOT/row['validator']
        if not validator.exists():raise SystemExit(f"M74 missing feature validator {row['validator']}")

    run('scripts/validate-reference-evidence.py')
    run('scripts/validate-m58-skill-identity.py')
    run('scripts/validate-m59-mechanics.py')
    run('scripts/validate-m60-balance-presentation.py')
    for row in features:run(row['validator'])

    verified=sum(row['status'].strip().upper()==FINAL_PROFILE_STATUS for row in profiles)
    parity_pass=sum(row['evidence_status'].strip().upper()==FINAL_FEATURE_STATUS for row in features)
    unresolved_profiles=[row['profile_id'] for row in profiles if row['status'].strip().upper()!=FINAL_PROFILE_STATUS]
    unresolved_features=[row['feature_id'] for row in features if row['evidence_status'].strip().upper()!=FINAL_FEATURE_STATUS]
    print(f'M74_PARITY_DASHBOARD profiles_verified={verified}/10 feature_parity={parity_pass}/{len(features)} unresolved_profiles={len(unresolved_profiles)} unresolved_features={len(unresolved_features)}')

    if args.enforce:
        blockers=[]
        if verified!=10:blockers.append(f'reference profiles VERIFIED={verified}/10')
        if parity_pass!=len(features):blockers.append(f'feature census PARITY_PASS={parity_pass}/{len(features)}')
        strict=[
            ('scripts/validate-m58-skill-identity.py','identity'),
            ('scripts/validate-m59-mechanics.py','mechanics'),
            ('scripts/validate-m60-balance-presentation.py','balance/presentation')]
        for path,label in strict:
            result=subprocess.run([sys.executable,str(ROOT/path),'--enforce'],cwd=ROOT)
            if result.returncode!=0:blockers.append(label+' review incomplete')
        if blockers:
            print('M74_PARITY_BLOCKED '+'; '.join(blockers),file=sys.stderr)
            if unresolved_profiles:print('M74_UNVERIFIED_PROFILES '+','.join(unresolved_profiles),file=sys.stderr)
            if unresolved_features:print('M74_UNVERIFIED_FEATURES '+','.join(unresolved_features),file=sys.stderr)
            return 1
    print('M74_PARITY_OK structure=1 evidence_truthful=1 enforce='+str(args.enforce).lower())
    return 0

if __name__=='__main__':raise SystemExit(main())
