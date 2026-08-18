#!/usr/bin/env python3
import json,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def main():
    descriptor=json.loads((ROOT/'art/batches/M70_B01_B10.json').read_text(encoding='utf-8'))
    if descriptor.get('milestone')!='M70' or descriptor.get('firstBatch')!='B01' or descriptor.get('lastBatch')!='B10' or descriptor.get('expectedPackages')!=100:raise SystemExit('M70 batch descriptor drifted')
    subprocess.run([sys.executable,str(ROOT/'scripts/validate-m69-art-pipeline.py')],cwd=ROOT,check=True)
    subprocess.run([sys.executable,str(ROOT/'scripts/validate-art-batch-range.py'),'--first','B01','--last','B10','--expected','100'],cwd=ROOT,check=True)
    print('M70_ART_BATCHES_OK scope=B01-B10 expected=100 readiness=reported_not_fabricated')
    return 0
if __name__=='__main__':raise SystemExit(main())
