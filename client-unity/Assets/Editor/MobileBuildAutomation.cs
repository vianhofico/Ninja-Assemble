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
                    "Android must be the active build target. In batch mode launch Unity/GameCI with targetPlatform Android.");
            }

            MobileSceneBuilder.GenerateCompleteSceneShell();
            string[] scenes = EditorBuildSettings.scenes
                .Where(scene => scene.enabled)
                .Select(scene => scene.path)
                .ToArray();
            if (scenes.Length == 0) throw new BuildFailedException("No enabled mobile scenes were generated.");

            ConfigurePlayerSettings();
            ApplyVersion();
            ConfigureSigning(release);

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
            WriteBuildMetadata(report, release, output, scenes);
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

        private static void ApplyVersion()
        {
            string version = FirstNonBlank(
                Environment.GetEnvironmentVariable("NINJA_BUILD_VERSION"),
                CommandLineValue("buildVersion"),
                CommandLineValue("ninjaBuildVersion"));
            if (!string.IsNullOrWhiteSpace(version)) PlayerSettings.bundleVersion = version.Trim();

            string code = FirstNonBlank(
                Environment.GetEnvironmentVariable("NINJA_ANDROID_VERSION_CODE"),
                CommandLineValue("androidVersionCode"),
                CommandLineValue("ninjaAndroidVersionCode"));
            if (!string.IsNullOrWhiteSpace(code))
            {
                int parsed;
                if (!int.TryParse(code, out parsed) || parsed <= 0)
                    throw new BuildFailedException("Android versionCode must be a positive integer.");
                PlayerSettings.Android.bundleVersionCode = parsed;
            }
        }

        private static void ConfigureSigning(bool release)
        {
            if (!release)
            {
                PlayerSettings.Android.useCustomKeystore = false;
                return;
            }

            string configuredPath = RequireValue("Android keystore",
                Environment.GetEnvironmentVariable("NINJA_ANDROID_KEYSTORE_RELATIVE"),
                CommandLineValue("androidKeystoreName"));
            string projectRoot = Path.GetFullPath(Path.Combine(Application.dataPath, ".."));
            string keystore = Path.IsPathRooted(configuredPath)
                ? Path.GetFullPath(configuredPath)
                : Path.GetFullPath(Path.Combine(projectRoot, configuredPath));
            string rootPrefix = projectRoot.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar) + Path.DirectorySeparatorChar;
            if (!keystore.StartsWith(rootPrefix, StringComparison.OrdinalIgnoreCase))
                throw new BuildFailedException("Release keystore must be materialized inside the Unity project workspace.");
            if (!File.Exists(keystore)) throw new BuildFailedException("Release keystore file does not exist: " + keystore);

            PlayerSettings.Android.useCustomKeystore = true;
            PlayerSettings.Android.keystoreName = keystore;
            PlayerSettings.Android.keystorePass = RequireValue("Android keystore password",
                Environment.GetEnvironmentVariable("NINJA_ANDROID_KEYSTORE_PASS"),
                CommandLineValue("androidKeystorePass"));
            PlayerSettings.Android.keyaliasName = RequireValue("Android key alias",
                Environment.GetEnvironmentVariable("NINJA_ANDROID_KEYALIAS_NAME"),
                CommandLineValue("androidKeyaliasName"));
            PlayerSettings.Android.keyaliasPass = RequireValue("Android key alias password",
                Environment.GetEnvironmentVariable("NINJA_ANDROID_KEYALIAS_PASS"),
                CommandLineValue("androidKeyaliasPass"));
        }

        private static string RequireValue(string label, params string[] values)
        {
            string value = FirstNonBlank(values);
            if (string.IsNullOrWhiteSpace(value)) throw new BuildFailedException(label + " is required for release Android signing.");
            return value.Trim();
        }

        private static string FirstNonBlank(params string[] values)
        {
            if (values == null) return null;
            foreach (string value in values)
                if (!string.IsNullOrWhiteSpace(value)) return value;
            return null;
        }

        private static string CommandLineValue(string key)
        {
            string[] args = Environment.GetCommandLineArgs();
            string expected = "-" + key;
            for (int i = 0; i < args.Length; i++)
            {
                string arg = args[i] ?? string.Empty;
                if (string.Equals(arg, expected, StringComparison.OrdinalIgnoreCase))
                    return i + 1 < args.Length ? args[i + 1] : string.Empty;
                string prefix = expected + "=";
                if (arg.StartsWith(prefix, StringComparison.OrdinalIgnoreCase)) return arg.Substring(prefix.Length);
            }
            return null;
        }

        private static void WriteBuildMetadata(BuildReport report, bool release, string output, string[] scenes)
        {
            string buildRoot = Path.GetDirectoryName(output) ?? throw new BuildFailedException("Invalid Android build output path.");
            string metadataPath = Path.Combine(buildRoot, "build-metadata.json");
            string commit = FirstNonBlank(
                Environment.GetEnvironmentVariable("NINJA_BUILD_COMMIT"),
                CommandLineValue("ninjaBuildCommit"),
                Environment.GetEnvironmentVariable("GITHUB_SHA")) ?? string.Empty;
            var metadata = new BuildMetadata
            {
                artifactType = release ? "AAB" : "APK",
                result = report.summary.result.ToString(),
                unityVersion = Application.unityVersion,
                applicationId = ApplicationId,
                bundleVersion = PlayerSettings.bundleVersion,
                versionCode = PlayerSettings.Android.bundleVersionCode,
                gitCommit = commit,
                outputFile = Path.GetFileName(output),
                totalSizeBytes = report.summary.totalSize.ToString(),
                durationSeconds = report.summary.totalTime.TotalSeconds,
                sceneCount = scenes.Length,
                builtAtUtc = DateTime.UtcNow.ToString("O")
            };
            File.WriteAllText(metadataPath, JsonUtility.ToJson(metadata, true));
        }

        [Serializable]
        private sealed class BuildMetadata
        {
            public string artifactType;
            public string result;
            public string unityVersion;
            public string applicationId;
            public string bundleVersion;
            public int versionCode;
            public string gitCommit;
            public string outputFile;
            public string totalSizeBytes;
            public double durationSeconds;
            public int sceneCount;
            public string builtAtUtc;
        }
    }
}
#endif
