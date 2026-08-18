# M64 Production Progression

M64 keeps the existing Frame Advance, hero-level and equipment systems, and fills the missing advanced progression families without duplicating those foundations.

## Advanced tracks

`game-data/progression/advanced-tracks.csv` freezes 11 internal release tracks: Scroll Mastery, Ninja College and the nine Tailed-Beast/Jinchuriki tracks from Shukaku through Kurama.

Each track defines EN/VI identity, max level, account-level unlock, Gold/item cost curve and cumulative gameplay bonus. These are internal product values, not external balance/parity evidence.

## Runtime

`AdvancedProgressionApplicationService` exposes an authoritative board and idempotent upgrade flow. Upgrade requests are locked by request ID, Gold and item costs use existing Wallet/Inventory idempotency ledgers, the track row is locked for update, and the exact response is stored in `progression_upgrade_requests` for deterministic retry.

## Unity

Advanced progression uses a dedicated Unity feature module: `AdvancedProgressionDtos`, `AdvancedProgressionClient` and `AdvancedProgressionStore`. This keeps the generic playable store from becoming a monolith and gives M66–M68 a clean feature boundary.

## Validation

`validate-m64-progression.py` reruns hero progression/equipment checks, verifies Frame Advance remains present, then enforces exactly two learning + nine Jinchuriki/Tailed-Beast tracks and the persistence/API/Unity idempotency contract.
