using System;

namespace NinjaAssemble.Presentation
{
    [Serializable]
    public sealed class BattlePresentationEvent
    {
        public long Sequence;
        public string Type;
        public int Round;
        public string ActorId;
        public string TargetId;
        public long Amount;
        public bool Critical;
        public string AbilityId;
        public string AbilityKind;
        public string EffectKey;
        public int EnergyAfter;
        public string EffectType;
        public string StatusId;
        public int DurationTurns;
        public string TriggerId;
    }
}
