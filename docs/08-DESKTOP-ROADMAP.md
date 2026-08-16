# Desktop Roadmap — after Mobile Release

## Strategy

Desktop is a new presentation/input target, not a second game. Reuse the same Unity project, game-data, hero assets, localization tables, backend APIs and deterministic battle protocol.

## Phase D0 — Platform foundation

- Windows x64 build target first;
- optional macOS and Linux targets;
- desktop build profiles and CI artifacts;
- environment selection for embedded/local or remote backend.

## Phase D1 — Desktop UX

- 16:9 and 16:10 responsive layout profiles;
- mouse hover/click states;
- keyboard navigation and shortcuts;
- scroll-wheel support;
- windowed, fullscreen and borderless modes;
- safe UI scaling for 1080p, 1440p and 4K.

## Phase D2 — Graphics/performance

- desktop quality presets;
- configurable FPS cap;
- higher particle density and resolution where appropriate;
- texture/atlas tier selection;
- shader quality options.

## Phase D3 — Packaging

- Windows installer/package;
- local-backend launcher option for private offline/LAN play;
- patch/cache strategy;
- crash logs and save-data location documentation.

## Entry condition

Desktop work begins only after mobile core systems, Complete Roster+, visual quality, EN/VI localization and E2E release gates are complete.
