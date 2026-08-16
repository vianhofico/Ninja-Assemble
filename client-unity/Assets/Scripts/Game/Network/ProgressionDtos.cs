using System;

namespace NinjaAssemble.Network
{
    [Serializable]
    public sealed class EvolutionPathDto
    {
        public string characterId;
        public string targetVariant;
        public string prerequisiteVariant;
        public int minLevel;
        public string minFrame;
        public long goldCost;
        public string status;
        public string profileVersion;
    }

    [Serializable]
    public sealed class FrameAdvanceResultDto
    {
        public string playerHeroId;
        public string frameTier;
        public int frameStep;
        public long goldCost;
        public string profileVersion;
    }

    [Serializable]
    public sealed class EvolutionResultDto
    {
        public string characterId;
        public string targetVariant;
        public long goldCost;
        public bool alreadyUnlocked;
        public string profileVersion;
    }

    [Serializable] public sealed class EvolutionRequestDto { public string requestId; public string targetVariant; }
}
