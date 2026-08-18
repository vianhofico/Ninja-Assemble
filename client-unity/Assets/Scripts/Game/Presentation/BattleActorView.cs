using TMPro;
using UnityEngine;
using UnityEngine.Serialization;
using UnityEngine.UI;

namespace NinjaAssemble.Presentation
{
    public sealed class BattleActorView : MonoBehaviour
    {
        [field: SerializeField] public string BattleUnitId { get; private set; }
        [field: SerializeField] public string CharacterId { get; private set; }
        [field: SerializeField] public string HeroId { get; private set; }
        [field: SerializeField] public bool Awakened { get; private set; }
        [field: SerializeField] public string AwakeningId { get; private set; }
        [field: SerializeField] public string PresentationKey { get; private set; }
        [field: SerializeField] public string Variant { get; private set; }
        [field: SerializeField] public long MaxHp { get; private set; }
        [field: SerializeField] public long CurrentHp { get; private set; }
        [field: SerializeField] public long Shield { get; private set; }
        [field: SerializeField] public int Rage { get; private set; }

        [SerializeField] private Animator animator;
        [SerializeField] private Transform hitAnchor;
        [SerializeField] private TMP_Text nameLabel;
        [SerializeField] private TMP_Text levelLabel;
        [SerializeField] private TMP_Text statusLabel;
        [SerializeField] private Slider healthSlider;
        [FormerlySerializedAs("energySlider")]
        [SerializeField] private Slider rageSlider;
        [SerializeField] private AudioSource audioSource;
        [SerializeField] private AudioClip attackClip;
        [SerializeField] private AudioClip skillClip;
        [FormerlySerializedAs("ultimateClip")]
        [SerializeField] private AudioClip rageSkillClip;
        [SerializeField] private AudioClip passiveClip;
        [SerializeField] private AudioClip hitClip;
        [SerializeField] private AudioClip deathClip;
        [SerializeField] private AudioClip victoryClip;

        private string activeStatusId = string.Empty;
        private double statusRemainingMs;
        private float targetHealth01 = 1f;
        private float targetRage;
        private int presentationSpeed = 1;
        private bool presentationPaused;

        public Transform HitAnchor => hitAnchor != null ? hitAnchor : transform;
        public bool IsAlive => CurrentHp > 0;

        private void Update()
        {
            if (presentationPaused) return;
            float rate = Mathf.Max(1f, presentationSpeed);
            if (healthSlider != null)
                healthSlider.value = Mathf.MoveTowards(healthSlider.value, targetHealth01, Time.unscaledDeltaTime * 3.8f * rate);
            if (rageSlider != null)
                rageSlider.value = Mathf.MoveTowards(rageSlider.value, targetRage, Time.unscaledDeltaTime * 150f * rate);
        }

        public void Bind(string battleUnitId) => BattleUnitId = battleUnitId;

        public void Configure(string battleUnitId, string characterId, string variant, string displayName, int level, long maxHp)
        {
            Configure(battleUnitId, characterId, string.Empty, false, string.Empty, string.Empty, variant, displayName, level, maxHp);
        }

        public void Configure(string battleUnitId, string characterId, string heroId, bool awakened, string awakeningId,
            string presentationKey, string variant, string displayName, int level, long maxHp)
        {
            BattleUnitId = battleUnitId ?? string.Empty;
            CharacterId = characterId ?? string.Empty;
            HeroId = heroId ?? string.Empty;
            Awakened = awakened;
            AwakeningId = awakeningId ?? string.Empty;
            PresentationKey = presentationKey ?? string.Empty;
            Variant = variant ?? string.Empty;
            MaxHp = System.Math.Max(1L, maxHp);
            CurrentHp = MaxHp;
            Shield = 0;
            Rage = 0;
            activeStatusId = string.Empty;
            statusRemainingMs = 0;
            targetHealth01 = 1f;
            targetRage = 0f;
            if (nameLabel != null) nameLabel.text = string.IsNullOrWhiteSpace(displayName) ? (string.IsNullOrWhiteSpace(HeroId) ? CharacterId : HeroId) : displayName;
            if (levelLabel != null) levelLabel.text = "Lv." + Mathf.Max(1, level);
            RefreshStatusLabel();
            ConfigureSliderRanges();
            SnapUiToTargets();
        }

        public void ConfigureUi(TMP_Text name, TMP_Text level, Slider hp)
        {
            nameLabel = name;
            levelLabel = level;
            healthSlider = hp;
            ConfigureSliderRanges();
            SnapUiToTargets();
        }
        public void ConfigureStatusUi(TMP_Text status) { statusLabel = status; RefreshStatusLabel(); }
        public void ConfigureRageUi(Slider rage) { rageSlider = rage; ConfigureSliderRanges(); SnapUiToTargets(); }
        public void PlayAttack() => PlayAbility("BASIC");

        public void SetPresentationRate(int speed, bool paused)
        {
            presentationSpeed = Mathf.Clamp(speed, 1, 4);
            presentationPaused = paused;
            if (animator != null) animator.speed = paused ? 0f : presentationSpeed;
            if (audioSource != null)
            {
                audioSource.pitch = Mathf.Clamp(0.9f + (presentationSpeed - 1) * 0.15f, 0.9f, 1.35f);
                if (paused) audioSource.Pause();
                else audioSource.UnPause();
            }
        }

        public void PlayAbility(string abilityKind)
        {
            string kind = string.IsNullOrWhiteSpace(abilityKind) ? "BASIC" : abilityKind.ToUpperInvariant();
            switch (kind)
            {
                case "AWAKENING_SKILL": TriggerWithFallback("AwakeningSkill", "RageSkill", "Ultimate"); PlayClip(rageSkillClip != null ? rageSkillClip : skillClip); break;
                case "RAGE_SKILL": TriggerWithFallback("RageSkill", "Ultimate", "Attack"); PlayClip(rageSkillClip != null ? rageSkillClip : skillClip); break;
                case "SKILL1": TriggerWithFallback("Skill1", "Attack"); PlayClip(skillClip != null ? skillClip : attackClip); break;
                case "SKILL2": TriggerWithFallback("Skill2", "Attack"); PlayClip(skillClip != null ? skillClip : attackClip); break;
                case "PASSIVE": PlayPassive(); break;
                default: Trigger("Attack"); PlayClip(attackClip); break;
            }
        }

        public void PlayPassive() { TryTrigger("Passive"); PlayClip(passiveClip != null ? passiveClip : skillClip); }

        public void SetRage(int value)
        {
            if (value < 0) return;
            Rage = Mathf.Clamp(value, 0, 100);
            targetRage = Rage;
            if (rageSlider != null && rageSlider.targetGraphic != null)
                rageSlider.targetGraphic.canvasRenderer.SetAlpha(Rage >= 100 ? 1f : 0.85f);
        }

        public void ApplyDamage(long amount, bool critical)
        {
            if (amount > 0) CurrentHp = System.Math.Max(0L, CurrentHp - amount);
            if (critical) TriggerWithFallback("CriticalHit", "Hit");
            else Trigger("Hit");
            PlayClip(hitClip);
            UpdateHealthTarget();
        }
        public void ApplyHeal(long amount) { if (amount <= 0 || !IsAlive) return; CurrentHp = System.Math.Min(MaxHp, CurrentHp + amount); TriggerWithFallback("Heal", "Hit"); UpdateHealthTarget(); }
        public void AddShield(long amount) { if (amount <= 0) return; Shield = System.Math.Max(0L, Shield + amount); TriggerWithFallback("Shield", "Hit"); }
        public void AbsorbShield(long amount) { if (amount <= 0) return; Shield = System.Math.Max(0L, Shield - amount); }

        public void ApplyStatus(string statusId, long durationMs)
        {
            activeStatusId = statusId ?? string.Empty;
            statusRemainingMs = System.Math.Max(0L, durationMs);
            RefreshStatusLabel();
        }

        public void AdvanceStatusClock(double simulationDeltaMs)
        {
            if (statusRemainingMs <= 0 || string.IsNullOrWhiteSpace(activeStatusId)) return;
            statusRemainingMs = System.Math.Max(0, statusRemainingMs - System.Math.Max(0, simulationDeltaMs));
            if (statusRemainingMs <= 0) activeStatusId = string.Empty;
            RefreshStatusLabel();
        }

        public void ClearStatus(string statusId)
        {
            if (string.IsNullOrWhiteSpace(statusId) || activeStatusId == statusId) { activeStatusId = string.Empty; statusRemainingMs = 0; RefreshStatusLabel(); }
        }

        public void Revive(long amount) { CurrentHp = System.Math.Min(MaxHp, System.Math.Max(1L, amount)); Shield = 0; TriggerWithFallback("Revive", "Hit"); UpdateHealthTarget(); }
        public void PlayHit(bool critical) => ApplyDamage(0, critical);
        public void PlayDeath() { CurrentHp = 0; UpdateHealthTarget(); Trigger("Death"); PlayClip(deathClip); }
        public void PlayVictory() { if (!IsAlive) return; Trigger("Victory"); PlayClip(victoryClip); }

        private void RefreshStatusLabel()
        {
            if (statusLabel == null) return;
            if (!string.IsNullOrWhiteSpace(activeStatusId) && statusRemainingMs > 0)
            {
                double seconds = statusRemainingMs / 1000.0;
                string suffix = PlayerPrefs.GetString("na.language", "en") == "vi" ? " giây" : "s";
                statusLabel.text = activeStatusId + " " + seconds.ToString("0.0") + suffix;
            }
            else statusLabel.text = Awakened ? "AWAKENED" : string.Empty;
        }

        private void UpdateHealthTarget() => targetHealth01 = MaxHp <= 0 ? 0f : Mathf.Clamp01((float)CurrentHp / MaxHp);

        private void ConfigureSliderRanges()
        {
            if (healthSlider != null) { healthSlider.minValue = 0f; healthSlider.maxValue = 1f; }
            if (rageSlider != null) { rageSlider.minValue = 0f; rageSlider.maxValue = 100f; }
        }

        private void SnapUiToTargets()
        {
            UpdateHealthTarget();
            targetRage = Rage;
            if (healthSlider != null) healthSlider.value = targetHealth01;
            if (rageSlider != null) rageSlider.value = targetRage;
        }

        private void TriggerWithFallback(string primary, string fallback) { if (!TryTrigger(primary)) Trigger(fallback); }
        private void TriggerWithFallback(string primary, string fallback, string finalFallback)
        {
            if (TryTrigger(primary)) return;
            if (TryTrigger(fallback)) return;
            Trigger(finalFallback);
        }
        private bool TryTrigger(string trigger)
        {
            if (animator == null) return false;
            int hash = Animator.StringToHash(trigger);
            foreach (AnimatorControllerParameter parameter in animator.parameters)
                if (parameter.type == AnimatorControllerParameterType.Trigger && parameter.nameHash == hash) { animator.SetTrigger(hash); return true; }
            return false;
        }
        private void Trigger(string trigger) { if (animator != null) animator.SetTrigger(trigger); }
        private void PlayClip(AudioClip clip) { if (audioSource != null && clip != null) audioSource.PlayOneShot(clip); }
    }
}
