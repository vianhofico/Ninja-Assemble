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
    }
}
