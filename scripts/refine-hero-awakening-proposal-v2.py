#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / "refine-hero-awakening-proposal.py"

spec = importlib.util.spec_from_file_location("hero_awake_refine", SOURCE)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load semantic refinement module")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class ForceSkillMap(dict):
    """Keep tuple-key lookup/items semantics while yielding variant labels for validation loops."""
    def __iter__(self):
        for character_id, variant in dict.keys(self):
            yield variant

module.FORCE_SKILLS = ForceSkillMap(module.FORCE_SKILLS)

if __name__ == "__main__":
    raise SystemExit(module.main())
