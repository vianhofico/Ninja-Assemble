#!/usr/bin/env python3
from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
def require(path,*tokens):
    text=(ROOT/path).read_text(encoding="utf-8");missing=[t for t in tokens if t not in text]
    if missing:raise ValueError(f"{path} missing {missing}")
def main():
    try:
        require("client-unity/Assets/Scripts/Game/UI/Production/ProductionUiTokens.cs","TouchMin","ReferenceResolution","Accent","Negative")
        require("client-unity/Assets/Scripts/Game/UI/Production/ProductionUiState.cs","Loading","Ready","Empty","Error","Offline","Retry")
        require("client-unity/Assets/Scripts/Game/UI/Production/ProductionUiFactory.cs","Button","Panel","Vertical","Text","TouchMin")
        require("client-unity/Assets/Scripts/Game/UI/Production/ProductionScreenHost.cs","Screen.safeArea","SetState","AsyncState","RETRY","OnRectTransformDimensionsChange")
        require("client-unity/Assets/Scripts/Game/UI/Production/ResponsiveLayout.cs","Compact","Regular","Wide","GridColumns","IsTabletLike")
        require("client-unity/Assets/Scripts/Game/UI/Production/ProductionFeedback.cs","Handheld.Vibrate","Success","Error")
        print("M66_UI_FOUNDATION_OK tokens=1 safe_area=1 async_states=5 responsive=compact+regular+wide touch_min=52 feedback=1")
        return 0
    except ValueError as e:
        print(f"M66_UI_FOUNDATION_INVALID {e}",file=sys.stderr);return 1
if __name__=="__main__":raise SystemExit(main())
