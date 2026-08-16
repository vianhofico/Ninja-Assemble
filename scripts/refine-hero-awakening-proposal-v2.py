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

class ForceKey(tuple):
    """Tuple key that also behaves like its variant label for legacy validation code."""
    def __new__(cls, character_id: str, variant: str):
        return super().__new__(cls, (character_id, variant))

    def lower(self) -> str:
        return self[1].lower()

module.FORCE_SKILLS = {
    ForceKey(character_id, variant): reason
    for (character_id, variant), reason in module.FORCE_SKILLS.items()
}

if __name__ == "__main__":
    raise SystemExit(module.main())
