#if UNITY_EDITOR
using System;
using System.IO;
using System.Linq;
using UnityEditor;
using UnityEditor.Build;
using UnityEditor.Build.Reporting;
using UnityEngine;

namespace NinjaAssemble.EditorTools
{
    public static class MobileBuildAutomation
    {
        private const string ApplicationId = "com.vianhofico.ninjaassemble";
        private const string ProductName = "Ninja Assemble";

        [MenuItem("Ninja Assemble/Mobile/Build Android Development APK")]
        public static void BuildAndroidDevelopment()
        {
            BuildAndroid(release: false);
        }

        [MenuItem("Ninja Assemble/Mobile/Build Android Release AAB")]
        public static void BuildAndroidRelease()
        {
            BuildAndroid(release: true);
        }

        private static void BuildAndroid(bool release)
        {
            if (EditorUserBuildSettings.activeBuildTarget != BuildTarget.Android)
            {
                throw new BuildFailedException(
                    "Android must be the active build target. In batch mode launch Unity with -buildTarget Android.");
            }

            MobileSceneBuilder.GenerateCompleteSceneShell();
            string[] scenes = EditorBuildSettings.scenes
                .Where(scene => scene.enabled)
                .Select(scene => scene.path)
                .ToArray();
            if (scenes.Length == 0) throw new BuildFailedException("No enabled mobile scenes were generated.");

            ConfigurePlayerSettings();
            ApplyVersionFromEnvironment();

            string buildRoot = Path.GetFullPath(Path.Combine(Application.dataPath, "../../builds/android"));
            Directory.CreateDirectory(buildRoot);
            EditorUserBuildSettings.buildAppBundle = release;
            string extension = release ? ".aab" : ".apk";
            string output = Path.Combine(buildRoot, "NinjaAssemble" + extension);

            var options = new BuildPlayerOptions
            {
                scenes = scenes,
                locationPathName = output,
                target = BuildTarget.Android,
                targetGroup = BuildTargetGroup.Android,
                options = release ? BuildOptions.StrictMode : BuildOptions.Development | BuildOptions.StrictMode
            };

            BuildReport report = BuildPipeline.BuildPlayer(options);
            if (report.summary.result != BuildResult.Succeeded)
            {
                throw new BuildFailedException(
                    $"Android build failed: result={report.summary.result}, errors={report.summary.totalErrors}");
            }

            Debug.Log(
                $"Android {(release ? "release AAB" : "development APK")} built: {output} " +
                $"size={report.summary.totalSize} bytes duration={report.summary.totalTime}");
        }

        private static void ConfigurePlayerSettings()
        {
            PlayerSettings.productName = ProductName;
            PlayerSettings.companyName = "vianhofico";
            PlayerSettings.SetApplicationIdentifier(NamedBuildTarget.Android, ApplicationId);
            PlayerSettings.SetScriptingBackend(NamedBuildTarget.Android, ScriptingImplementation.IL2CPP);
            PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64;
            PlayerSettings.defaultInterfaceOrientation = UIOrientation.LandscapeLeft;
            PlayerSettings.allowedAutorotateToLandscapeLeft = true;
            PlayerSettings.allowedAutorotateToLandscapeRight = true;
            PlayerSettings.allowedAutorotateToPortrait = false;
            PlayerSettings.allowedAutorotateToPortraitUpsideDown = false;
        }

        private static void ApplyVersionFromEnvironment()
        {
            string version = Environment.GetEnvironmentVariable("NINJA_BUILD_VERSION");
            if (!string.IsNullOrWhiteSpace(version)) PlayerSettings.bundleVersion = version.Trim();

            string code = Environment.GetEnvironmentVariable("NINJA_ANDROID_VERSION_CODE");
            if (!string.IsNullOrWhiteSpace(code))
            {
                int parsed;
                if (!int.TryParse(code, out parsed) || parsed <= 0)
                    throw new BuildFailedException("NINJA_ANDROID_VERSION_CODE must be a positive integer.");
                PlayerSettings.Android.bundleVersionCode = parsed;
            }
        }
    }
}
#endif
