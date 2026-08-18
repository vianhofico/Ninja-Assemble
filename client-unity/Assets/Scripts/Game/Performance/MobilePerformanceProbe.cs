using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using UnityEngine;
using UnityEngine.Profiling;

namespace NinjaAssemble.Performance
{
    /// <summary>Presentation-only benchmark probe. It records local metrics but never promotes release evidence.</summary>
    public sealed class MobilePerformanceProbe : MonoBehaviour
    {
        [SerializeField] private float warmupSeconds = 30f;
        [SerializeField] private float measurementSeconds = 180f;
        [SerializeField] private string scenarioId = "campaign-realtime-battle-rage-v1";

        private readonly List<float> frameTimesMs = new List<float>(20000);
        private float elapsed;
        private float measured;
        private long peakMemoryBytes;
        private bool completed;

        public event Action<BenchmarkResult> Completed;

        private void Update()
        {
            if (completed) return;
            elapsed += Time.unscaledDeltaTime;
            if (elapsed < warmupSeconds) return;
            float delta = Mathf.Max(0.000001f, Time.unscaledDeltaTime);
            measured += delta;
            frameTimesMs.Add(delta * 1000f);
            peakMemoryBytes = Math.Max(peakMemoryBytes, Profiler.GetTotalAllocatedMemoryLong());
            if (measured >= measurementSeconds) Complete();
        }

        public void RestartProbe()
        {
            elapsed = 0f; measured = 0f; peakMemoryBytes = 0L; completed = false; frameTimesMs.Clear();
        }

        private void Complete()
        {
            completed = true;
            float[] ordered = frameTimesMs.OrderBy(value => value).ToArray();
            double averageFrameMs = ordered.Length == 0 ? 0d : ordered.Average(value => (double)value);
            double averageFps = averageFrameMs <= 0d ? 0d : 1000d / averageFrameMs;
            double p95FrameMs = ordered.Length == 0 ? 0d : ordered[Mathf.Clamp(Mathf.CeilToInt(ordered.Length * 0.95f) - 1, 0, ordered.Length - 1)];
            var result = new BenchmarkResult(
                "m76-probe-v1", scenarioId, SystemInfo.deviceModel, SystemInfo.operatingSystem,
                SystemInfo.graphicsDeviceName, Application.unityVersion, averageFps, p95FrameMs,
                peakMemoryBytes / (1024d * 1024d), measured, ordered.Length, DateTimeOffset.UtcNow.ToString("O"));
            string output = Path.Combine(Application.persistentDataPath, "m76-performance-probe.json");
            File.WriteAllText(output, JsonUtility.ToJson(result, true));
            Debug.Log($"M76 performance probe complete: {output}");
            Completed?.Invoke(result);
        }

        [Serializable]
        public sealed class BenchmarkResult
        {
            public string schemaVersion; public string scenarioId; public string deviceModel; public string operatingSystem;
            public string graphicsDevice; public string unityVersion; public double averageFps; public double p95FrameMs;
            public double peakMemoryMb; public float measuredSeconds; public int sampledFrames; public string capturedAt;
            public BenchmarkResult(string version,string scenario,string model,string os,string gpu,string unity,double fps,double p95,double memory,float seconds,int frames,string captured)
            { schemaVersion=version; scenarioId=scenario; deviceModel=model; operatingSystem=os; graphicsDevice=gpu; unityVersion=unity; averageFps=fps; p95FrameMs=p95; peakMemoryMb=memory; measuredSeconds=seconds; sampledFrames=frames; capturedAt=captured; }
        }
    }
}
