# Content Kit System — Complete Roster+

## Goal

A character/variant is not considered implemented merely because it has a roster row or portrait. Every base character must resolve to a five-part gameplay kit:

1. Basic attack
2. Skill 1
3. Skill 2
4. Ultimate
5. Passive

Major forms can override the base kit profile. This is the mechanism that makes Sage Mode, KCM, Rinnegan, Susanoo and Jinchuriki forms play differently rather than acting as cosmetic skins.

## Technique library

Technique definitions are bilingual at the content layer (`name_en`, `name_vi`, `description_en`, `description_vi`) and declare damage channel, technique kind and semantic tags. Numerical coefficients/targets/status effects stay in versioned balance/effect profiles so reference parity can be tuned without rewriting kit identity.

## Kit profiles

Profiles are reusable only when the source characters genuinely share a combat identity (for example Hyuga Gentle Fist, Nara shadow control, Akimichi expansion, Seven Swordsmen or generic village combat styles). Flagship characters use dedicated profiles.

## Variant overrides

`variant-kit-overrides.csv` changes the profile for forms where combat identity changes materially. Examples:

- Naruto → Sage → KCM → Six Paths
- Sasuke → Mangekyo/EMS → Rinnegan
- Madara/Obito → Ten-Tails/Six Paths
- Jinchuriki cloak forms
- Guy Seventh/Eighth Gate
- Kabuto Snake Sage

## Release gate

The release validator requires:

- at least 180 base characters;
- at least 300 variants;
- at least 100 bilingual techniques;
- at least 35 kit profiles;
- every base character mapped to an existing kit profile;
- every variant override references an existing census variant and profile;
- every release variant has a complete `READY` presentation package.
