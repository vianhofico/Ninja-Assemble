using UnityEngine;

namespace NinjaAssemble.Presentation
{
    public sealed class BattleActorView : MonoBehaviour
    {
        [field: SerializeField] public string BattleUnitId { get; private set; }
        [SerializeField] private Animator animator;
        [SerializeField] private Transform hitAnchor;

        public Transform HitAnchor => hitAnchor != null ? hitAnchor : transform;

        public void Bind(string battleUnitId) => BattleUnitId = battleUnitId;
        public void PlayAttack() => Trigger("Attack");
        public void PlayHit(bool critical) => Trigger(critical ? "CriticalHit" : "Hit");
        public void PlayDeath() => Trigger("Death");
        public void PlayVictory() => Trigger("Victory");

        private void Trigger(string trigger)
        {
            if (animator != null) animator.SetTrigger(trigger);
        }
    }
}
