using System;

namespace NinjaAssemble.Network
{
    /// <summary>
    /// Promotes the additive server real-time replay into the existing BattleResultDto pipeline.
    /// The legacy projection remains a fallback for older servers/other battle modes.
    /// </summary>
    public static class RealtimeBattleDtoCompatibility
    {
        private const long LegacyTurnDurationMs = 3000L;

        public static PlayBattleDto Promote(PlayBattleDto result)
        {
            if (result == null) return null;

            if (HasReplay(result.realtimeBattle))
                result.battle = Project(result.battle, result.realtimeBattle);

            foreach (CampaignWaveDto wave in result.waves ?? Array.Empty<CampaignWaveDto>())
            {
                if (wave != null && HasReplay(wave.realtimeBattle))
                    wave.battle = Project(wave.battle, wave.realtimeBattle);
            }

            return result;
        }

        public static BattleResultDto Project(BattleResultDto fallback, RealtimeBattleResultDto realtime)
        {
            if (realtime == null) return fallback;

            RealtimeBattleEventDto[] source = realtime.events ?? Array.Empty<RealtimeBattleEventDto>();
            var events = new BattleEventDto[source.Length];
            for (int i = 0; i < source.Length; i++)
            {
                RealtimeBattleEventDto item = source[i];
                if (item == null) continue;
                events[i] = new BattleEventDto
                {
                    sequence = item.sequence,
                    type = item.type,
                    round = ProjectTurns(item.timestampMs),
                    timestampMs = item.timestampMs,
                    actorId = item.actorId,
                    targetId = item.targetId,
                    amount = item.amount,
                    critical = item.critical,
                    abilityId = item.abilityId,
                    abilityKind = item.abilityKind,
                    effectKey = item.effectKey,
                    energyAfter = item.energyAfter,
                    effectType = item.effectType,
                    statusId = item.statusId,
                    durationTurns = ProjectTurns(item.durationMs),
                    durationMs = item.durationMs,
                    triggerId = item.triggerId,
                    realtime = true
                };
            }

            Array.Sort(events, CompareEvents);
            return new BattleResultDto
            {
                seed = realtime.seed,
                rulesetVersion = realtime.rulesetVersion,
                outcome = realtime.outcome,
                rounds = fallback != null && fallback.rounds > 0 ? fallback.rounds : ProjectTurns(realtime.durationMs),
                durationMs = realtime.durationMs,
                events = events,
                realtime = true
            };
        }

        private static bool HasReplay(RealtimeBattleResultDto value) => value?.events != null && value.events.Length > 0;

        private static int CompareEvents(BattleEventDto left, BattleEventDto right)
        {
            if (ReferenceEquals(left, right)) return 0;
            if (left == null) return 1;
            if (right == null) return -1;
            int timestamp = left.timestampMs.CompareTo(right.timestampMs);
            return timestamp != 0 ? timestamp : left.sequence.CompareTo(right.sequence);
        }

        private static int ProjectTurns(long milliseconds)
        {
            if (milliseconds <= 0) return 0;
            long turns = (milliseconds + LegacyTurnDurationMs - 1L) / LegacyTurnDurationMs;
            return turns > int.MaxValue ? int.MaxValue : (int)turns;
        }
    }
}
