using System;
using System.Linq;
using System.Threading.Tasks;
namespace NinjaAssemble.Progression
{
    public sealed class AdvancedProgressionStore
    {
        private readonly AdvancedProgressionClient api;
        public string PlayerId{get;private set;} public AdvancedProgressionBoardDto Board{get;private set;}
        public AdvancedProgressionTrackDto RecommendedTrack=>(Board?.tracks??Array.Empty<AdvancedProgressionTrackDto>()).FirstOrDefault(track=>track.affordable&&!track.maxed);
        public AdvancedProgressionStore(AdvancedProgressionClient api)=>this.api=api??throw new ArgumentNullException(nameof(api));
        public async Task InitializeAsync(string playerId){if(string.IsNullOrWhiteSpace(playerId))throw new ArgumentException("playerId is required",nameof(playerId));PlayerId=playerId;await RefreshAsync();}
        public async Task RefreshAsync(){if(string.IsNullOrWhiteSpace(PlayerId))throw new InvalidOperationException("AdvancedProgressionStore is not initialized");Board=await api.GetBoardAsync(PlayerId);}
        public async Task<AdvancedProgressionUpgradeDto> UpgradeAsync(string trackId){if(string.IsNullOrWhiteSpace(trackId))throw new ArgumentException("trackId is required",nameof(trackId));AdvancedProgressionUpgradeDto result=await api.UpgradeAsync(PlayerId,trackId,Guid.NewGuid().ToString());await RefreshAsync();return result;}
    }
}
