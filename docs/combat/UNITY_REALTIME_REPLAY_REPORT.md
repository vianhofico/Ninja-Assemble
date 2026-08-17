# Unity Real-Time Replay Cutover — M49

## Goal

Consume the timestamped campaign replay introduced by M47/M48 without breaking Arena, Shadow Arena or older server payloads that still return the legacy round-shaped `BattleResult`.

## Architecture

M49 keeps the existing Unity presentation pipeline and inserts one compatibility boundary at the network/store layer:

`server realtimeBattle -> RealtimeBattleDtoCompatibility -> BattleResultDto -> BattlePresentationAdapter -> BattleTimelinePlayer`

The real-time replay is authoritative whenever it is present. The legacy `battle` payload remains the fallback for modes or server versions that have not been cut over yet.

## DTO changes

Unity now models:

- `RealtimeBattleResultDto.durationMs`
- `RealtimeBattleEventDto.timestampMs`
- `RealtimeBattleEventDto.durationMs`
- additive `realtimeBattle` on campaign wave and top-level campaign battle responses

The existing `BattleResultDto`/`BattleEventDto` also carry promoted timestamp metadata so all presentation code can continue using one shape.

Unity `JsonUtility` ignores server fields that the client does not need, so the server-side `finalHp` map does not require a Unity DTO representation for replay playback.

## Promotion and fallback

`RealtimeBattleDtoCompatibility.Promote(...)` runs immediately after a campaign response is deserialized.

When `realtimeBattle.events` exists it:

1. projects each timestamped event into the existing client battle-event DTO;
2. preserves `timestampMs` and `durationMs` as canonical presentation timing;
3. marks the promoted result/event as real-time;
4. keeps approximate legacy `round`/`durationTurns` buckets only for temporary UI compatibility;
5. sorts by timestamp and sequence;
6. replaces the client-side `battle` reference with the promoted result.

It does not execute or request another simulation.

When no real-time replay exists, the original `battle` object is untouched.

## Timeline playback

`BattleTimelinePlayer` now waits for the delta between consecutive simulation timestamps instead of inserting artificial per-event delays.

For real-time replay:

- `CAST_START` begins the actor ability animation and updates energy;
- `CAST_COMPLETE` is an ordering/presentation hook and does not duplicate the animation;
- `ATTACK` no longer starts a second animation;
- `DAMAGE` / `STATUS_TICK` apply at their simulation timestamp without an extra impact hold;
- `STATUS_EXPIRED` clears status presentation;
- multiple events sharing the same timestamp are presented in sequence without a fabricated delay.

Legacy replay behavior is unchanged: attack lead and impact hold delays remain active when events are not marked real-time.

## Playback speed

`BattleTimelinePlayer.PlaybackSpeed` supports 0.25x–4x presentation speed. It scales presentation waits only; it does not mutate simulation timestamps, outcomes, RNG or rewards.

## Mode rollout state

| Mode | Server simulation | Unity replay after M49 |
|---|---|---|
| Campaign / Adventure | Real-time authoritative | Timestamped real-time |
| Arena | Legacy round engine | Legacy fallback |
| Shadow Arena | Legacy round engine | Legacy fallback |

This staged rollout keeps the UI compatible while M50 migrates competitive modes.

## Integrity gate

`scripts/validate-realtime-unity-replay.py` checks the migration boundary for required DTO fields, promotion logic, timestamp playback, cast-start behavior, status expiry handling and absence of blocking `Thread.Sleep`/`Task.Delay` calls.

`.github/workflows/realtime-unity-replay-integrity.yml` runs this validator for pull requests touching the relevant Unity replay files.

## Remaining work

1. Add in-Editor/play-mode tests once a Unity licensed runner is available in CI.
2. Migrate Arena and Shadow Arena server responses to `RealtimeBattleResult` and reuse the same Unity promotion boundary.
3. Replace temporary projected `durationTurns` status labels with millisecond/semantic UI.
4. Add user-facing 1x/2x/4x controls and pause/resume wiring around `PlaybackSpeed`.
5. Remove the legacy replay projection only after all battle modes are timestamped.
