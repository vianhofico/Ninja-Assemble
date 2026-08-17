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

            Array.Sort(battle.events, CompareEvents);
            foreach (BattleEventDto item in battle.events)
            {
                if (item == null) continue;
                result.Add(new BattlePresentationEvent
                {
                    Sequence = item.sequence,
                    Type = item.type ?? string.Empty,
                    Round = item.round,
                    TimestampMs = item.timestampMs,
                    ActorId = item.actorId ?? string.Empty,
                    TargetId = item.targetId ?? string.Empty,
                    Amount = item.amount,
                    Critical = item.critical,
                    AbilityId = item.abilityId ?? string.Empty,
                    AbilityKind = item.abilityKind ?? string.Empty,
                    EffectKey = item.effectKey ?? string.Empty,
                    EnergyAfter = item.energyAfter,
                    EffectType = item.effectType ?? string.Empty,
                    StatusId = item.statusId ?? string.Empty,
                    DurationTurns = item.durationTurns,
                    DurationMs = item.durationMs,
                    TriggerId = item.triggerId ?? string.Empty,
                    IsRealtime = battle.realtime || item.realtime
                });
            }
            return result;
        }

        private static int CompareEvents(BattleEventDto left, BattleEventDto right)
        {
            if (ReferenceEquals(left, right)) return 0;
            if (left == null) return 1;
            if (right == null) return -1;
            if (left.realtime || right.realtime)
            {
                int timestamp = left.timestampMs.CompareTo(right.timestampMs);
                if (timestamp != 0) return timestamp;
            }
            return left.sequence.CompareTo(right.sequence);
        }
    }
}
