#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT / "client-unity"


def main() -> int:
    errors: list[str] = []
    version_path = PROJECT / "ProjectSettings/ProjectVersion.txt"
    version = version_path.read_text(encoding="utf-8") if version_path.exists() else ""
    if "6000.0." not in version:
        errors.append("Unity project must remain on the pinned 6000.0 editor line")

    manifest_path = PROJECT / "Packages/manifest.json"
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except Exception as exc:
        errors.append(f"invalid Unity package manifest: {exc}")
        manifest = {"dependencies": {}}
    dependencies = manifest.get("dependencies", {})
    for package in ("com.unity.addressables", "com.unity.textmeshpro", "com.unity.test-framework"):
        if package not in dependencies:
            errors.append(f"required Unity package missing: {package}")

    build_path = PROJECT / "Assets/Editor/MobileBuildAutomation.cs"
    source = build_path.read_text(encoding="utf-8") if build_path.exists() else ""
    required_tokens = (
        "BuildAndroidDevelopment",
        "BuildAndroidRelease",
        "BuildPipeline.BuildPlayer",
        "BuildTarget.Android",
        "NamedBuildTarget.Android",
        "ScriptingImplementation.IL2CPP",
        "AndroidArchitecture.ARM64",
        "MobileSceneBuilder.GenerateCompleteSceneShell",
        "BuildOptions.StrictMode",
    )
    for token in required_tokens:
        if token not in source:
            errors.append(f"MobileBuildAutomation missing required build contract: {token}")

    build_script = ROOT / "scripts/build-mobile.sh"
    if not build_script.exists():
        errors.append("scripts/build-mobile.sh is missing")
    else:
        shell = build_script.read_text(encoding="utf-8")
        for token in ("-batchmode", "-buildTarget Android", "-executeMethod"):
            if token not in shell:
                errors.append(f"build-mobile.sh missing {token}")

    if errors:
        print("MOBILE_BUILD_SOURCE_INVALID", file=sys.stderr)
        for error in errors:
            print(" -", error, file=sys.stderr)
        return 1

    print("MOBILE_BUILD_SOURCE_OK unity=6000.0 android=ARM64 backend=IL2CPP scene_generation=enabled")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
