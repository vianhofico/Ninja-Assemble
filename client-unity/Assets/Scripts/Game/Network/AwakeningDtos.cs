using System;

namespace NinjaAssemble.Network
{
    [Serializable]
    public sealed class AwakeningViewDto
    {
        public OwnedHeroDto hero;
        public bool available;
        public bool awakened;
        public bool changed;
        public string awakeningId;
        public string awakeningName;
        public AwakeningVisualDto visual;
    }

    [Serializable]
    public sealed class AwakeningVisualDto
    {
        public string awakeningId;
        public string heroId;
        public string transitionStart;
        public string transitionMid;
        public string transitionEnd;
        public string idleAnimation;
        public string movementAnimation;
        public string basicVfxModifier;
        public string skill1VfxModifier;
        public string skill2VfxModifier;
        public string ultimateVfxModifier;
        public string awakeningSkillVfx;
        public string cameraSequence;
        public string screenEffect;
        public string sfxDescription;
        public string referenceSource;
        public string status;
    }
}
