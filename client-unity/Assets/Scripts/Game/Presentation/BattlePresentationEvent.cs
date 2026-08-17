using System;

namespace NinjaAssemble.Presentation
{
    [Serializable]
    public sealed class BattlePresentationEvent
    {
        public int Sequence;
        public long TimestampMs;
        public string Type;
        public string ActorId;
        public string TargetId;
        public long Amount;
        public bool Critical;
        public string AbilityId;
        public string AbilityKind;
        public string EffectKey;
        public int RageAfter;
        public string EffectType;
        public string StatusId;
        public long DurationMs;
        public string TriggerId;
    }
}
