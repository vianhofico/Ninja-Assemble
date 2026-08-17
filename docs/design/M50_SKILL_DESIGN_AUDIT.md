# M50 Full Hero Version Skill Design Audit

## Purpose

M50 is the mandatory skill-design workstream after the M49 continuous-time/Rage runtime. It does not merely rename old skills. It migrates every playable Hero Version to a five-slot design where Skill 1 is the signature Rage Skill, then researches/tunes every kit and every one-time Awakening Skill.

## Authoritative slot model

Normal Hero Version:

1. `BASIC` — automatic basic attack; universal baseline Rage generator.
2. `SKILL_1` — `RAGE_SKILL`; trigger `RAGE_FULL`; signature/Ultimate/Nộ Kỹ; normally costs 100 Rage.
3. `SKILL_2` — normal active/conditional skill with its own trigger/cooldown.
4. `SKILL_3` — normal active/conditional skill with its own trigger/cooldown.
5. `PASSIVE` — event/time-based passive.

Awakened Hero Version:

- retains the five skills above;
- may receive reviewed changes to Rage Skill presentation/effects where canonically appropriate;
- adds exactly one `AWAKENING_SKILL` as skill 6;
- Awakening never creates another competing Ultimate/Rage meter.

The historical M47 fields `BASIC / SKILL_1 / SKILL_2 / ULTIMATE / PASSIVE` are provenance only during migration. The old `ULTIMATE` technique is the strongest current candidate for new `SKILL_1 / RAGE_SKILL`; old active Skill 1 and Skill 2 shift to Skill 2 and Skill 3. This is a candidate migration, not a canon approval.

## Current baseline discovered

The repository has an explicit five-alias kit for every production Hero Version. However those aliases are mostly `PLAYABLE_DESIGN_BASELINE` seeds. Many `SKILL_1` aliases currently point to ordinary active techniques while the strongest/signature candidate sits in the legacy `ULTIMATE` slot. M49 runtime already treats the fourth executable technique as `RAGE_SKILL`, so M50 must align the production content model, naming and presentation with that runtime instead of preserving contradictory slot semantics.

## Structural migration implemented on this branch

`scripts/generate-m50-skill-design.py` generates a full candidate from every explicit Hero Version alias:

| Final slot | Legacy source | Ability kind | Trigger |
|---|---|---|---|
| BASIC | BASIC | BASIC | BASIC_AUTO |
| SKILL_1 | ULTIMATE | RAGE_SKILL | RAGE_FULL |
| SKILL_2 | SKILL_1 | ACTIVE_SKILL | COOLDOWN |
| SKILL_3 | SKILL_2 | ACTIVE_SKILL | COOLDOWN |
| PASSIVE | PASSIVE | PASSIVE | EVENT |

Every generated row deliberately starts as `RESEARCH_REQUIRED`. The generator preserves the legacy source slot and source technique so every change remains auditable.

## Per-Hero Version research checklist

For every Hero Version, M50 must review all five normal skills together rather than independently.

### Basic

- exact weapon/taijutsu/ninjutsu identity appropriate to the version;
- attack cadence intent relative to SPD;
- Rage gain interaction;
- animation and impact readability.

### Skill 1 — Rage Skill / Nộ Kỹ

Must be the most iconic, recognizable and cinematic technique appropriate to the exact version. Review:

- candidate signature techniques available at that point in canon;
- iconic value;
- climax/power value;
- cinematic value;
- kit differentiation from other versions of the same character;
- gameplay utility beyond raw damage;
- target pattern and counterplay;
- cast/windup/impact/recovery timing;
- Rage cost/readiness;
- animation/VFX/SFX/voice/camera/cut-in specification;
- awakened upgrade behavior, if any.

An early Hero Version must never use a future technique merely because that technique is famous.

### Skill 2 / Skill 3

- distinct combat purpose;
- independent cooldown/trigger/condition;
- no fixed global ability rotation;
- target/effect semantics;
- interaction with status, control, summon, mark, shield, counter, cooperation or role identity as appropriate;
- realtime timing in milliseconds.

### Passive

- event/time trigger, never turn-start semantics;
- exact event conditions and interval/threshold where applicable;
- once-per-battle/repeat semantics;
- counterplay and readable feedback.

### Awakening Skill

For each Hero Version with an Awakening:

- exactly one sixth skill;
- tightly associated with the awakened form;
- distinct effect/animation/VFX/icon;
- explicit timing/trigger/counterplay;
- no second Rage resource.

## Cinematic contract

Every Rage Skill must eventually specify:

- startup pose;
- camera focus/cut-in;
- aura/background treatment;
- movement/projectile/melee choreography;
- impact frames;
- enemy reaction;
- shake/flash/slow-motion where appropriate;
- final pose;
- presentation duration;
- animation key;
- VFX key;
- SFX key;
- optional voice line;
- full-Rage UI READY feedback.

Gameplay time remains server-authoritative. Cinematic duration must never control simulation outcome.

## EN/VI contract

All player-facing skill names/descriptions require English and Vietnamese. Time values must be expressed in seconds to the player and derived from runtime milliseconds where practical. Descriptions must not claim turns/rounds or disagree with runtime effects.

## Balance/evidence rule

M50 must not silently promote experimental values to parity. SPD curves, Rage gain, cooldowns, coefficients, control durations and signature choices remain evidence/research-labelled until reviewed. Deterministic simulation and PvE/PvP sanity tests are required before a kit becomes `READY`.

## Completion gate

M50 is complete only when:

- every production Hero Version has exactly five reviewed base skills;
- every `SKILL_1` is a reviewed `RAGE_SKILL / RAGE_FULL` signature technique;
- every Hero Version with an Awakening has exactly one reviewed sixth Awakening Skill;
- versions of the same character have meaningfully differentiated kits;
- all skill mechanics use M49 continuous-time semantics;
- all Rage/Awakening skills have cinematic manifests;
- EN/VI descriptions match runtime values;
- full-roster canon-risk and coverage audits have no unresolved HIGH blocker;
- deterministic/balance tests pass;
- server/content/mobile gates are green.
