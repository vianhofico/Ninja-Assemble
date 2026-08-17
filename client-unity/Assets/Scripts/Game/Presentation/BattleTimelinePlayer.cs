using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace NinjaAssemble.Presentation
{
    public sealed class BattleTimelinePlayer : MonoBehaviour
    {
        [SerializeField, Min(0f)] private float attackLeadSeconds = 0.12f;
        [SerializeField, Min(0f)] private float impactHoldSeconds = 0.08f;
        [SerializeField, Range(0.25f, 4f)] private float playbackSpeed = 1f;
        private readonly Dictionary<string, BattleActorView> actors = new Dictionary<string, BattleActorView>();
        private Coroutine playback;

        public event Action<BattlePresentationEvent> EventPresented;
        public event Action PlaybackCompleted;
        public bool IsPlaying => playback != null;
        public float PlaybackSpeed
        {
            get => playbackSpeed;
            set => playbackSpeed = Mathf.Clamp(value, 0.25f, 4f);
        }

        public void Register(BattleActorView actor)
        {
            if (actor == null || string.IsNullOrWhiteSpace(actor.BattleUnitId)) return;
            actors[actor.BattleUnitId] = actor;
        }

        public bool TryGetActor(string battleUnitId, out BattleActorView actor) => actors.TryGetValue(battleUnitId ?? string.Empty, out actor);
        public void ClearActors() => actors.Clear();

        public Coroutine Play(IReadOnlyList<BattlePresentationEvent> events)
        {
            if (events == null) throw new ArgumentNullException(nameof(events));
            if (playback != null) StopCoroutine(playback);
            playback = StartCoroutine(PlayRoutine(events));
            return playback;
        }

        private IEnumerator PlayRoutine(IReadOnlyList<BattlePresentationEvent> events)
        {
            long previousRealtimeTimestampMs = 0L;
            bool hasRealtimeTimestamp = false;

            foreach (BattlePresentationEvent item in events)
            {
                if (item == null) continue;
                if (item.IsRealtime)
                {
                    if (hasRealtimeTimestamp)
                    {
                        long deltaMs = Math.Max(0L, item.TimestampMs - previousRealtimeTimestampMs);
                        if (deltaMs > 0L)
                            yield return new WaitForSeconds((deltaMs / 1000f) / Mathf.Max(0.01f, playbackSpeed));
                    }
                    previousRealtimeTimestampMs = item.TimestampMs;
                    hasRealtimeTimestamp = true;
                }

                BattleActorView actor;
                switch (item.Type)
                {
                    case "PASSIVE_TRIGGER":
                        if (TryGetActor(item.ActorId, out actor)) actor.PlayPassive();
                        break;
                    case "CAST_START":
                        if (TryGetActor(item.ActorId, out actor))
                        {
                            actor.PlayAbility(item.AbilityKind);
                            actor.SetEnergy(item.EnergyAfter);
                        }
                        break;
                    case "ATTACK":
                        if (TryGetActor(item.ActorId, out actor))
                        {
                            // Real-time replays start the presentation on CAST_START. Legacy replays still
                            // animate on ATTACK because they have no cast lifecycle events.
                            if (!item.IsRealtime) actor.PlayAbility(item.AbilityKind);
                            actor.SetEnergy(item.EnergyAfter);
                        }
                        if (!item.IsRealtime && attackLeadSeconds > 0f)
                            yield return new WaitForSeconds(attackLeadSeconds / Mathf.Max(0.01f, playbackSpeed));
                        break;
                    case "DAMAGE":
                    case "STATUS_TICK":
                        if (TryGetActor(item.TargetId, out actor)) actor.ApplyDamage(item.Amount, item.Critical);
                        if (!item.IsRealtime && impactHoldSeconds > 0f)
                            yield return new WaitForSeconds(impactHoldSeconds / Mathf.Max(0.01f, playbackSpeed));
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
                    case "ENERGY":
                        if (TryGetActor(item.TargetId, out actor)) actor.SetEnergy(item.EnergyAfter);
                        break;
                    case "STATUS_APPLIED":
                    case "TURN_SKIPPED":
                        if (TryGetActor(item.TargetId, out actor)) actor.ApplyStatus(item.StatusId, item.DurationTurns);
                        break;
                    case "STATUS_EXPIRED":
                    case "STATUS_CLEANSED":
                        if (TryGetActor(item.TargetId, out actor)) actor.ClearStatus(item.StatusId);
                        break;
                    case "REVIVE":
                        if (TryGetActor(item.TargetId, out actor)) actor.Revive(item.Amount);
                        break;
                    case "KO":
                        if (TryGetActor(item.TargetId, out actor)) actor.PlayDeath();
                        break;
                }
                EventPresented?.Invoke(item);
            }
            playback = null;
            PlaybackCompleted?.Invoke();
        }
    }
}
