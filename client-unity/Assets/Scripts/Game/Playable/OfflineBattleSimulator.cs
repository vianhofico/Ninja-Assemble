using System;
using System.Collections.Generic;
using System.Linq;
using NinjaAssemble.Network;

namespace NinjaAssemble.Playable
{
    public static class OfflineBattleSimulator
    {
        public static PlayBattleDto Simulate(string stageId, OwnedHeroDto[] formation, long seed, int energyCost, long goldReward, long diamondReward)
        {
            OwnedHeroDto[] team = (formation ?? Array.Empty<OwnedHeroDto>()).Take(5).ToArray();
            if (team.Length != 5) throw new InvalidOperationException("Offline battle requires a five-ninja formation");

            var participants = new List<BattleParticipantDto>();
            for (int i = 0; i < 5; i++) participants.Add(ToParticipant(team[i], "A", i));
            for (int i = 0; i < 5; i++) participants.Add(BotParticipant(i, Math.Max(1, team[i].level - 2)));

            var random = new Random(unchecked((int)(seed ^ (seed >> 32))));
            var events = new List<BattleEventDto>();
            long timestamp = 100;
            int sequence = 1;

            for (int round = 0; round < 3; round++)
            {
                for (int slot = 0; slot < 5; slot++)
                {
                    BattleParticipantDto actor = participants[slot];
                    BattleParticipantDto target = participants[5 + slot];
                    long damage = 900 + random.Next(0, 450) + actor.level * 35L;
                    events.Add(Start(sequence++, timestamp, actor.battleUnitId, round == 2 ? "RAGE_SKILL" : "BASIC", round == 2 ? 1000 : 250));
                    timestamp += 80;
                    events.Add(new BattleEventDto { sequence = sequence++, timestampMs = timestamp, type = "DAMAGE", actorId = actor.battleUnitId, targetId = target.battleUnitId, amount = damage, critical = random.NextDouble() < 0.2, abilityId = round == 2 ? "offline-rage" : "offline-basic", abilityKind = round == 2 ? "RAGE_SKILL" : "BASIC", effectKey = "hit" });
                    timestamp += 70;
                    events.Add(new BattleEventDto { sequence = sequence++, timestampMs = timestamp, type = "RAGE_GAIN", actorId = actor.battleUnitId, rageAfter = Math.Min(1000, (round + 1) * 350) });
                    timestamp += 120;
                }
            }

            for (int slot = 0; slot < 5; slot++)
            {
                BattleParticipantDto target = participants[5 + slot];
                events.Add(new BattleEventDto { sequence = sequence++, timestampMs = timestamp, type = "KO", actorId = participants[slot].battleUnitId, targetId = target.battleUnitId, amount = 0, effectKey = "ko" });
                timestamp += 80;
            }

            var battle = new BattleResultDto
            {
                seed = seed,
                rulesetVersion = "offline-deterministic-v1",
                outcome = "TEAM_A",
                durationMs = timestamp,
                events = events.ToArray()
            };

            return new PlayBattleDto
            {
                battleId = "offline-" + Math.Abs(seed),
                stageId = stageId,
                campaignCatalogVersion = "offline-playtest-v1",
                waveRulesVersion = "offline-wave-v1",
                energyCost = energyCost,
                stars = 3,
                firstClear = false,
                playerExpReward = 100,
                goldReward = goldReward,
                diamondReward = diamondReward,
                accountLevelAfter = 20,
                itemRewards = Array.Empty<InventoryRewardItemDto>(),
                combatStatsVersion = "offline-stats-v1",
                abilityProfileVersion = "offline-ability-v1",
                techniqueMappingVersion = "offline-technique-v1",
                passiveProfileVersion = "offline-passive-v1",
                waves = Array.Empty<CampaignWaveDto>(),
                participants = participants.ToArray(),
                battle = battle
            };
        }

        public static long StableSeed(string value, IEnumerable<string> ids, int counter = 0)
        {
            unchecked
            {
                long hash = 1469598103934665603L;
                foreach (char c in value ?? string.Empty) { hash ^= c; hash *= 1099511628211L; }
                foreach (string id in ids ?? Array.Empty<string>())
                    foreach (char c in id ?? string.Empty) { hash ^= c; hash *= 1099511628211L; }
                hash ^= counter;
                hash *= 1099511628211L;
                return hash == long.MinValue ? long.MaxValue : Math.Abs(hash);
            }
        }

        private static BattleParticipantDto ToParticipant(OwnedHeroDto hero, string side, int slot)
        {
            return new BattleParticipantDto
            {
                battleUnitId = "offline-A-" + slot,
                characterId = hero.characterId,
                heroId = hero.heroId,
                awakened = hero.awakened,
                awakeningId = hero.awakeningId,
                presentationKey = hero.heroId,
                displayName = hero.displayName,
                variant = hero.currentVariant,
                level = hero.level,
                side = side,
                slot = slot,
                maxHp = 6000 + hero.level * 400L
            };
        }

        private static BattleParticipantDto BotParticipant(int slot, int level)
        {
            return new BattleParticipantDto
            {
                battleUnitId = "offline-B-" + slot,
                characterId = "training-opponent-" + slot,
                heroId = string.Empty,
                awakened = false,
                presentationKey = "offline-training",
                displayName = "Training Ninja " + (slot + 1),
                variant = "BASE",
                level = level,
                side = "B",
                slot = slot,
                maxHp = 4500 + level * 320L
            };
        }

        private static BattleEventDto Start(int sequence, long timestamp, string actorId, string abilityKind, int rageAfter)
        {
            return new BattleEventDto
            {
                sequence = sequence,
                timestampMs = timestamp,
                type = abilityKind == "RAGE_SKILL" ? "RAGE_SKILL_CAST_START" : "BASIC_ATTACK_START",
                actorId = actorId,
                abilityId = abilityKind == "RAGE_SKILL" ? "offline-rage" : "offline-basic",
                abilityKind = abilityKind,
                effectKey = abilityKind == "RAGE_SKILL" ? "rage" : "attack",
                rageAfter = rageAfter
            };
        }
    }
}
