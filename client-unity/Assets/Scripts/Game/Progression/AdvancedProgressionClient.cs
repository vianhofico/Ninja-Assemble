using System;
using System.Text;
using System.Threading.Tasks;
using NinjaAssemble.Network;
using UnityEngine;
using UnityEngine.Networking;
namespace NinjaAssemble.Progression
{
    public sealed class AdvancedProgressionClient
    {
        private readonly string baseUrl;
        public AdvancedProgressionClient(GameApiConfig config){if(config==null||string.IsNullOrWhiteSpace(config.BaseUrl))throw new ArgumentException("API config/base URL required",nameof(config));baseUrl=config.BaseUrl;}
        public Task<AdvancedProgressionBoardDto> GetBoardAsync(string playerId)=>GetAsync<AdvancedProgressionBoardDto>($"/api/v1/play/{Escape(playerId)}/progression/advanced");
        public Task<AdvancedProgressionUpgradeDto> UpgradeAsync(string playerId,string trackId,string requestId)=>PostJsonAsync<AdvancedProgressionUpgradeDto>($"/api/v1/play/{Escape(playerId)}/progression/advanced/{Escape(trackId)}/upgrade",JsonUtility.ToJson(new UpgradeRequest{requestId=requestId}));
        private async Task<T> GetAsync<T>(string path){using UnityWebRequest request=UnityWebRequest.Get(baseUrl+path);await Send(request);return JsonUtility.FromJson<T>(request.downloadHandler.text);}
        private async Task<T> PostJsonAsync<T>(string path,string json){using UnityWebRequest request=new UnityWebRequest(baseUrl+path,UnityWebRequest.kHttpVerbPOST);request.uploadHandler=new UploadHandlerRaw(Encoding.UTF8.GetBytes(json));request.downloadHandler=new DownloadHandlerBuffer();request.SetRequestHeader("Content-Type","application/json");await Send(request);return JsonUtility.FromJson<T>(request.downloadHandler.text);}
        private static async Task Send(UnityWebRequest request){if(ApiAuthSession.HasToken)request.SetRequestHeader("Authorization","Bearer "+ApiAuthSession.BearerToken);UnityWebRequestAsyncOperation op=request.SendWebRequest();while(!op.isDone)await Task.Yield();if(request.result!=UnityWebRequest.Result.Success)throw new InvalidOperationException($"HTTP {(long)request.responseCode}: {request.error} :: {request.downloadHandler?.text}");}
        private static string Escape(string value)=>UnityWebRequest.EscapeURL(value??string.Empty);
        [Serializable] private sealed class UpgradeRequest{public string requestId;}
    }
}
