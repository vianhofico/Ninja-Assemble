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
        private readonly Dictionary<string, BattleActorView> actors = new Dictionary<string, BattleActorView>();
        private Coroutine playback;

        public event Action<BattlePresentationEvent> EventPresented;
        public event Action PlaybackCompleted;
        public bool IsPlaying => playback != null;

        public void Register(BattleActorView actor)
        {
            if (actor == null || string.IsNullOrWhiteSpace(actor.BattleUnitId)) return;
            actors[actor.BattleUnitId] = actor;
        }

        public bool TryGetActor(string battleUnitId, out BattleActorView actor)
        {
            return actors.TryGetValue(battleUnitId ?? string.Empty, out actor);
        }

        public void ClearActors()
        {
            actors.Clear();
        }

        public Coroutine Play(IReadOnlyList<BattlePresentationEvent> events)
        {
            if (events == null) throw new ArgumentNullException(nameof(events));
            if (playback != null) StopCoroutine(playback);
            playback = StartCoroutine(PlayRoutine(events));
            return playback;
        }

        private IEnumerator PlayRoutine(IReadOnlyList<BattlePresentationEvent> events)
        {
            foreach (BattlePresentationEvent item in events)
            {
                switch (item.Type)
                {
                    case "ATTACK":
                        BattleActorView actor;
                        if (TryGetActor(item.ActorId, out actor)) actor.PlayAttack();
                        yield return new WaitForSeconds(attackLeadSeconds);
                        break;
                    case "DAMAGE":
                        BattleActorView target;
                        if (TryGetActor(item.TargetId, out target)) target.ApplyDamage(item.Amount, item.Critical);
                        yield return new WaitForSeconds(impactHoldSeconds);
                        break;
                    case "KO":
                        BattleActorView defeated;
                        if (TryGetActor(item.TargetId, out defeated)) defeated.PlayDeath();
                        break;
                }
                EventPresented?.Invoke(item);
            }
            playback = null;
            PlaybackCompleted?.Invoke();
        }
    }
}
