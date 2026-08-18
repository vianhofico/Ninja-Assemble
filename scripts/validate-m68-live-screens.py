#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]


def require(path,*tokens):
    text=(ROOT/path).read_text(encoding='utf-8')
    missing=[t for t in tokens if t not in text]
    if missing: raise SystemExit(f'M68_LIVE_SCREEN_INVALID {path} missing={missing}')


def main():
    require('client-unity/Assets/Scripts/Game/UI/ScreenId.cs','ResourcePve','Progression')
    require('client-unity/Assets/Scripts/Game/UI/MobileScreenRoot.cs','ScreenId.ResourcePve => "ResourcePve"','ScreenId.Progression => "Progression"')
    require('client-unity/Assets/Editor/MobileSceneBuilder.cs','ScreenId.ResourcePve, "Resource PvE"','ScreenId.Progression, "Advanced Progression"')
    require('client-unity/Assets/Scripts/Game/Bootstrap/MobileGameBootstrap.cs','AdvancedProgressionStore AdvancedProgression','AdvancedProgressionClient(apiConfig)','InitializeAsync(Store.PlayerId)')
    catalog=(ROOT/'client-unity/Assets/Scripts/Game/UI/Production/LiveScreenCatalog.cs').read_text(encoding='utf-8')
    for key in ['shadowarena','arena','guild','shop','quest','events','mail','settings','resourcepve','progression']:
        if f'new Spec("{key}"' not in catalog: raise SystemExit(f'M68 missing screen spec {key}')
    if catalog.count('new Spec(')!=10: raise SystemExit('M68 live screen catalog must contain exactly 10 production specs')
    require('client-unity/Assets/Scripts/Game/UI/Production/ProductionLiveScreenInstaller.cs','ProductionLivePanel','ProductionLegacyScreenBinding','ProductionLiveFeatureBinding','legacy.gameObject.SetActive(false)','ScreenId.ResourcePve','ScreenId.Progression')
    require('client-unity/Assets/Scripts/Game/UI/Production/ProductionLiveFeatureBinding.cs','RecommendedResourcePve','BattleResourcePveAsync','RecommendedTrack','UpgradeAsync','attemptsRemaining','cumulativeBonus')
    require('client-unity/Assets/Scripts/Game/UI/Production/ProductionLegacyScreenBinding.cs','sourceButton.onClick.Invoke()','targetButton.interactable=sourceButton.interactable')
    print('M68_LIVE_SCREENS_OK screens=10 resource_pve=live advanced_progression=live legacy_bridges=mirrored')
    return 0


if __name__=='__main__': raise SystemExit(main())
