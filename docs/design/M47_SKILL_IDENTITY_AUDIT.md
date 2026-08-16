# M47 — Hero Version Skill Identity / Canon Audit

## Purpose

M47 locks the identity boundary between the Hero/Awakening migration and the later real-time combat + full skill-design work.

It does **not** pretend that the existing seeded technique aliases are final balanced/canon-complete kits. The goal is to guarantee that every collectible Hero Version has an explicit five-slot identity, every Awakening has exactly one reserved sixth skill, and all remaining research debt is measurable rather than hidden behind a generic runtime fallback.

## Runtime identity contract

Normal Hero Version:

1. `BASIC`
2. `SKILL_1`
3. `SKILL_2`
4. `ULTIMATE`
5. `PASSIVE`

Awakened Hero Version:

- the same five base identities;
- plus exactly one `AWAKENING_SKILL`.

The final semantic rename of `SKILL_1` into the universal Rage Skill / Nộ Kỹ is intentionally coordinated with #51 M49 and #54 M50. M47 must not hard-code final Rage gains, cooldowns, cast timings or cinematic timings before the continuous-time runtime exists.

## M47 integrity gate

`scripts/validate-m47-skill-identity.py` audits the whole production catalog and requires:

- every Hero Version ID is unique;
- exactly five explicit aliases per Hero Version;
- hero CSV slot IDs and alias rows agree exactly;
- every alias source technique exists in the packaged technique libraries;
- no duplicate slot inside a Hero Version;
- two collectible versions of the same character may not share an identical complete five-technique source kit;
- every Hero Version has at most one Awakening;
- every Awakening has exactly one sixth Awakening Skill;
- Awakening Skill EN/VI names and canon source/confidence metadata are present;
- baseline/unresolved design rows are counted and surfaced as research debt rather than silently accepted as VERIFIED.

## Current design-debt policy

Several existing rows deliberately carry markers such as:

- `PLAYABLE_DESIGN_BASELINE`
- `M47 must tune`
- `M47_EXPLICIT_DESIGN_REQUIRED`
- `SCHEMA_BASELINE_NOT_RUNTIME`
- `UNRESOLVED_EXPLICIT_DESIGN`

These markers are not failures by themselves in M47 because final mechanics depend on the M49 continuous-time/Rage runtime. They **must not** be removed merely to make a dashboard green.

#54/M50 is the mandatory zero-debt gate for full skill design. It must research every Hero Version, select canon/version-appropriate signature techniques, implement Rage Skill semantics, real-time triggers/timings, balance/counterplay, cinematics, VFX/SFX and EN/VI descriptions.

## Canon rules carried forward

- Earlier Hero Versions must not receive techniques learned only in later eras/forms.
- Different collectible versions of the same character need meaningfully differentiated five-skill identities.
- Named attacks or temporary transformations from the legacy variant census must remain skills/temporary forms where research classified them that way; they must not reappear as collectible Hero Versions.
- Awakening Skill must belong to the awakened form and must not be a generic sixth damage button.
- Generic/shared low-level techniques may exist where canonically reasonable, but a complete same-character kit may not be duplicated.
- Unknown mechanics stay explicitly unverified; no canon-confidence upgrade without evidence.

## Relationship to upcoming work

### M48

Final 427-row Hero/Awakening migration audit and removal/deprecation of old linear evolution semantics.

### #51 / M49

Replace authoritative turn/round combat with deterministic continuous-time auto combat, including timestamped events, real-time statuses/cooldowns/casts/recovery, SPD-based action frequency and Rage 0–100 runtime.

### #54 / M50

Use the M49 runtime to complete every Hero Version kit:

- Basic identity;
- signature Rage Skill / Nộ Kỹ;
- independent active/conditional skills;
- passive triggers;
- one sixth Awakening Skill when applicable;
- canon evidence and confidence;
- real-time timing and balance;
- cinematic/animation/VFX/SFX contract;
- Vietnamese + English descriptions generated from runtime values where possible.

## Completion rule

M47 is complete when the full-catalog structural/canon-risk gate and existing server/content regressions are green. This does **not** mean skill design is finished. Overall project completion remains blocked by #51 and #54.
