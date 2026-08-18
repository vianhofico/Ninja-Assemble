#!/usr/bin/env python3
import json,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def main():
    descriptor=json.loads((ROOT/'art/batches/M73_B31_B43.json').read_text(encoding='utf-8'))
    if descriptor.get('milestone')!='M73' or descriptor.get('firstBatch')!='B31' or descriptor.get('lastBatch')!='B43' or descriptor.get('expectedPackages')!=127:raise SystemExit('M73 batch descriptor drifted')
    subprocess.run([sys.executable,str(ROOT/'scripts/validate-m69-art-pipeline.py')],cwd=ROOT,check=True)
    subprocess.run([sys.executable,str(ROOT/'scripts/validate-art-batch-range.py'),'--first','B31','--last','B43','--expected','127'],cwd=ROOT,check=True)
    print('M73_ART_BATCHES_OK scope=B31-B43 expected=127 readiness=reported_not_fabricated')
    return 0
if __name__=='__main__':raise SystemExit(main())
