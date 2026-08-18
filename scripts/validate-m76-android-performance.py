#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
import subprocess
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PROFILES=ROOT/'game-data/release/m76-device-profiles.json'
EVIDENCE=ROOT/'game-data/release/mobile-device-evidence.csv'
SCHEMA=ROOT/'game-data/release/evidence/m76/device-run.schema.json'
PROBE='client-unity/Assets/Scripts/Game/Performance/MobilePerformanceProbe.cs'
EXPECTED_CLASSES={'LOW','MID','HIGH'}
EXPECTED_HEADER=[
    'evidence_id','git_sha','unity_version','artifact_type','artifact_ref','device_model','android_version','device_class',
    'physical_device','build_fingerprint','benchmark_profile','smoke_pass','performance_pass','avg_fps','p95_frame_ms',
    'max_memory_mb','capture_ref','notes'
]


def load(path:Path):return json.loads(path.read_text(encoding='utf-8'))

def require(path:str,*tokens:str):
    target=ROOT/path
    if not target.is_file():raise ValueError(f'missing M76 contract file {path}')
    text=target.read_text(encoding='utf-8')
    missing=[token for token in tokens if token not in text]
    if missing:raise ValueError(f'{path} missing {missing}')
    return text


def validate_profiles():
    doc=load(PROFILES)
    if doc.get('profileVersion')!='m76-device-profiles-v1':raise ValueError('M76 profile version drifted')
    if doc.get('measurementWindowSeconds')!=180 or doc.get('warmupSeconds')!=30:raise ValueError('M76 benchmark window must remain 30s warmup + 180s measure')
    if doc.get('requiredScenario')!='campaign-realtime-battle-rage-v1':raise ValueError('M76 benchmark scenario drifted')
    classes=doc.get('classes',{})
    if set(classes)!=EXPECTED_CLASSES:raise ValueError('M76 must define LOW/MID/HIGH exactly')
    for name,profile in classes.items():
        fps=float(profile.get('minAverageFps',0));p95=float(profile.get('maxP95FrameMs',0));memory=float(profile.get('maxMemoryMb',0))
        if fps<=0 or p95<=0 or memory<=0:raise ValueError(f'{name}: non-positive performance threshold')
        if fps>120 or p95>100 or memory>8192:raise ValueError(f'{name}: implausible performance threshold')
    if not classes['LOW']['minAverageFps']<=classes['MID']['minAverageFps']<=classes['HIGH']['minAverageFps']:raise ValueError('M76 FPS targets must become stricter LOW→HIGH')
    if not classes['LOW']['maxP95FrameMs']>=classes['MID']['maxP95FrameMs']>=classes['HIGH']['maxP95FrameMs']:raise ValueError('M76 p95 targets must become stricter LOW→HIGH')
    rule=doc.get('releaseRule',{})
    if int(rule.get('minimumPassingPhysicalDevices',0))<2 or int(rule.get('minimumDistinctDeviceModels',0))<2 or int(rule.get('minimumDistinctClasses',0))<2:raise ValueError('M76 release rule must require >=2 devices/models/classes')
    for flag in ('requireExactCommitSha','requireBuildFingerprint','requireCaptureReference'):
        if rule.get(flag) is not True:raise ValueError(f'M76 release rule must require {flag}')
    return doc


def validate_schema():
    schema=load(SCHEMA)
    if schema.get('properties',{}).get('schemaVersion',{}).get('const')!='m76-device-run-v1':raise ValueError('M76 device evidence schema version drifted')
    device=schema.get('properties',{}).get('device',{}).get('properties',{})
    if device.get('physical',{}).get('const') is not True:raise ValueError('M76 evidence schema must require physical device')
    benchmark=schema.get('properties',{}).get('benchmark',{}).get('properties',{})
    if benchmark.get('profile',{}).get('const')!='m76-device-profiles-v1':raise ValueError('M76 evidence schema benchmark profile drifted')
    if benchmark.get('measurementSeconds',{}).get('minimum')!=180:raise ValueError('M76 evidence schema must require >=180s measurement')


def validate_ledger_header():
    with EVIDENCE.open(encoding='utf-8-sig',newline='') as handle:
        reader=csv.reader(handle);header=next(reader,[])
    if header!=EXPECTED_HEADER:raise ValueError(f'M76 evidence header drifted: {header}')


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--enforce',action='store_true');args=parser.parse_args()
    try:
        validate_profiles();validate_schema();validate_ledger_header()
        require(PROBE,
                'warmupSeconds = 30f','measurementSeconds = 180f','campaign-realtime-battle-rage-v1',
                'Time.unscaledDeltaTime','Profiler.GetTotalAllocatedMemoryLong','Average','0.95f',
                'Application.persistentDataPath','m76-performance-probe.json','Presentation-only benchmark probe')
        subprocess.run([sys.executable,str(ROOT/'scripts/validate-android-build.py')],cwd=ROOT,check=True)
        subprocess.run([sys.executable,str(ROOT/'scripts/validate-mobile-release-evidence.py'),*(['--release'] if args.enforce else [])],cwd=ROOT,check=True)
        print(f'M76_ANDROID_PERFORMANCE_OK framework=1 classes=3 physical_required=1 enforce={str(args.enforce).lower()}')
        return 0
    except (ValueError,KeyError,json.JSONDecodeError,subprocess.CalledProcessError) as error:
        print(f'M76_ANDROID_PERFORMANCE_INVALID {error}',file=sys.stderr);return 1

if __name__=='__main__':raise SystemExit(main())
