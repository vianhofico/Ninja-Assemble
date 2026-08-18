# M61 Production Campaign

M61 freezes the first mobile release Campaign at 12 stages across three chapters/difficulties and completes the missing sweep contract.

## Release census

`game-data/campaign/release-census.csv` mirrors the authoritative 12-stage catalog:

- Chapter 1: 4 NORMAL stages
- Chapter 2: 4 ELITE stages
- Chapter 3: 4 HEROIC stages
- chapter-final stages use two five-enemy waves

Every stage has EN/VI naming, Energy cost, prerequisite progression, first/repeat EXP/currency rewards, enemy waves and item reward coverage. The census is an internal product release decision backed by committed repo data; it does not claim parity with an external game's stage count.

## Sweep

`CampaignSweepService` adds server-authoritative sweep with these rules:

1. stage must already have a real clear;
2. normal stage Energy is spent;
3. only repeat rewards are granted;
4. best stars are not improved by sweep;
5. clear count is incremented;
6. requestId is mandatory and serialised with a PostgreSQL transaction advisory lock;
7. retries return the recorded result without re-spending Energy/re-granting rewards;
8. reward idempotency uses `campaign-sweep:` namespace, separate from battle rewards;
9. no fake BattleResult/BattleSeed is created.

`V13__campaign_sweep.sql` persists sweep results so retries are deterministic at the reward/result level.

## Client contract

Unity now has:

- `CampaignSweepDto` / `CampaignSweepItemDto`;
- `GameApiClient.SweepCampaignStageAsync`;
- `PlayableGameStore.SweepableStage`;
- `PlayableGameStore.SweepCampaignAsync` with authoritative Campaign/Inventory/Shop/Quest refresh.

The current generic Adventure shell continues to use its primary action for battle. M67's production Campaign screen will expose separate Battle and Sweep controls using this completed store/API contract.

## Validation

`validate-m61-campaign.py --enforce` runs the existing Campaign and multi-wave/inventory validators, then requires exact release-census coverage and the sweep idempotency/client contracts.

The production release workflow includes the M61 enforce gate.
