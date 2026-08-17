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

            Array.Sort(battle.events, (a, b) =>
            {
                int time = a.timestampMs.CompareTo(b.timestampMs);
                return time != 0 ? time : a.sequence.CompareTo(b.sequence);
            });
            foreach (BattleEventDto item in battle.events)
            {
                if (item == null) continue;
                result.Add(new BattlePresentationEvent
                {
                    Sequence = item.sequence,
                    TimestampMs = item.timestampMs,
                    Type = item.type ?? string.Empty,
                    ActorId = item.actorId ?? string.Empty,
                    TargetId = item.targetId ?? string.Empty,
                    Amount = item.amount,
                    Critical = item.critical,
                    AbilityId = item.abilityId ?? string.Empty,
                    AbilityKind = item.abilityKind ?? string.Empty,
                    EffectKey = item.effectKey ?? string.Empty,
                    RageAfter = item.rageAfter,
                    EffectType = item.effectType ?? string.Empty,
                    StatusId = item.statusId ?? string.Empty,
                    DurationMs = item.durationMs,
                    TriggerId = item.triggerId ?? string.Empty
                });
            }
            return result;
        }
    }
}
