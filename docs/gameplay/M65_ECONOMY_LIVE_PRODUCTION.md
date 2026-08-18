# M65 Economy and Live Production Gate

M65 freezes the release economy/live feature census around the already implemented wallet, inventory, shop, summon/pity, guild, quest, event and mail loops rather than duplicating those services.

`game-data/release/m65-economy-live-census.csv` defines the eight required release families and their reset/idempotency policies. `validate-m65-economy-live.py` re-runs the existing feature validators, verifies the release census, and scans the server source for the transaction/idempotency/reset contracts required by the aggregate release loop.

This milestone is an integration gate over existing runtime features. It does not claim that internal prices, pity rates or reward magnitudes are externally parity-verified; those remain subject to the M57/M60/M74 evidence gates.
