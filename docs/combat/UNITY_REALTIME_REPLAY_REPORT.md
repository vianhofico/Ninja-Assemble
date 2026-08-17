# Unity Real-Time Replay Cutover — M49/M50

## Goal

Consume timestamped deterministic battle replays across Campaign, Arena and Shadow Arena while preserving a temporary legacy `BattleResult` projection for compatibility with older server/client builds.

## Architecture

The Unity presentation path is shared across all battle modes:

`server realtimeBattle -> RealtimeBattleDtoCompatibility -> BattleResultDto -> BattlePresentationAdapter -> BattleTimelinePlayer`

The real-time replay is authoritative whenever it is present. The legacy `battle` payload is a compatibility projection/fallback only.

## DTO coverage

Unity models:

- `RealtimeBattleResultDto.durationMs`
- `RealtimeBattleEventDto.timestampMs`
- `RealtimeBattleEventDto.durationMs`
- additive `realtimeBattle` on campaign waves and campaign battle responses
- additive `realtimeBattle` on Arena battle responses
- additive `realtimeBattle` on each Shadow Arena squad battle

The existing `BattleResultDto`/`BattleEventDto` carry promoted timestamp metadata so presentation code continues using one normalized shape.

Unity `JsonUtility` ignores server fields the client does not need, so server-side `finalHp` does not need a Unity map DTO for playback.

## Promotion and fallback

`RealtimeBattleDtoCompatibility` provides overloads for:

- `PlayBattleDto`
- `ArenaBattleDto`
- `ShadowArenaBattleDto`

For every available `realtimeBattle.events` stream it:

1. projects each timestamped event into the existing client battle-event DTO;
2. preserves `timestampMs` and `durationMs` as canonical presentation timing;
3. marks the promoted result/event as real-time;
4. keeps approximate legacy `round`/`durationTurns` buckets only for temporary UI compatibility;
5. sorts by timestamp and sequence;
6. replaces the client-side `battle` reference with the promoted result.

It does not execute, request or compare a second simulation.

If a server payload has no real-time replay, the original `battle` object remains untouched.

## Timeline playback

`BattleTimelinePlayer` waits for the delta between consecutive simulation timestamps instead of inserting artificial per-event delays.

For real-time replay:

- `CAST_START` begins the actor ability animation and updates energy;
- `CAST_COMPLETE` is an ordering/presentation hook and does not duplicate the animation;
- `ATTACK` no longer starts a second animation;
- `DAMAGE` / `STATUS_TICK` apply at their simulation timestamp without an extra impact hold;
- `STATUS_EXPIRED` clears status presentation;
- multiple events sharing the same timestamp are presented in sequence without a fabricated delay.

Legacy replay behavior remains available for compatibility: attack lead and impact hold delays apply only to non-real-time events.

## Playback speed

`BattleTimelinePlayer.PlaybackSpeed` supports 0.25x–4x presentation speed. This scales Unity waits only; it does not mutate simulation timestamps, outcomes, RNG, rating or rewards.

## Mode rollout state after M50

| Mode | Server simulation | Unity replay |
|---|---|---|
| Campaign / Adventure | Real-time authoritative | Timestamped real-time |
| Arena | Real-time authoritative | Timestamped real-time |
| Shadow Arena | Real-time authoritative per squad | Timestamped real-time per squad |

Competitive rating/reward logic now consumes authoritative real-time outcomes. Shadow Arena DRAW tiebreaks use authoritative real-time final HP before falling back to squad power/seed ordering.

## Integrity gates

`scripts/validate-realtime-unity-replay.py` checks DTO coverage, promotion for all three modes, timestamp playback, cast-start behavior, status expiry handling and absence of blocking `Thread.Sleep`/`Task.Delay` calls.

`scripts/validate-realtime-competitive-cutover.py` additionally prevents Arena/Shadow Arena application services from reintroducing `DeterministicBattleEngine` or legacy `BattleRequest` execution.

CI workflows:

- `.github/workflows/realtime-unity-replay-integrity.yml`
- `.github/workflows/realtime-competitive-cutover.yml`

## Remaining work

1. Add Unity Editor/play-mode tests when a licensed runner is available.
2. Replace projected `durationTurns` labels/compatibility fields with millisecond/semantic UI.
3. Add visible 1x/2x/4x controls plus pause/resume wiring around `PlaybackSpeed`.
4. Convert all production skill/status content to authored millisecond timing.
5. Remove the old round engine and legacy replay projection only after code search/validation proves no production caller remains.
