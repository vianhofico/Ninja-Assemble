#!/usr/bin/env python3
from __future__ import annotations

import csv,json,subprocess,sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CONTRACT=ROOT/'art/pipeline/art-package-contract-v2.json'
MOBILE_BUDGETS=ROOT/'art/mobile-asset-budgets.json'
BUDGETS=ROOT/'art/pipeline/performance-budgets.csv'
ADDRESSES=ROOT/'art/pipeline/addressables-layout.csv'
REVIEW_SCHEMA=ROOT/'art/pipeline/review-evidence.schema.json'
PREFAB_HIERARCHY=ROOT/'art/pipeline/PREFAB-HIERARCHY.md'
MANIFEST=ROOT/'art/manifests/hero-art-manifest.csv'
COMPONENTS=ROOT/'art/manifests/hero-art-component-status.csv'
PACKAGE_SCHEMA=ROOT/'art/hero-art-package.schema.json'
REPRESENTATIVE=('naruto-uzumaki','Sage Mode','sage-mode')
REQUIRED_COMPONENTS={'portrait','icon','chibiPrefab','animationSet','vfxSet','sfxSet','regressionCapture','reviewEvidence'}
READY_FIELDS=('portrait_status','icon_status','chibi_prefab_status','animation_status','vfx_status','sfx_status','regression_capture_status','review_status')
ANIMATOR_STATES={'Idle','Move','Attack','Hit','KO','Skill','RageSkill'}


def read_csv(path:Path):
    with path.open(encoding='utf-8-sig',newline='') as handle:return list(csv.DictReader(handle))


def main()->int:
    errors=[]
    for path in (CONTRACT,MOBILE_BUDGETS,BUDGETS,ADDRESSES,REVIEW_SCHEMA,PREFAB_HIERARCHY,MANIFEST,COMPONENTS,PACKAGE_SCHEMA):
        if not path.exists():errors.append(f'missing required M69 input: {path.relative_to(ROOT)}')
    if errors:return fail(errors)

    contract=json.loads(CONTRACT.read_text(encoding='utf-8'))
    mobile=json.loads(MOBILE_BUDGETS.read_text(encoding='utf-8'))
    review=json.loads(REVIEW_SCHEMA.read_text(encoding='utf-8'))
    package_schema=json.loads(PACKAGE_SCHEMA.read_text(encoding='utf-8'))

    if contract.get('contractVersion')!='m69-art-package-v2':errors.append('unexpected M69 v2 contract version')
    if (contract.get('censusTarget'),contract.get('batchCount'),contract.get('batchSize'),contract.get('lastBatchSize'))!=(427,43,10,7):errors.append('M70-M73 rollout must remain 427 / 43 batches / 10 except B43=7')
    if set(contract.get('requiredComponents',[]))!=REQUIRED_COMPONENTS:errors.append('requiredComponents must freeze eight production gates')
    if set(contract.get('requiredAnimatorStates',[]))!=ANIMATOR_STATES:errors.append('v2 animator contract must remain canonical Rage states')
    if contract.get('statusFlow')!=['TODO','CONCEPT','IN_PROGRESS','REVIEW','READY']:errors.append('art status flow drifted')
    if 'READY is valid only' not in contract.get('readyRule',''):errors.append('READY anti-fabrication rule missing')

    if mobile.get('budgetVersion')!='m69-v1':errors.append('mobile art budget version drifted')
    if mobile.get('texture',{}).get('compression')!='ASTC':errors.append('Android hero textures must use ASTC')
    if set(mobile.get('animation',{}).get('requiredStates',[]))!=ANIMATOR_STATES:errors.append('mobile animator states drifted')
    package=mobile.get('package',{})
    if package.get('releaseRequiresRegressionCapture') is not True or package.get('releaseRequiresHumanReviewEvidence') is not True:errors.append('release must require capture + human review evidence')
    if not isinstance(package.get('maxInstalledBytesPerHeroVariant'),int) or package.get('maxInstalledBytesPerHeroVariant')<=0:errors.append('per-variant installed byte budget invalid')

    addresses={row['component']:row for row in read_csv(ADDRESSES)}
    if set(addresses)!=REQUIRED_COMPONENTS-{'reviewEvidence'}:errors.append('Addressables layout incomplete')
    for row in addresses.values():
        if '{characterId}' not in row['address_pattern'] or '{variantSlug}' not in row['address_pattern']:errors.append(f"address pattern not hero-scoped: {row['component']}")

    budgets={row['metric']:row for row in read_csv(BUDGETS)}
    if len(budgets)!=10 or any(int(row['limit'])<=0 for row in budgets.values()):errors.append('performance budget table invalid')

    if set(package_schema.get('properties',{}).get('paths',{}).get('required',[]))!=REQUIRED_COMPONENTS-{'reviewEvidence'}:errors.append('package schema paths do not match M69 contract')
    if 'reviewEvidence' not in package_schema.get('required',[]):errors.append('package schema must require reviewEvidence')
    required_review={'characterId','variant','reviewer','reviewedAt','captures','addressablesAudit','performanceAudit','result'}
    if not required_review.issubset(set(review.get('required',[]))):errors.append('review evidence schema missing mandatory audit fields')

    prefab=PREFAB_HIERARCHY.read_text(encoding='utf-8')
    for token in ('VisualRoot','VfxSockets','UiAnchor','BattleActorView','RageSkill','No authoritative combat logic'):
        if token not in prefab:errors.append(f'prefab hierarchy missing {token}')

    character_id,variant,slug=REPRESENTATIVE;key=(character_id,variant)
    manifest={(r['character_id'],r['variant']):r for r in read_csv(MANIFEST)}
    components={(r['character_id'],r['variant']):r for r in read_csv(COMPONENTS)}
    if key not in manifest or key not in components:errors.append(f'representative package missing: {key}')
    else:
        ready=all(components[key].get(field,'').strip().upper()=='READY' for field in READY_FIELDS)
        if ready and not (ROOT/f'art/packages/{character_id}/{slug}/package.json').exists():errors.append('representative READY without repository descriptor')
        if manifest[key].get('status','').strip().upper()=='READY' and not ready:errors.append('manifest READY before component READY')

    if errors:return fail(errors)
    subprocess.run([sys.executable,str(ROOT/'scripts/generate-art-batch-plan.py')],cwd=ROOT,check=True)
    subprocess.run([sys.executable,str(ROOT/'scripts/validate-art-packages.py')],cwd=ROOT,check=True)
    ready_count=sum(all(row.get(field,'').strip().upper()=='READY' for field in READY_FIELDS) for row in components.values())
    print(f'M69_ART_PIPELINE_OK census=427 batches=43 tracked={len(components)} ready={ready_count} contract=m69-art-package-v2')
    return 0


def fail(errors):
    print('M69 ART PIPELINE VALIDATION FAILED',file=sys.stderr)
    for error in errors:print(' -',error,file=sys.stderr)
    return 1

if __name__=='__main__':raise SystemExit(main())
