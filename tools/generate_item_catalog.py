#!/usr/bin/env python3
"""Validate the canonical JSON item catalogs consumed directly by the app."""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EFFECT_TYPES = {"ATTRIBUTE", "KNOWLEDGE", "MAGIC_DAMAGE", "DURABILITY", "GEM_POWER", "RULE"}
CONDITIONS = {"EQUIPPED", "WIELDED"}


def validate_effect(entry):
    effect = entry["effect"]
    assert set(effect) == {"type", "value", "target", "condition", "description"}, f'{entry["id"]} has an invalid typed effect contract'
    assert effect["type"] in EFFECT_TYPES, f'{entry["id"]} has unknown effect type'
    assert effect["condition"] in CONDITIONS, f'{entry["id"]} has unknown effect condition'
    assert isinstance(effect["value"], int)
    assert isinstance(effect["target"], str)
    assert effect["description"].strip()


def validate(items, modifications):
    expected_kinds = {"WEAPON_MATERIAL", "ARMOR_MATERIAL", "WEAPON_BASE", "ARMOR_BASE", "CATALOG_ITEM"}
    assert {entry["kind"] for entry in items["entries"]} == expected_kinds
    identifiers = [f'{entry["kind"]}:{entry["id"]}' for entry in items["entries"]]
    assert len(identifiers) == len(set(identifiers)), "duplicate item-part identifiers"
    modification_ids = [entry["id"] for entry in modifications["entries"]]
    assert len(modification_ids) == len(set(modification_ids)), "duplicate modification identifiers"

    typed_fields = ("id", "name", "creationCost", "price", "load", "durability", "region", "pg", "pl", "agilityLimit")
    for entry in items["entries"] + modifications["entries"]:
        for field in typed_fields:
            assert field in entry, f'{entry.get("id", "unknown")} lacks typed field {field}'
    for entry in items["entries"]:
        if entry["kind"] == "CATALOG_ITEM":
            assert entry.get("source") and entry.get("ruleReference"), f'{entry["id"]} lacks catalog provenance'

    base_groups = {item["group"] for item in items["entries"] if item["kind"].endswith("_BASE")}
    base_ids = {item["id"] for item in items["entries"] if item["kind"].endswith("_BASE")}
    for entry in modifications["entries"]:
        assert "compatibleBaseGroups" in entry and "compatibleBaseIds" in entry
        assert set(entry["compatibleBaseGroups"]) <= base_groups, f'{entry["id"]} has unknown base group'
        assert set(entry["compatibleBaseIds"]) <= base_ids, f'{entry["id"]} has unknown base id'
        validate_effect(entry)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="Retained for a uniform CI interface")
    parser.parse_args()
    items = json.loads((ROOT / "catalogs/items.json").read_text())
    modifications = json.loads((ROOT / "catalogs/modifications.json").read_text())
    validate(items, modifications)
    print(f'{len(items["entries"])} item definitions and {len(modifications["entries"])} modifications validated')
