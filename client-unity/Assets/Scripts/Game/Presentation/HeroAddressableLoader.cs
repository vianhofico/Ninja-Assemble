using System.Threading.Tasks;
using UnityEngine;
using UnityEngine.AddressableAssets;
using UnityEngine.ResourceManagement.AsyncOperations;

namespace NinjaAssemble.Presentation
{
    public sealed class HeroAddressableLoader
    {
        public async Task<Sprite> LoadPortraitAsync(string address)
        {
            return await Load<Sprite>(address);
        }

        public async Task<GameObject> LoadPrefabAsync(string address)
        {
            return await Load<GameObject>(address);
        }

        public async Task<GameObject> TryLoadPrefabAsync(string address)
        {
            if (string.IsNullOrWhiteSpace(address)) return null;
            AsyncOperationHandle<GameObject> handle = Addressables.LoadAssetAsync<GameObject>(address);
            await handle.Task;
            if (handle.Status != AsyncOperationStatus.Succeeded || handle.Result == null)
            {
                if (handle.IsValid()) Addressables.Release(handle);
                return null;
            }
            return handle.Result;
        }

        private static async Task<T> Load<T>(string address) where T : Object
        {
            if (string.IsNullOrWhiteSpace(address)) throw new System.ArgumentException("Address is required", nameof(address));
            AsyncOperationHandle<T> handle = Addressables.LoadAssetAsync<T>(address);
            T asset = await handle.Task;
            if (handle.Status != AsyncOperationStatus.Succeeded || asset == null)
                throw new System.InvalidOperationException($"Addressable load failed: {address}");
            return asset;
        }
    }
}
