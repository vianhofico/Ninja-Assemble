using System;
using System.Collections;
using NinjaAssemble.Presentation;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;

namespace NinjaAssemble.Tests.Editor
{
    public sealed class BattlePlaybackContractTests
    {
        private GameObject root;
        private BattleTimelinePlayer timeline;

        [SetUp]
        public void SetUp()
        {
            root = new GameObject("BattleTimelineTestRoot", typeof(RectTransform));
            timeline = root.AddComponent<BattleTimelinePlayer>();
        }

        [TearDown]
        public void TearDown()
        {
            if (root != null) UnityEngine.Object.DestroyImmediate(root);
        }

        [Test]
        public void PlaybackSpeed_AcceptsOnlyOneTwoAndFour()
        {
            timeline.SetPlaybackSpeed(1);
            Assert.AreEqual(1, timeline.PlaybackSpeed);

            timeline.SetPlaybackSpeed(2);
            Assert.AreEqual(2, timeline.PlaybackSpeed);

            timeline.SetPlaybackSpeed(4);
            Assert.AreEqual(4, timeline.PlaybackSpeed);

            Assert.Throws<ArgumentOutOfRangeException>(() => timeline.SetPlaybackSpeed(0));
            Assert.Throws<ArgumentOutOfRangeException>(() => timeline.SetPlaybackSpeed(3));
            Assert.Throws<ArgumentOutOfRangeException>(() => timeline.SetPlaybackSpeed(5));
        }

        [Test]
        public void PauseState_IsExplicitAndReversible()
        {
            Assert.IsFalse(timeline.IsPaused);
            timeline.SetPaused(true);
            Assert.IsTrue(timeline.IsPaused);
            timeline.SetPaused(false);
            Assert.IsFalse(timeline.IsPaused);
        }

        [UnityTest]
        public IEnumerator EmptyReplay_CompletesWithoutLeavingStalePlayingHandle()
        {
            int completed = 0;
            timeline.PlaybackCompleted += () => completed++;

            Coroutine handle = timeline.Play(Array.Empty<BattlePresentationEvent>());

            Assert.IsNotNull(handle);
            Assert.IsTrue(timeline.IsPlaying);

            // PlayRoutine deliberately yields once before it can complete, so Play() always
            // stores the handle before the routine clears it. Give the editor runner enough
            // frames to resume both the playback and this test coroutine deterministically.
            yield return null;
            yield return null;

            Assert.IsFalse(timeline.IsPlaying);
            Assert.AreEqual(1, completed);
            Assert.AreEqual(0L, timeline.CurrentTimestampMs);
        }
    }
}
