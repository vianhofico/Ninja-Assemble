using System.Linq;
using NinjaAssemble.Network;
using NinjaAssemble.Playable;
using NUnit.Framework;

namespace NinjaAssemble.Tests.Editor
{
    public sealed class OfflineBattleSimulatorTests
    {
        [Test]
        public void SameSeedAndFormation_ProduceSameReplayContract()
        {
            OwnedHeroDto[] heroes = OfflineSeedFactory.Create().heroes.Take(5).ToArray();
            PlayBattleDto first = OfflineBattleSimulator.Simulate("c1-s1", heroes, 123456L, 5, 1000, 10);
            PlayBattleDto second = OfflineBattleSimulator.Simulate("c1-s1", heroes, 123456L, 5, 1000, 10);

            Assert.AreEqual(first.battle.seed, second.battle.seed);
            Assert.AreEqual(first.battle.outcome, second.battle.outcome);
            Assert.AreEqual(first.battle.events.Length, second.battle.events.Length);
            for (int i = 0; i < first.battle.events.Length; i++)
            {
                Assert.AreEqual(first.battle.events[i].type, second.battle.events[i].type);
                Assert.AreEqual(first.battle.events[i].timestampMs, second.battle.events[i].timestampMs);
                Assert.AreEqual(first.battle.events[i].actorId, second.battle.events[i].actorId);
                Assert.AreEqual(first.battle.events[i].targetId, second.battle.events[i].targetId);
                Assert.AreEqual(first.battle.events[i].amount, second.battle.events[i].amount);
                Assert.AreEqual(first.battle.events[i].critical, second.battle.events[i].critical);
            }
        }

        [Test]
        public void Replay_HasTenParticipantsAndTerminalKnockouts()
        {
            OwnedHeroDto[] heroes = OfflineSeedFactory.Create().heroes.Take(5).ToArray();
            PlayBattleDto result = OfflineBattleSimulator.Simulate("c1-s1", heroes, 42L, 5, 1000, 0);
            Assert.AreEqual(10, result.participants.Length);
            Assert.AreEqual("TEAM_A", result.battle.outcome);
            Assert.AreEqual(5, result.battle.events.Count(e => e.type == "KO"));
            Assert.Greater(result.battle.durationMs, 0);
        }
    }
}
