# Time-Based Skill Audit

Status: M49 baseline. Values below are explicit design starting points and remain `EXPERIMENTAL` until reference/balance evidence upgrades them.

| Technique / passive | Old mechanic | Old duration | New mechanic | New durationMs | Display | Reason |
|---|---|---:|---|---:|---|---|
| Acid Spray | POISON | 2 turns | periodic poison, 1s ticks | 7000 | 7 giây / 7 seconds | sustained poison identity without turn coupling |
| Amaterasu | BURN | 3 turns | periodic black flame, 1s ticks | 6000 | 6 giây / 6 seconds | iconic persistent pressure |
| Body Flicker | SPEED_UP | 2 turns | speed window | 5000 | 5 giây / 5 seconds | short mobility tempo window |
| Eight Gates | ATK_UP | 3 turns | high-power self window | 10000 | 10 giây / 10 seconds | transformation-like combat window |
| Kamui Phase | DEF_UP | 2 turns | brief defensive phasing | 4000 | 4 giây / 4 seconds | strong defense needs short counterplay window |
| Lightning Armor | SPEED_UP + DEF_UP | 2 turns | dual buff | 8000 | 8 giây / 8 seconds | sustained signature state |
| Mind Destruction | SILENCE | turn-bound | active/Rage skill restriction | 3000 | 3 giây / 3 seconds | basics remain available |
| Poison Cloud | POISON | turn-bound | periodic poison, 1s ticks | 6000 | 6 giây / 6 seconds | area attrition |
| Sand Coffin | STUN | 1 turn | action-initiation lock | 1800 | 1.8 giây / 1.8 seconds | short hard CC |
| Shadow Neck Bind | STUN | 1 turn | action-initiation lock | 2000 | 2 giây / 2 seconds | stronger bind than generic stun |
| Shadow Possession | STUN | 1 turn | action-initiation lock | 2500 | 2.5 giây / 2.5 seconds | signature control identity |
| Sand Shield | DEF_UP | turn-bound | defense window | 5000 | 5 giây / 5 seconds | defensive stance |
| Tailed Cloak | ATK_UP | 3 turns | temporary combat-form power window | 10000 | 10 giây / 10 seconds | cloak should persist visibly |
| Tsukuyomi | STUN | 2 turns | high-value genjutsu lock | 3000 | 3 giây / 3 seconds | powerful control with explicit counter window |
| Weight Control | SPEED_DOWN | 2 turns | battlefield slow | 6000 | 6 giây / 6 seconds | meaningful tempo manipulation |
| White Rage | STUN | 1 turn | short AoE action lock | 1500 | 1.5 giây / 1.5 seconds | AoE CC duration kept conservative |
| Copy Tactics passive | ATK_UP | turn-bound | battle-start buff | 8000 | 8 giây / 8 seconds | opening adaptation window |
| Byakugan passive | SPEED/ATK_UP | turn-bound | battle-start dual buff | 10000 | 10 giây / 10 seconds | sustained opening perception advantage |
| Jinchuriki passive | ATK_UP | 3 turns | low-HP power window | 8000 | 8 giây / 8 seconds | clutch transformation response |
| Medical passive | heal each turn | each turn | `TIME_INTERVAL` heal | 3000 interval | mỗi 3 giây / every 3 seconds | periodic intent, not universal turn conversion |
| Sage passive | ATK/SPEED_UP | turn-bound | battle-start dual buff | 10000 | 10 giây / 10 seconds | sage-state opening tempo |
| Strategist passive | SPEED_UP team | turn-bound | battle-start team speed buff | 8000 | 8 giây / 8 seconds | opening tactical advantage |
| Swordsman passive | ATK_UP after damage | 2 turns | event-triggered attack buff | 4000 | 4 giây / 4 seconds | rewards sustained offense |
| Will of Fire passive | ATK_UP after ally KO | 3 turns | KO-triggered team buff | 7000 | 7 giây / 7 seconds | comeback window |

## Runtime rules

- STUN blocks basic/active/Rage action initiation until expiration and interrupts an interruptible cast already in windup.
- SILENCE allows Basic attacks but blocks active and Rage skills while active.
- DOT ticks from the simulation clock; it never waits for the affected actor to act.
- SPEED changes future action scheduling through the experimental clamped interval formula.
- Buff/debuff expiry is clock-based.
- UI descriptions must be generated from these runtime values where possible; internal milliseconds are displayed as seconds in EN/VI.

## Follow-up ownership

M49 validates timing mechanics across all existing structured data. M50 performs full Hero Version skill research, signature Rage Skill selection, differentiated kits, canon validation, final balance and cinematic specifications.
