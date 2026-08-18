#!/usr/bin/env python3
from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
def require(path,*tokens):
    text=(ROOT/path).read_text(encoding='utf-8');missing=[t for t in tokens if t not in text]
    if missing:raise ValueError(f'{path} missing {missing}')
def main():
    try:
        catalog=(ROOT/'client-unity/Assets/Scripts/Game/UI/Production/CoreScreenCatalog.cs').read_text(encoding='utf-8')
        for token in ('"home"','"ninja"','"herodetail"','"formation"','"adventure"','"summon"','"inventory"'):
            if token not in catalog:raise ValueError(f'core screen catalog missing {token}')
        if catalog.count('new Spec(')!=7:raise ValueError('M67 requires exactly seven core production screen specs')
        require('client-unity/Assets/Scripts/Game/UI/Production/ProductionLegacyScreenBinding.cs','ForwardAction','sourceButton.onClick.Invoke()','targetButton.interactable','targetBody.text=sourceBody.text')
        require('client-unity/Assets/Scripts/Game/UI/Production/ProductionCoreScreenInstaller.cs','SceneManager.sceneLoaded','ProductionCorePanel','LiveStateCard','ProductionUiFactory','ProductionUiTokens','legacy.gameObject.SetActive(false)','ProductionLegacyScreenBinding')
        require('client-unity/Assets/Scripts/Game/UI/Production/ProductionUiTokens.cs','TouchMin')
        print('M67_CORE_SCREENS_OK screens=7 live_binding=1 action_forward=1 production_foundation=1')
        return 0
    except ValueError as e:
        print(f'M67_CORE_SCREENS_INVALID {e}',file=sys.stderr);return 1
if __name__=='__main__':raise SystemExit(main())
