using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace NinjaAssemble.Presentation
{
    /** Replays authoritative timestamped combat events. Playback speed never changes server simulation outcome. */
    public sealed class BattleTimelinePlayer : MonoBehaviour
    {
        private readonly Dictionary<string, BattleActorView> actors = new Dictionary<string, BattleActorView>();
        private Coroutine playback;
        private int playbackSpeed = 1;
        private bool paused;
        private BattlePlaybackHud playbackHud;
        private BattleImpactFeedback impactFeedback;

        public event Action<BattlePresentationEvent> EventPresented;
        public event Action PlaybackCompleted;
        public bool IsPlaying => playback != null;
        public int PlaybackSpeed => playbackSpeed;
        public bool IsPaused => paused;
        public long CurrentTimestampMs { get; private set; }

        public void Register(BattleActorView actor)
        {
            if (actor == null || string.IsNullOrWhiteSpace(actor.BattleUnitId)) return;
            actors[actor.BattleUnitId] = actor;
            actor.SetPresentationRate(playbackSpeed, paused);
        }
        public bool TryGetActor(string battleUnitId, out BattleActorView actor) => actors.TryGetValue(battleUnitId ?? string.Empty, out actor);
        public void ClearActors() => actors.Clear();

        public void SetPlaybackSpeed(int speed)
        {
            if (speed != 1 && speed != 2 && speed != 4) throw new ArgumentOutOfRangeException(nameof(speed), "Battle playback supports 1x, 2x or 4x");
            playbackSpeed = speed;
            RefreshActorPresentationRates();
        }

        public void SetPaused(bool value)
        {
            paused = value;
            RefreshActorPresentationRates();
        }

        public Coroutine Play(IReadOnlyList<BattlePresentationEvent> events)
        {
            if (events == null) throw new ArgumentNullException(nameof(events));
            if (playback != null) StopCoroutine(playback);
            paused = false;
            CurrentTimestampMs = 0;
            EnsurePlayableQualityLayer();
            RefreshActorPresentationRates();
            playback = StartCoroutine(PlayRoutine(events));
            return playback;
        }

        private void EnsurePlayableQualityLayer()
        {
            playbackHud = GetComponent<BattlePlaybackHud>();
            if (playbackHud == null) playbackHud = gameObject.AddComponent<BattlePlaybackHud>();
            playbackHud.Bind(this);

            impactFeedback = GetComponent<BattleImpactFeedback>();
            if (impactFeedback == null) impactFeedback = gameObject.AddComponent<BattleImpactFeedback>();
            impactFeedback.Bind(this);
        }

        private IEnumerator PlayRoutine(IReadOnlyList<BattlePresentationEvent> events)
        {
            long previousTimestampMs = 0;
            foreach (BattlePresentationEvent item in events)
            {
                long delta = Math.Max(0, item.TimestampMs - previousTimestampMs);
                if (delta > 0) yield return WaitForSimulationDelay(delta);
                previousTimestampMs = Math.Max(previousTimestampMs, item.TimestampMs);
                CurrentTimestampMs = previousTimestampMs;
                Present(item);
                EventPresented?.Invoke(item);
            }
            playback = null;
            PlaybackCompleted?.Invoke();
        }

        private IEnumerator WaitForSimulationDelay(long simulationDelayMs)
        {
            double remainingMs = simulationDelayMs;
            while (remainingMs > 0)
            {
                if (paused) { yield return null; continue; }
                double simulationStepMs = Time.unscaledDeltaTime * 1000.0 * playbackSpeed;
                if (simulationStepMs <= 0) { yield return null; continue; }
                double consumed = Math.Min(remainingMs, simulationStepMs);
                AdvanceStatusClocks(consumed);
                remainingMs -= consumed;
                yield return null;
            }
        }

        private void AdvanceStatusClocks(double simulationDeltaMs)
        {
            foreach (BattleActorView actor in actors.Values) actor.AdvanceStatusClock(simulationDeltaMs);
        }

        private void RefreshActorPresentationRates()
        {
            foreach (BattleActorView actor in actors.Values) actor.SetPresentationRate(playbackSpeed, paused);
        }

        private void Present(BattlePresentationEvent item)
        {
            BattleActorView actor;
            switch (item.Type)
            {
                case "PASSIVE_TRIGGER":
                    if (TryGetActor(item.ActorId, out actor)) actor.PlayPassive();
                    break;
                case "BASIC_ATTACK_START":
                case "CAST_START":
                case "RAGE_SKILL_CAST_START":
                case "ABILITY":
                    if (TryGetActor(item.ActorId, out actor)) { actor.PlayAbility(item.AbilityKind); actor.SetRage(item.RageAfter); }
                    break;
                case "RAGE_GAIN":
                case "RAGE_FULL":
                case "RAGE_SKILL_READY":
                    if (TryGetActor(item.ActorId, out actor)) actor.SetRage(item.RageAfter);
                    break;
                case "DAMAGE":
                case "STATUS_TICK":
                    if (TryGetActor(item.TargetId, out actor)) actor.ApplyDamage(item.Amount, item.Critical);
                    break;
                case "SHIELD_ABSORB":
                    if (TryGetActor(item.TargetId, out actor)) actor.AbsorbShield(item.Amount);
                    break;
                case "HEAL":
                    if (TryGetActor(item.TargetId, out actor)) actor.ApplyHeal(item.Amount);
                    break;
                case "SHIELD":
                    if (TryGetActor(item.TargetId, out actor)) actor.AddShield(item.Amount);
                    break;
                case "STATUS_APPLIED":
                    if (TryGetActor(item.TargetId, out actor)) actor.ApplyStatus(item.StatusId, item.DurationMs);
                    break;
                case "STATUS_EXPIRED":
                case "STATUS_CLEANSED":
                case "STATUS_REMOVED":
                    if (TryGetActor(item.TargetId, out actor)) actor.ClearStatus(item.StatusId);
                    break;
                case "REVIVE":
                    if (TryGetActor(item.TargetId, out actor)) actor.Revive(item.Amount);
                    break;
                case "KO":
                    if (TryGetActor(item.TargetId, out actor)) actor.PlayDeath();
                    break;
            }
        }
    }
}
