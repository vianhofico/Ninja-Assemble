using TMPro;
using UnityEngine;
using UnityEngine.UI;

namespace NinjaAssemble.Presentation
{
    public sealed class BattleActorView : MonoBehaviour
    {
        [field: SerializeField] public string BattleUnitId { get; private set; }
        [field: SerializeField] public string CharacterId { get; private set; }
        [field: SerializeField] public string Variant { get; private set; }
        [field: SerializeField] public long MaxHp { get; private set; }
        [field: SerializeField] public long CurrentHp { get; private set; }
        [field: SerializeField] public long Shield { get; private set; }
        [field: SerializeField] public int Energy { get; private set; }

        [SerializeField] private Animator animator;
        [SerializeField] private Transform hitAnchor;
        [SerializeField] private TMP_Text nameLabel;
        [SerializeField] private TMP_Text levelLabel;
        [SerializeField] private TMP_Text statusLabel;
        [SerializeField] private Slider healthSlider;
        [SerializeField] private Slider energySlider;
        [SerializeField] private AudioSource audioSource;
        [SerializeField] private AudioClip attackClip;
        [SerializeField] private AudioClip skillClip;
        [SerializeField] private AudioClip ultimateClip;
        [SerializeField] private AudioClip passiveClip;
        [SerializeField] private AudioClip hitClip;
        [SerializeField] private AudioClip deathClip;
        [SerializeField] private AudioClip victoryClip;

        public Transform HitAnchor => hitAnchor != null ? hitAnchor : transform;
        public bool IsAlive => CurrentHp > 0;

        public void Bind(string battleUnitId) => BattleUnitId = battleUnitId;

        public void Configure(string battleUnitId, string characterId, string variant, string displayName, int level, long maxHp)
        {
            BattleUnitId = battleUnitId ?? string.Empty;
            CharacterId = characterId ?? string.Empty;
            Variant = variant ?? string.Empty;
            MaxHp = System.Math.Max(1L, maxHp);
            CurrentHp = MaxHp;
            Shield = 0;
            Energy = 0;
            if (nameLabel != null) nameLabel.text = string.IsNullOrWhiteSpace(displayName) ? CharacterId : displayName;
            if (levelLabel != null) levelLabel.text = "Lv." + Mathf.Max(1, level);
            if (statusLabel != null) statusLabel.text = string.Empty;
            UpdateHealthUi();
            UpdateEnergyUi();
        }

        public void ConfigureUi(TMP_Text name, TMP_Text level, Slider hp)
        {
            nameLabel = name;
            levelLabel = level;
            healthSlider = hp;
            UpdateHealthUi();
        }

        public void ConfigureStatusUi(TMP_Text status) { statusLabel = status; }
        public void ConfigureEnergyUi(Slider energy) { energySlider = energy; UpdateEnergyUi(); }
        public void PlayAttack() => PlayAbility("BASIC");

        public void PlayAbility(string abilityKind)
        {
            string kind = string.IsNullOrWhiteSpace(abilityKind) ? "BASIC" : abilityKind.ToUpperInvariant();
            switch (kind)
            {
                case "ULTIMATE":
                    TriggerWithFallback("Ultimate", "Attack");
                    PlayClip(ultimateClip != null ? ultimateClip : skillClip);
                    break;
                case "SKILL1":
                    TriggerWithFallback("Skill1", "Attack");
                    PlayClip(skillClip != null ? skillClip : attackClip);
                    break;
                case "SKILL2":
                    TriggerWithFallback("Skill2", "Attack");
                    PlayClip(skillClip != null ? skillClip : attackClip);
                    break;
                case "PASSIVE":
                    PlayPassive();
                    break;
                default:
                    Trigger("Attack");
                    PlayClip(attackClip);
                    break;
            }
        }

        public void PlayPassive()
        {
            TryTrigger("Passive");
            PlayClip(passiveClip != null ? passiveClip : skillClip);
        }

        public void SetEnergy(int value)
        {
            if (value < 0) return;
            Energy = Mathf.Clamp(value, 0, 100);
            UpdateEnergyUi();
        }

        public void ApplyDamage(long amount, bool critical)
        {
            if (amount > 0) CurrentHp = System.Math.Max(0L, CurrentHp - amount);
            Trigger(critical ? "CriticalHit" : "Hit");
            PlayClip(hitClip);
            UpdateHealthUi();
        }

        public void ApplyHeal(long amount)
        {
            if (amount <= 0 || !IsAlive) return;
            CurrentHp = System.Math.Min(MaxHp, CurrentHp + amount);
            TriggerWithFallback("Heal", "Hit");
            UpdateHealthUi();
        }

        public void AddShield(long amount)
        {
            if (amount <= 0) return;
            Shield = System.Math.Max(0L, Shield + amount);
            TriggerWithFallback("Shield", "Hit");
        }

        public void AbsorbShield(long amount)
        {
            if (amount <= 0) return;
            Shield = System.Math.Max(0L, Shield - amount);
        }

        public void ApplyStatus(string statusId, int durationTurns)
        {
            if (statusLabel != null)
                statusLabel.text = string.IsNullOrWhiteSpace(statusId) ? string.Empty : statusId + (durationTurns > 0 ? " " + durationTurns : string.Empty);
        }

        public void ClearStatus(string statusId)
        {
            if (statusLabel != null && (string.IsNullOrWhiteSpace(statusId) || statusLabel.text.StartsWith(statusId))) statusLabel.text = string.Empty;
        }

        public void Revive(long amount)
        {
            CurrentHp = System.Math.Min(MaxHp, System.Math.Max(1L, amount));
            Shield = 0;
            TriggerWithFallback("Revive", "Hit");
            UpdateHealthUi();
        }

        public void PlayHit(bool critical) => ApplyDamage(0, critical);

        public void PlayDeath()
        {
            CurrentHp = 0;
            UpdateHealthUi();
            Trigger("Death");
            PlayClip(deathClip);
        }

        public void PlayVictory()
        {
            if (!IsAlive) return;
            Trigger("Victory");
            PlayClip(victoryClip);
        }

        private void UpdateHealthUi()
        {
            if (healthSlider == null) return;
            healthSlider.minValue = 0f;
            healthSlider.maxValue = 1f;
            healthSlider.value = MaxHp <= 0 ? 0f : Mathf.Clamp01((float)CurrentHp / MaxHp);
        }

        private void UpdateEnergyUi()
        {
            if (energySlider == null) return;
            energySlider.minValue = 0f;
            energySlider.maxValue = 100f;
            energySlider.value = Energy;
        }

        private void TriggerWithFallback(string primary, string fallback)
        {
            if (!TryTrigger(primary)) Trigger(fallback);
        }

        private bool TryTrigger(string trigger)
        {
            if (animator == null) return false;
            int hash = Animator.StringToHash(trigger);
            foreach (AnimatorControllerParameter parameter in animator.parameters)
            {
                if (parameter.type == AnimatorControllerParameterType.Trigger && parameter.nameHash == hash)
                {
                    animator.SetTrigger(hash);
                    return true;
                }
            }
            return false;
        }

        private void Trigger(string trigger) { if (animator != null) animator.SetTrigger(trigger); }
        private void PlayClip(AudioClip clip) { if (audioSource != null && clip != null) audioSource.PlayOneShot(clip); }
    }
}
