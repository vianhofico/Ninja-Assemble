#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / "generate-awakening-production-data.py"

spec = importlib.util.spec_from_file_location("awakening_production_generator", SOURCE)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load M42 production-data generator")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def sql_preserve_empty(value: object) -> str:
    """Only Python None is SQL NULL; an empty string remains an explicit empty text value."""
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace("'", "''") + "'"


module.sql = sql_preserve_empty

if __name__ == "__main__":
    raise SystemExit(module.main())
