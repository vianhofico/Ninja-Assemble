using System;
using System.Collections.Generic;
using NinjaAssemble.Network;

namespace NinjaAssemble.Presentation
{
    public static class BattlePresentationAdapter
    {
        public static List<BattlePresentationEvent> From(BattleResultDto battle)
        {
            if (battle == null) throw new ArgumentNullException(nameof(battle));
            var result = new List<BattlePresentationEvent>();
            if (battle.events == null) return result;

            Array.Sort(battle.events, (a, b) => a.sequence.CompareTo(b.sequence));
            foreach (BattleEventDto item in battle.events)
            {
                if (item == null) continue;
                result.Add(new BattlePresentationEvent
                {
                    Sequence = item.sequence,
                    Type = item.type ?? string.Empty,
                    Round = item.round,
                    ActorId = item.actorId ?? string.Empty,
                    TargetId = item.targetId ?? string.Empty,
                    Amount = item.amount,
                    Critical = item.critical
                });
            }
            return result;
        }
    }
}
