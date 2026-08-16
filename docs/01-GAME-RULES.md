# Researched Game Rules

Last research pass: 2026-08-16.

This file records **what is known, how confident we are, and what still requires direct capture**. Community guides are useful evidence but are not treated as hidden-formula truth.

## Sources used in the bootstrap

1. Official Google Play listing, Ninja Assemble - Rebirth Mania  
   https://play.google.com/store/apps/details?id=com.fpx.ninja.gp&hl=en
2. MangtoyPedia — Tips Bermain Game Ninja Rebirth  
   https://www.mangtoypedia.com/2020/04/tips-bermain-game-ninja-rebirth.html
3. MangtoyPedia — Tips Memilih Formasi Team Ninja Rebirth  
   https://www.mangtoypedia.com/2020/05/tips-memilih-formasi-team-ninja-rebirth.html
4. MangtoyPedia — Tips Memilih Scroll yang Tepat  
   https://www.mangtoypedia.com/2020/06/ini-dia-tips-memilih-scroll-yang-tepat.html
5. MangtoyPedia — Tips Daily Quest Ninja Rebirth  
   https://www.mangtoypedia.com/2022/12/tips-daily-quest-ninja-rebirth.html

## Core loop

| Rule | Status | Notes |
|---|---|---|
| Build/develop the Hidden Village and gather/train ninjas | OFFICIAL_CONFIRMED | Official listing |
| Adventure/PvE consumes energy/stamina | OFFICIAL_CONFIRMED | Official listing describes free energy used to continue adventure |
| Multiple currencies support hero training and mode stores | OFFICIAL_CONFIRMED | Diamond, Arena Coin, Hero Coin, Guild Coin, Shadow Coin named officially |
| Team composition/synergy matters | OFFICIAL_CONFIRMED | Official listing explicitly emphasizes partners/teamwork |

## Combat archetypes

`COMMUNITY_CONFIRMED`

Ninjas are described in three practical combat archetypes:

- `CHAKRA` — skills/healing primarily scale from Chakra Attack;
- `PHYSICAL` — physical skills/basic attacks dominate;
- `HYBRID` — meaningfully uses both channels.

Examples documented by community guides include Naruto as Hybrid and specialized Chakra/Physical formations.

### Stats/effects evidenced by guides

- Physical Attack / Physical Defense;
- Chakra Attack / Chakra Defense;
- Physical Critical / Chakra Critical;
- HP;
- speed increase/decrease;
- Initial Energy;
- Physical/Chakra immunity;
- stun;
- silence;
- purity/cleanse-type behavior;
- chakra absorption;
- healing;
- revive-related effects.

**Exact damage, critical and immunity formulas remain `UNKNOWN` until measured.** The community guide presents an immunity description, but that is not sufficient to freeze a final server formula.

## Arena

`COMMUNITY_CONFIRMED`

- formation size: **5 ninjas**;
- asynchronous/non-real-time fights are described;
- attacker and defender can use automatic ultimate behavior;
- objective includes ranking competition;
- Arena Coin is an official currency tied to Hero War/Arena.

Implementation rule frozen now: `ARENA_TEAM_SIZE = 5`.

Still to measure:

- opponent refresh algorithm;
- exact number of daily attempts;
- ranking delta formula;
- ultimate AI/energy thresholds;
- season/reset timings;
- defense snapshot semantics.

## Shadow Arena / Shadow Fight

`COMMUNITY_CONFIRMED` + official currency/mode support.

- roster requirement: **15 ninjas**;
- split into **3 battles**, five ninjas per battle;
- winning **2 battles** wins the series;
- if first two games split 1–1, the third is the decider;
- Shadow Coin is officially tied to Shadow Fight Arena.

Implementation rules frozen now:

- `SHADOW_ROSTER_SIZE = 15`;
- `SHADOW_SQUAD_SIZE = 5`;
- `SHADOW_SQUAD_COUNT = 3`;
- series ends on 2 wins.

## Main Quest

`COMMUNITY_CONFIRMED`

Three difficulty tracks:

1. Normal;
2. Elite;
3. Heroic.

The guide states progression is gated in order: Normal before Elite/Heroic. Main Quest provides character progression and rewards such as jutsu shards/diamonds/XP-related resources.

Still to capture:

- exact chapter count;
- stage stamina costs;
- star conditions;
- drop tables;
- sweep rules;
- boss/wave layouts.

## Ninja Trial

`COMMUNITY_CONFIRMED`

Three documented modes, each described as having six levels:

### Authentic Waterfall

- White Zetsu and a final ninja encounter;
- enemies described as resistant/immune to Physical damage;
- therefore Chakra-focused team is required.

### Gama Temple

- frog enemies and final ninja encounter;
- enemies described as resistant/immune to Chakra damage;
- therefore Physical-focused team is required.

### Path of Kunoichi

- only female ninja (kunoichi) can be used.

These restrictions should be represented as generic stage-entry/combat modifiers, not hard-coded screens.

## Forest Hunt

`COMMUNITY_CONFIRMED`

Two documented modes with six levels each:

- **Hunt Forest** — rewards sellable items → Gold;
- **Land of Pain** — rewards XP books for ninja leveling.

## Tailed Beast Conquer

`COMMUNITY_CONFIRMED`

PvE challenge against Tailed Beasts; documented rewards include rare jutsu shards used for advancement.

Exact encounter rotation/reward tables are still `UNKNOWN`.

## Ninja Quest

`COMMUNITY_CONFIRMED`

- quest ranks described as S/A/B/C/D;
- higher rank → better reward;
- can yield jutsu shards, XP books and late-game progression prerequisites;
- participating ninja gain XP.

## Crusade

`COMMUNITY_CONFIRMED`

- fights opponents from the same server / server population;
- opponent strength is described as scaling to player/character level;
- rewards across successive steps include Gold, jutsu shards and sellable items;
- at least Easy and additional difficulty tiers are described; full exact tier naming/count must be captured before finalization.

## Frame Advance

`COMMUNITY_CONFIRMED`

Frame progression is distinct from hero level:

| Current frame | Color/evidence | Advances to next |
|---|---|---:|
| Genin | no frame | 1 |
| Chunin | yellow | 2 |
| Jonin | blue | 3 |
| Kage | purple | 4 |
| Six Path | gold | transitions toward Awakening | n/a |
| Awakening | late-game state | target details unknown | n/a |

Implementation can safely freeze the first four advance counts. Six Path/Awakening requirements must remain data-driven until captured.

## Six Path Mode / character evolution

`COMMUNITY_CONFIRMED`, exact hero-by-hero paths `UNKNOWN`.

The guide distinguishes frame progression from a stronger character-version transformation. It states that eligible heroes may require:

- Six Path+1 state;
- a dedicated Ninja Quest;
- Class S shard/item requirement;
- skill-5 eligibility (some heroes are marked as having no Six Path version).

Examples described in guides include transformations such as Naruto → Sage Mode and Sasuke → Susanoo Mode, but **the full roster and exact paths must be census-driven**.

## Tailed Beast / Jinchuriki progression

`COMMUNITY_CONFIRMED`

- each ninja can progress a Tailed Beast/Jinchuriki-related system;
- uses ninja soul and Beast Bone resources;
- progression is described up to Kyubi;
- full Tailed Beast progression is described as one prerequisite for late Six Path progression.

Do not merge this with Frame Advance in code.

## Scroll / Ninja College

`COMMUNITY_CONFIRMED`

Ninja College contains at least:

- Training — acquire scrolls;
- Inlay Scroll — equip scrolls to ninjas;
- Combine — feed/upgrade scrolls.

Community guide states scroll level max is **10**.

### Yin-Yang slot

Only one Yin-Yang slot is described. Examples:

- Komatsukami — Physical & Chakra immunity;
- In'yoton — Initial Energy;
- Revive — HP.

### Elemental scroll slots

The five elements are:

- Lightning;
- Water;
- Fire;
- Earth;
- Wind.

Documented scroll combinations can grant channel-specific attack, defense and critical stats. The content layer must model an item as occupying one or multiple supported element tags and exposing stat modifiers.

## Team synergy

`COMMUNITY_CONFIRMED`

A Team feature provides attribute bonuses for related characters. A guide uses Konoha Class 7 (Naruto, Sasuke, Sakura) as an example and describes raising the members' frame levels to increase related team attributes.

Exact bonus formulas and all team groups require census.

## Shops / currencies

Official currencies:

- Diamond;
- Arena Coin;
- Hero Coin;
- Guild Coin;
- Shadow Coin.

Community guides additionally describe Gold and mode stores including:

- Ninja Store;
- Mystic Vendor;
- 6-Path Shop;
- Arena Store;
- Crusade/Ninja store;
- Guild Store;
- Shadow Store;
- Boruto Store/soul exchange.

Because historical builds may rename menus, final IDs and UI labels must come from the selected reference build.

## Daily/event loop

Community evidence describes Daily Quest and events for:

- Gold Spent;
- Diamond Spent;
- Arena Battle;
- Shadow Arena Battle;
- Ninjutsu Training;
- Free Gold;
- Free Stamina;
- roulette/exchange activities.

Daily Quest examples include running Ninja Trial/Hunt Forest, upgrading skills, Tailed Beast/Jinchuriki actions, Arena battles, Challenge, buying Gold and Tailed Beast Conquer.

## What is intentionally not frozen yet

The following remain parity-blocking research items:

- complete hero/variant roster;
- every hero's 1–5 skill definitions and upgrade breakpoints;
- damage formula;
- speed/turn scheduler formula;
- energy gain/spend rules;
- crit/dodge/immunity formulas;
- status duration/stack rules;
- all formation slots and front/rear targeting rules;
- all late-game evolution requirements;
- exact stage and drop tables;
- exact shop refresh/price tables;
- event calendars and reset times;
- UI animation timing;
- battle animation timing.

These must be observed/captured instead of guessed.
