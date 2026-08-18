# M75 E2E Evidence

This directory stores **real execution evidence only**. Do not commit synthetic PASS reports.

A certification report must conform to `e2e-run.schema.json`, reference the exact 40-character commit SHA that was exercised, include all 20 ordered journey steps and all 10 reliability cases, and point to retained server/test/migration artifacts.

`python scripts/validate-m75-e2e.py --enforce` intentionally fails when no real PASS report exists.
