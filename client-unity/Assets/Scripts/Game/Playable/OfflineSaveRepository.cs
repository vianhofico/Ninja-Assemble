using System;
using System.IO;
using UnityEngine;

namespace NinjaAssemble.Playable
{
    public sealed class OfflineSaveRepository
    {
        public const int CurrentVersion = 1;
        private readonly string path;
        public string Path => path;

        public OfflineSaveRepository(string explicitPath = null)
        {
            path = string.IsNullOrWhiteSpace(explicitPath)
                ? System.IO.Path.Combine(Application.persistentDataPath, "ninjaassemble-offline-save.json")
                : explicitPath;
        }

        public OfflineSaveData LoadOrCreate()
        {
            try
            {
                if (!File.Exists(path)) return CreateSeed();
                string json = File.ReadAllText(path);
                OfflineSaveData data = JsonUtility.FromJson<OfflineSaveData>(json);
                if (data == null || data.saveVersion != CurrentVersion)
                {
                    BackupExisting();
                    return CreateSeed();
                }
                Normalize(data);
                return data;
            }
            catch (Exception exception)
            {
                Debug.LogWarning("Offline save could not be loaded; a fresh playtest profile will be used. " + exception.Message);
                BackupExisting();
                return CreateSeed();
            }
        }

        public void Save(OfflineSaveData data)
        {
            if (data == null) throw new ArgumentNullException(nameof(data));
            data.saveVersion = CurrentVersion;
            Normalize(data);
            string directory = System.IO.Path.GetDirectoryName(path);
            if (!string.IsNullOrWhiteSpace(directory)) Directory.CreateDirectory(directory);
            string temp = path + ".tmp";
            File.WriteAllText(temp, JsonUtility.ToJson(data, true));
            if (File.Exists(path)) File.Delete(path);
            File.Move(temp, path);
        }

        public void Reset()
        {
            if (File.Exists(path)) File.Delete(path);
        }

        private OfflineSaveData CreateSeed()
        {
            OfflineSaveData data = OfflineSeedFactory.Create();
            Save(data);
            return data;
        }

        private void BackupExisting()
        {
            try
            {
                if (!File.Exists(path)) return;
                File.Copy(path, path + ".bak", true);
            }
            catch (Exception exception)
            {
                Debug.LogWarning("Offline save backup failed: " + exception.Message);
            }
        }

        private static void Normalize(OfflineSaveData data)
        {
            data.heroes = data.heroes ?? Array.Empty<NinjaAssemble.Network.OwnedHeroDto>();
            data.formationIds = data.formationIds ?? Array.Empty<string>();
            data.clearedStageIds = data.clearedStageIds ?? Array.Empty<string>();
            data.claimedQuestIds = data.claimedQuestIds ?? Array.Empty<string>();
            data.claimedMailIds = data.claimedMailIds ?? Array.Empty<string>();
        }
    }
}
