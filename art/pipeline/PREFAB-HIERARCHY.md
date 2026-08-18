# M69 Hero Prefab Hierarchy

Every release hero prefab must use this stable runtime hierarchy so battle presentation, pooling and regression tooling can bind without hero-specific reflection.

```text
Hero_<characterId>_<variantSlug>
├── VisualRoot
│   ├── ModelRoot
│   ├── WeaponRoot
│   └── ShadowRoot
├── VfxSockets
│   ├── Root
│   ├── Head
│   ├── Chest
│   ├── LeftHand
│   ├── RightHand
│   ├── Weapon
│   └── Ground
├── AudioRoot
├── UiAnchor
└── Runtime
    ├── Animator
    ├── BattleActorView
    └── AddressableIdentity
```

## Animator contract
Required states/triggers: `Idle`, `Move`, `Attack`, `Hit`, `KO`, `Skill`, `RageSkill`. Existing prefab compatibility may retain an `Ultimate` trigger only as a fallback; new art packages must author `RageSkill`.

## Runtime rules
- No authoritative combat logic inside hero prefabs.
- No scene-singleton references serialized into prefabs.
- VFX spawned from sockets must be pool-safe and self-resetting.
- Audio clips are presentation-only and replay-rate compatible.
- `UiAnchor` must remain stable for damage/status labels.
- Prefab root scale is `(1,1,1)` and runtime facing is handled by battle presentation.

## READY rule
A prefab cannot be marked READY until the Addressables audit resolves the configured address, the hierarchy audit passes, battle regression capture exists, and the package performance audit is attached.
