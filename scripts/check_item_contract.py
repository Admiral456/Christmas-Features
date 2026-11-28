#!/usr/bin/env python3
"""Check that Java-created custom items match the shipped resource-pack models."""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


EXPECTED = {
    "gifts": {
        "material": "PAPER",
        "custom_model_data": 12022,
        "item_definition": "assets/minecraft/items/paper.json",
        "model_id": "christmasfeatures:item/christmas_gift",
    },
    "ornaments": {
        "material": "AMETHYST_SHARD",
        "custom_model_data": 12023,
        "item_definition": "assets/minecraft/items/amethyst_shard.json",
        "model_id": "christmasfeatures:item/christmas_ornament",
    },
}


class ContractError(Exception):
    """Raised when Java item creation and the resource pack do not agree."""


def _json_file(pack_root: Path, relative_path: str) -> dict[str, Any]:
    path = pack_root / relative_path
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise ContractError(f"Chybí soubor resource packu: {relative_path}") from exc
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ContractError(f"Neplatný JSON v {relative_path}: {exc}") from exc
    if not isinstance(value, dict):
        raise ContractError(f"{relative_path} musí obsahovat JSON objekt.")
    return value


def verify_contract(java_source: str, pack_root: Path) -> list[str]:
    material_match = re.search(
        r'Material\\s+base\\s*=\\s*type\\.equals\\("gifts"\\)\\s*'
        r'\\?\\s*Material\\.(\\w+)\\s*:\\s*Material\\.(\\w+)\\s*;',
        java_source,
    )
    if not material_match:
        raise ContractError(
            "V HolidayEventManager.java nebylo nalezeno očekávané mapování "
            "gifts/ornaments na základní Material."
        )
    materials = {
        "gifts": material_match.group(1),
        "ornaments": material_match.group(2),
    }

    branch_match = re.search(
        r'if\\s*\\(\\s*type\\.equals\\("gifts"\\)\\s*\\)\\s*'
        r'\\{(?P<gifts>.*?)\\}\\s*else\\s*\\{(?P<ornaments>.*?)\\}',
        java_source,
        re.DOTALL,
    )
    if not branch_match:
        raise ContractError(
            "V HolidayEventManager.java nebyly nalezeny samostatné větve "
            "pro vytvoření dárku a ozdoby."
        )

    summaries: list[str] = []
    for item_type, specification in EXPECTED.items():
        actual_material = materials[item_type]
        if actual_material != specification["material"]:
            raise ContractError(
                f"{item_type}: Java používá Material.{actual_material}, ale resource "
                f"pack očekává Material.{specification['material']}."
            )

        branch = branch_match.group(item_type)
        values = re.findall(r"setCustomModelData\\s*\\(\\s*(\\d+)\\s*\\)", branch)
        if len(values) != 1:
            raise ContractError(
                f"{item_type}: očekávána právě jedna hodnota setCustomModelData, "
                f"nalezeno {len(values)}."
            )
        java_value = int(values[0])
        expected_value = specification["custom_model_data"]
        if java_value != expected_value:
            raise ContractError(
                f"{item_type}: Java používá CustomModelData {java_value}, "
                f"resource pack očekává {expected_value}."
            )

        definition = _json_file(pack_root, specification["item_definition"])
        model = definition.get("model")
        if not isinstance(model, dict):
            raise ContractError(
                f"{item_type}: {specification['item_definition']} nemá objekt model."
            )
        if (
            model.get("type") != "minecraft:range_dispatch"
            or model.get("property") != "minecraft:custom_model_data"
            or model.get("index") != 0
        ):
            raise ContractError(
                f"{item_type}: model musí používat range_dispatch nad "
                "custom_model_data index 0."
            )

        entries = model.get("entries")
        if not isinstance(entries, list):
            raise ContractError(f"{item_type}: v item definition chybí entries.")
        matching = [
            entry for entry in entries
            if isinstance(entry, dict)
            and entry.get("threshold") == expected_value
        ]
        if len(matching) != 1:
            raise ContractError(
                f"{item_type}: pro CustomModelData {expected_value} musí existovat "
                f"právě jeden modelový záznam, nalezeno {len(matching)}."
            )

        model_reference = matching[0].get("model")
        if (
            not isinstance(model_reference, dict)
            or model_reference.get("type") != "minecraft:model"
            or model_reference.get("model") != specification["model_id"]
        ):
            raise ContractError(
                f"{item_type}: CustomModelData {expected_value} neukazuje na "
                f"{specification['model_id']}."
            )

        model_namespace, model_path = specification["model_id"].split(":", 1)
        model_file = pack_root / "assets" / model_namespace / "models" / f"{model_path}.json"
        if not model_file.is_file():
            raise ContractError(
                f"{item_type}: chybí cílový model {model_file.relative_to(pack_root)}."
            )

        summaries.append(
            f"{item_type}: Material.{actual_material}, CustomModelData {java_value} "
            f"-> {specification['model_id']}"
        )
    return summaries


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Verify Java event item IDs and the resource-pack item models."
    )
    parser.add_argument(
        "--java",
        default="src/main/java/cz/vanocni/features/HolidayEventManager.java",
        help="Path to HolidayEventManager.java",
    )
    parser.add_argument("--pack", default="resourcepack", help="Resource-pack directory")
    args = parser.parse_args()

    try:
        java_source = Path(args.java).read_text(encoding="utf-8")
        for summary in verify_contract(java_source, Path(args.pack)):
            print(f"OK: {summary}")
    except (OSError, ContractError) as exc:
        print(f"CHYBA: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
