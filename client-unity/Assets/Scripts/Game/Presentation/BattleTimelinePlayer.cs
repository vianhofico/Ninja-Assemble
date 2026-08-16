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
        private readonly Dictionary<string, BattleActorView> actors = new();

        public event Action<BattlePresentationEvent> EventPresented;

        public void Register(BattleActorView actor)
        {
            if (actor == null || string.IsNullOrWhiteSpace(actor.BattleUnitId)) return;
            actors[actor.BattleUnitId] = actor;
        }

        public Coroutine Play(IReadOnlyList<BattlePresentationEvent> events)
        {
            if (events == null) throw new ArgumentNullException(nameof(events));
            return StartCoroutine(PlayRoutine(events));
        }

        private IEnumerator PlayRoutine(IReadOnlyList<BattlePresentationEvent> events)
        {
            foreach (var item in events)
            {
                switch (item.Type)
                {
                    case "ATTACK":
                        if (Try(item.ActorId, out var actor)) actor.PlayAttack();
                        yield return new WaitForSeconds(attackLeadSeconds);
                        break;
                    case "DAMAGE":
                        if (Try(item.TargetId, out var target)) target.PlayHit(item.Critical);
                        yield return new WaitForSeconds(impactHoldSeconds);
                        break;
                    case "KO":
                        if (Try(item.TargetId, out var defeated)) defeated.PlayDeath();
                        break;
                }
                EventPresented?.Invoke(item);
            }
        }

        private bool Try(string id, out BattleActorView actor) => actors.TryGetValue(id ?? string.Empty, out actor);
    }
}
