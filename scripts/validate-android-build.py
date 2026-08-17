#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> str:
    file = ROOT / path
    if not file.exists():
        raise SystemExit(f"ANDROID_BUILD_INVALID missing={path}")
    text = file.read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"ANDROID_BUILD_INVALID {path} missing={missing}")
    return text


def main() -> int:
    version = require("client-unity/ProjectSettings/ProjectVersion.txt", "m_EditorVersion: 6000.0.42f1")
    automation = require(
        "client-unity/Assets/Editor/MobileBuildAutomation.cs",
        "BuildAndroidDevelopment", "BuildAndroidRelease", "BuildTarget.Android",
        "ScriptingImplementation.IL2CPP", "AndroidArchitecture.ARM64",
        '".apk"', '".aab"', "ConfigureSigning", "CommandLineValue",
        "build-metadata.json", "ninjaBuildCommit")
    shell = require(
        "scripts/build-mobile.sh",
        "BuildAndroidDevelopment", "BuildAndroidRelease", "-buildTarget Android")
    workflow = require(
        ".github/workflows/android-playtest-build.yml",
        "game-ci/unity-builder@v5", "targetPlatform: Android", "unityVersion: auto",
        "BuildAndroidDevelopment", "BuildAndroidRelease",
        "androidExportType: androidPackage", "androidExportType: androidAppBundle",
        "NinjaAssemble.apk", "NinjaAssemble.aab", "actions/upload-artifact@v4",
        "ANDROID_KEYSTORE_BASE64", "ANDROID_KEYSTORE_PASS", "ANDROID_KEYALIAS_NAME", "ANDROID_KEYALIAS_PASS")

    if "useCustomKeystore = false" not in automation:
        raise SystemExit("ANDROID_BUILD_INVALID development build must explicitly disable custom release keystore")
    if "BuildOptions.Development | BuildOptions.StrictMode" not in automation:
        raise SystemExit("ANDROID_BUILD_INVALID development APK lost Development|StrictMode")
    if "release ? BuildOptions.StrictMode" not in automation:
        raise SystemExit("ANDROID_BUILD_INVALID release AAB lost StrictMode")
    if "workflow_dispatch" not in workflow or "pull_request" not in workflow:
        raise SystemExit("ANDROID_BUILD_INVALID expected PR APK and manual release lanes")
    if "release-aab" not in workflow or "development-apk" not in workflow:
        raise SystemExit("ANDROID_BUILD_INVALID workflow lanes missing")

    print("ANDROID_BUILD_OK unity=6000.0.42f1 apk=pr aab=manual-signed il2cpp=1 arm64=1 metadata=1 gameci=v5")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
