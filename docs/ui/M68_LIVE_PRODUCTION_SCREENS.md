# M68 — Competitive, Social and Live Production Screens

## Scope
M68 productionizes the remaining player-facing mobile surfaces on top of the M66 UI foundation without forking authoritative gameplay state.

Production screen set (10):
- Arena
- Shadow Arena
- Guild
- Shop
- Quest
- Events
- Mail
- Settings
- Resource PvE
- Advanced Progression

## Runtime binding
Arena/Shadow/Guild/Shop/Quest/Events/Mail/Settings continue using their existing gameplay/dedicated bridge state and actions. `ProductionLiveScreenInstaller` mirrors that state through `ProductionLegacyScreenBinding`, then hides the generic BodyPanel only after the production panel is attached.

Resource PvE and Advanced Progression are first-class scenes in M68. Resource PvE binds directly to `PlayableGameStore.ResourcePve`; Advanced Progression binds to `MobileGameBootstrap.AdvancedProgression`. Their primary CTA calls the real server-authoritative battle/upgrade endpoints.

## Scene shell
`ScreenId`, `MobileSceneNames` and `MobileSceneBuilder` now include `ResourcePve` and `Progression`, so generated Android builds include these screens rather than relying on a documentation-only feature hub.

## Validation
`python scripts/validate-m68-live-screens.py` requires exactly ten production screen specs, scene generation contracts, progression bootstrap state and live server-backed CTA bindings.
