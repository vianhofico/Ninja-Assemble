# Desktop Port Plan

## Goal

Ship a desktop edition from the same Ninja Assemble gameplay/server contracts without forking balance, progression, economy, localization, combat simulation, roster data, or art manifests. The first target is **Windows x64 via Unity desktop build** because the current client is already Unity; macOS/Linux can follow once the Windows interaction/build pipeline is stable.

The desktop client is a presentation/input/build target, not a second game implementation.

## Shared architecture rules

1. **Server-authoritative gameplay remains unchanged.** Desktop uses the same REST endpoints, request idempotency and deterministic battle replay as mobile.
2. **One game-data source.** Hero census, variants, skills, campaign, items, shops, quests, progression, localization and reference profiles remain under `game-data/`.
3. **One art package contract.** Desktop consumes the same production art package descriptors and READY evidence used by mobile. Do not create a separate desktop art manifest.
4. **One localization source.** EN/VI remains sourced from `game-data/localization/strings.csv` and the packaged Unity resource.
5. **No client-side balance duplication.** Costs, rewards, eligibility, rating, boss damage, evolution gates and equipment stats stay on the server.
6. **Platform-specific code is limited to UI layout, input, display, filesystem/logging and packaging.**

## D0 — Preconditions and dependency boundary

Desktop development can start immediately from the playable baseline. Production launch still depends on the shared production-art gate. Android device evidence is a mobile-release blocker only and does not block desktop engineering.

Before calling the desktop build production-ready:

- all required hero variants used by the desktop release must have concrete READY art packages;
- Windows build artifacts must have desktop QA evidence;
- all existing server/content/readiness workflows must remain green;
- no desktop-only balance/profile fork may exist.

## D1 — Desktop project/build profile

### Work

- Add a Unity standalone Windows x86_64 build profile.
- Add a desktop build entry point alongside Android automation.
- Keep the existing scenes/prefabs/game API bootstrap shared.
- Add desktop-specific PlayerSettings: product name, window icon, resizable window, min/default resolution, fullscreen modes and target frame rate.
- Introduce a `PlatformPresentationProfile` with mobile/desktop layout/input flags; it must not contain gameplay rules.
- Add GitHub Actions source validation for Windows build automation. Actual Unity builds should use a licensed/self-hosted or otherwise valid Unity build environment rather than fabricated artifacts.

### Exit criteria

- Windows build can be invoked deterministically from a documented command/CI entry point.
- Desktop build uses the same server URL/config mechanism as mobile.
- No duplicated game-data or REST contract files.

## D2 — Responsive desktop layout

### Work

- Preserve the current mobile vertical slice as a narrow-layout mode.
- Add desktop layout breakpoints for 16:9 and common desktop windows.
- Convert bottom-navigation-heavy flows to a left/top navigation rail where appropriate while retaining the same `ScreenId` destinations.
- Give Roster, Formation, Inventory/Equipment, Shop, Quest/Event/Mail and Guild more horizontal information density.
- Battle view: center battlefield, move controls/log/status to desktop side panels, keep deterministic replay unchanged.
- Support window resize without recreating authoritative game state.

### Target resolutions

- Minimum supported: 1280×720.
- Primary QA: 1920×1080.
- High-resolution QA: 2560×1440 and 3840×2160 UI scaling check.

### Exit criteria

- All 16 screens remain usable at 1280×720 and 1920×1080.
- No clipped primary actions at supported aspect ratios.
- Battle participants/VFX remain readable at desktop scale.

## D3 — Keyboard/mouse interaction

### Global mappings

- `Esc`: back/close modal/pause battle replay where applicable.
- `Enter` / `Space`: activate focused primary action.
- Arrow keys / WASD: navigation where focus navigation is appropriate.
- Mouse wheel: scroll long roster/inventory/mail lists.

### Screen improvements

- Roster: click hero to select; keyboard focus selection.
- Hero Detail: clickable tabs/actions for variant, train, frame advance and evolve.
- Formation: drag/drop hero cards into slots; server save still validates exact five unique owned heroes.
- Inventory/Equipment: drag or click equip; explicit enhance button and slot filters.
- Shop: select exact offer rather than mobile “first purchasable” shortcut.
- Arena/Shadow Arena: select exact opponent.
- Battle: optional shortcuts for replay speed/pause; never alter server battle result.

### Exit criteria

- Every primary flow can be completed using mouse only.
- Core navigation can be completed using keyboard without requiring touch semantics.
- Drag/drop produces the same server requests as the existing authoritative actions.

## D4 — Desktop settings/platform services

### Work

- Persist language/audio/graphics using the current settings contract, with desktop resolution/fullscreen additions.
- Add display mode: Windowed / Borderless / Fullscreen.
- Add resolution selector and VSync/frame cap.
- Add platform-safe config/log directories using Unity persistent data paths.
- Add an in-app “Open Logs Folder” desktop-only action.
- Keep account/guest identity portable through server identity, not local gameplay saves.
- Optional later: controller support and Steam integration. Steam must be an adapter around account/distribution services, not a gameplay dependency.

### Exit criteria

- Settings survive restart.
- EN/VI switching affects the desktop runtime UI.
- No secrets or auth tokens are logged.

## D5 — Performance and visual quality

### Baseline targets

- 1080p / 60 FPS on the chosen mid-tier Windows reference hardware.
- No gameplay divergence when graphics quality changes.
- Scalable VFX density/shadows/post-processing for low/mid/high profiles.
- Memory/performance telemetry captured during battle, roster browsing and repeated screen transitions.

### Art handling

- Reuse the same chibi prefabs, portraits, animation sets, VFX and SFX package descriptors.
- Desktop may use higher-resolution texture/import variants only when they remain generated/derived from the same approved production package.
- Do not mark art READY from upscaled placeholders or concepts.

## D6 — Packaging/distribution

### Stage 1

- Windows x64 ZIP build for internal QA.
- Version includes Git SHA and game-data/profile versions in diagnostics.

### Stage 2

- Signed Windows installer when release credentials are available.
- Verify clean install, upgrade, uninstall and writable data paths.

### Optional Stage 3

- Steam build/depot, achievements/friends only after core desktop QA is stable.
- No Steam-specific balance, inventory or currency authority.

## D7 — Desktop QA matrix

Record desktop evidence separately from Android evidence. Minimum production matrix should include:

- at least one lower-spec integrated/discrete GPU class;
- one representative mid-tier Windows machine;
- one high-resolution/high-refresh configuration;
- 1280×720, 1920×1080 and 2560×1440 presentation checks;
- windowed + borderless/fullscreen;
- English + Vietnamese;
- keyboard/mouse navigation;
- Campaign boss multi-wave, Arena, Shadow Arena, Guild boss, Summon, Shop, Equipment, Frame Advance/Evolution, Quest/Event/Mail.

Capture Git SHA, Unity version, build artifact, OS/GPU/CPU/RAM, resolution, smoke result, average FPS/p95 frame time/memory and capture references.

## Recommended implementation sequence

1. `D1` Windows build profile + CI source validation.
2. `D2` desktop responsive shell for all 16 screens.
3. `D3` exact mouse/keyboard interactions and drag/drop editors.
4. `D4` desktop settings/logging.
5. `D5` performance/quality tiers.
6. `D6` packaging/signing/distribution.
7. `D7` evidence-driven QA and production release gate.

## Desktop definition of done

Desktop is complete only when:

- all mobile playable-baseline gameplay features are available without client-side rule forks;
- all 16 screens have desktop-appropriate layout and mouse/keyboard interaction;
- EN/VI works across dynamic runtime UI;
- authoritative server contracts and deterministic replay are reused unchanged;
- Windows build automation and packaging are reproducible;
- desktop QA evidence meets the target performance matrix;
- production art used by the release has genuine READY package evidence;
- a desktop release gate passes from a concrete build SHA/artifact.
