using UnityEngine;

namespace NinjaAssemble.Network
{
    [CreateAssetMenu(menuName = "Ninja Assemble/API Config", fileName = "GameApiConfig")]
    public sealed class GameApiConfig : ScriptableObject
    {
        [SerializeField] private string baseUrl = "http://127.0.0.1:8080";
        public string BaseUrl => (baseUrl ?? string.Empty).TrimEnd('/');
    }
}
