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

        [SerializeField] private Animator animator;
        [SerializeField] private Transform hitAnchor;
        [SerializeField] private TMP_Text nameLabel;
        [SerializeField] private TMP_Text levelLabel;
        [SerializeField] private Slider healthSlider;
        [SerializeField] private AudioSource audioSource;
        [SerializeField] private AudioClip attackClip;
        [SerializeField] private AudioClip hitClip;
        [SerializeField] private AudioClip deathClip;
        [SerializeField] private AudioClip victoryClip;

        public Transform HitAnchor => hitAnchor != null ? hitAnchor : transform;
        public bool IsAlive => CurrentHp > 0;

        public void Bind(string battleUnitId)
        {
            BattleUnitId = battleUnitId;
        }

        public void Configure(
            string battleUnitId,
            string characterId,
            string variant,
            string displayName,
            int level,
            long maxHp)
        {
            BattleUnitId = battleUnitId ?? string.Empty;
            CharacterId = characterId ?? string.Empty;
            Variant = variant ?? string.Empty;
            MaxHp = System.Math.Max(1L, maxHp);
            CurrentHp = MaxHp;
            if (nameLabel != null) nameLabel.text = string.IsNullOrWhiteSpace(displayName) ? CharacterId : displayName;
            if (levelLabel != null) levelLabel.text = "Lv." + Mathf.Max(1, level);
            UpdateHealthUi();
        }

        public void ConfigureUi(TMP_Text name, TMP_Text level, Slider hp)
        {
            nameLabel = name;
            levelLabel = level;
            healthSlider = hp;
            UpdateHealthUi();
        }

        public void PlayAttack()
        {
            Trigger("Attack");
            PlayClip(attackClip);
        }

        public void ApplyDamage(long amount, bool critical)
        {
            if (amount > 0) CurrentHp = System.Math.Max(0L, CurrentHp - amount);
            Trigger(critical ? "CriticalHit" : "Hit");
            PlayClip(hitClip);
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

        private void Trigger(string trigger)
        {
            if (animator != null) animator.SetTrigger(trigger);
        }

        private void PlayClip(AudioClip clip)
        {
            if (audioSource != null && clip != null) audioSource.PlayOneShot(clip);
        }
    }
}
