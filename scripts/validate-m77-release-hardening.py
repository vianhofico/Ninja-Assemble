#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CHECKLIST=ROOT/'game-data/release/m77-release-checklist.json'
PROVENANCE=ROOT/'game-data/release/m77-asset-provenance.csv'
STORE=ROOT/'game-data/release/m77-store-metadata.json'
OPERATOR=ROOT/'game-data/release/m77-operator-evidence.csv'
EXPECTED_CATEGORIES={
    'security-config','rate-limit-cache','database-backup-rollback','android-signing-reproducibility',
    'localization','accessibility','licensing-provenance','store-metadata','release-notes-known-issues',
    'support-operations','aggregate-release-evidence'
}
REQUIRED_OPERATOR_TYPES={'DATABASE_RESTORE','SIGNED_AAB','STORE_REVIEW','SUPPORT_ENDPOINT','RIGHTS_CLEARANCE'}


def load(path:Path):return json.loads(path.read_text(encoding='utf-8'))
def rows(path:Path):
    with path.open(encoding='utf-8-sig',newline='') as handle:return list(csv.DictReader(handle))
def require(path:str,*tokens:str):
    target=ROOT/path
    if not target.is_file():raise ValueError(f'missing release contract {path}')
    text=target.read_text(encoding='utf-8')
    missing=[token for token in tokens if token not in text]
    if missing:raise ValueError(f'{path} missing {missing}')
    return text

def run(path:str,*args:str):subprocess.run([sys.executable,str(ROOT/path),*args],cwd=ROOT,check=True)
def current_commit():
    try:return subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    except Exception:return ''


def validate_structure():
    checklist=load(CHECKLIST);categories=checklist.get('categories',[])
    if checklist.get('checklistVersion')!='m77-release-hardening-v1':raise ValueError('M77 checklist version drifted')
    ids={row.get('id') for row in categories}
    if ids!=EXPECTED_CATEGORIES or len(categories)!=len(EXPECTED_CATEGORIES):raise ValueError(f'M77 checklist categories drifted: {sorted(ids)}')
    if any(row.get('implementationStatus')!='READY' for row in categories):raise ValueError('all M77 implementation categories must be READY before milestone merge')
    for row in categories:
        refs=row.get('evidenceRefs',[])
        if not refs:raise ValueError(f"{row.get('id')}: missing evidenceRefs")
        for ref in refs:
            if not (ROOT/ref).exists():raise ValueError(f"{row.get('id')}: missing evidenceRef {ref}")

    prod=require('server/src/main/resources/application-prod.yml','${DB_URL}','${DB_USER}','${DB_PASSWORD}','${REDIS_PASSWORD}','${GAME_SESSION_SECRET}','shutdown: graceful','show-details: never')
    if 'DB_PASSWORD:' in prod or 'GAME_SESSION_SECRET:' in prod:raise ValueError('production config must not hard-code release secrets')
    require('server/src/main/java/com/ninjaassemble/security/SessionTokenService.java','HmacSHA256','MessageDigest.isEqual','expiresAt','expectedPlayerId')
    require('server/src/main/java/com/ninjaassemble/security/PlayerAuthorizationFilter.java','@Profile("prod")','Authorization','Bearer ','UNAUTHORIZED_PLAYER_SESSION')
    require('server/src/main/java/com/ninjaassemble/security/ProductionSecurityHeadersFilter.java','X-Content-Type-Options','X-Frame-Options','Permissions-Policy','Cache-Control')
    require('server/src/main/java/com/ninjaassemble/security/ProductionRateLimitFilter.java','StringRedisTemplate','release-rate:','Retry-After','RATE_LIMIT_BACKEND_UNAVAILABLE','Authorization')
    require('server/src/main/java/com/ninjaassemble/player/api/PlayerController.java','SessionTokenService','sessionToken','sessions.issue')
    require('client-unity/Assets/Scripts/Game/Network/ApiAuthSession.cs','BearerToken','Set','Clear')
    require('client-unity/Assets/Scripts/Game/Network/GameApiClient.cs','ApiAuthSession.Set','Authorization','Bearer ','signed player session token')
    require('client-unity/Assets/Scripts/Game/Progression/AdvancedProgressionClient.cs','ApiAuthSession.HasToken','Authorization','Bearer ')
    require('server/src/test/java/com/ninjaassemble/security/SessionTokenServiceTest.java','issuedTokenIsBoundToPlayerAndSignature','expiredTokenIsRejected')

    require('client-unity/Assets/Scripts/Game/Settings/AccessibilityPreferences.cs','TextScaleKey','ReduceMotionKey','HapticsKey','1.3f','ApplyTextScale')
    require('client-unity/Assets/Scripts/Game/UI/Production/ProductionUiTokens.cs','TouchMin')
    require('client-unity/Assets/Scripts/Game/UI/Production/ProductionFeedback.cs','AccessibilityPreferences.HapticsEnabled')

    store=load(STORE)
    if store.get('metadataVersion')!='m77-store-metadata-v1':raise ValueError('M77 store metadata version drifted')
    if store.get('applicationId')!='com.vianhofico.ninjaassemble':raise ValueError('store applicationId does not match Android build contract')
    if set(store.get('supportedLocales',[]))!={'vi-VN','en-US'}:raise ValueError('M77 store locales must be vi-VN/en-US')
    if store.get('releaseNotesRef')!='docs/release/M77_RELEASE_NOTES.md':raise ValueError('store release notes ref drifted')

    provenance=rows(PROVENANCE)
    if not provenance or len({row['scope'] for row in provenance})!=len(provenance):raise ValueError('M77 provenance ledger missing/duplicate scopes')
    if not any(row['release_status']=='RIGHTS_NOT_DOCUMENTED' for row in provenance):raise ValueError('M77 provenance must truthfully retain undocumented third-party IP blocker')

    with OPERATOR.open(encoding='utf-8-sig',newline='') as handle:
        header=next(csv.reader(handle),[])
    if header!=['evidence_id','git_sha','evidence_type','status','artifact_ref','recorded_at','notes']:raise ValueError('M77 operator evidence header drifted')

    require('docs/release/M77_DATABASE_BACKUP_ROLLBACK.md','pg_dump','Flyway','Restore','Stop writes')
    require('docs/release/M77_SUPPORT_RUNBOOK.md','P0','request ID','rollback')
    require('docs/release/M77_KNOWN_ISSUES.md','Reference/parity evidence is incomplete','Third-party character/IP rights are not documented','Spring Boot 4 migration debt')
    require('docs/release/M77_RELEASE_NOTES.md','Certification blockers')

    run('scripts/validate-runtime-localization.py')
    run('scripts/validate-m74-parity.py')
    run('scripts/validate-m75-e2e.py')
    run('scripts/validate-m76-android-performance.py')
    run('scripts/validate-art-packages.py')
    return checklist,categories,provenance,store


def validate_certification(categories,provenance,store):
    blockers=[]
    pending=[row['id'] for row in categories if row.get('certificationStatus')!='READY']
    if pending:blockers.append('checklist pending='+','.join(pending))
    bad_provenance=[row['scope'] for row in provenance if row.get('release_status')!='READY' or not row.get('license_or_rights_ref','').strip()]
    if bad_provenance:blockers.append('provenance not ready='+','.join(bad_provenance))
    pending_store=[key for key in ('privacyPolicyUrl','supportUrl','contentRatingQuestionnaire','dataSafetyForm','rightsClearance') if str(store.get(key,'')).startswith(('PENDING','BLOCKED'))]
    if pending_store:blockers.append('store metadata pending='+','.join(pending_store))

    head=current_commit();operator_rows=rows(OPERATOR);passing={}
    for row in operator_rows:
        if row.get('status')!='PASS':continue
        if not re.fullmatch(r'[0-9a-f]{40}',row.get('git_sha','')):continue
        if head and row['git_sha']!=head:continue
        if not row.get('artifact_ref','').strip() or not row.get('recorded_at','').strip():continue
        passing[row.get('evidence_type','')]=row
    missing=sorted(REQUIRED_OPERATOR_TYPES-set(passing))
    if missing:blockers.append('operator evidence missing='+','.join(missing))

    strict=(
        ('scripts/validate-m74-parity.py',['--enforce']),
        ('scripts/validate-m75-e2e.py',['--enforce']),
        ('scripts/validate-m76-android-performance.py',['--enforce']),
        ('scripts/validate-art-packages.py',['--release']),
        ('scripts/validate-production-assets.py',[]),
        ('scripts/validate-release-readiness.py',['--release']),
        ('scripts/release-audit.py',['--enforce'])
    )
    for path,args in strict:
        result=subprocess.run([sys.executable,str(ROOT/path),*args],cwd=ROOT)
        if result.returncode!=0:blockers.append(Path(path).name+' failed')
    if blockers:raise ValueError('; '.join(blockers))


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--enforce',action='store_true');args=parser.parse_args()
    try:
        checklist,categories,provenance,store=validate_structure()
        if args.enforce:validate_certification(categories,provenance,store)
        ready=sum(row.get('certificationStatus')=='READY' for row in categories)
        print(f'M77_RELEASE_HARDENING_OK implementation=11/11 certification={ready}/11 enforce={str(args.enforce).lower()} rc_tag_allowed={str(args.enforce).lower()}')
        return 0
    except (ValueError,KeyError,json.JSONDecodeError,subprocess.CalledProcessError) as error:
        print(f'M77_RELEASE_HARDENING_INVALID {error}',file=sys.stderr);return 1

if __name__=='__main__':raise SystemExit(main())
