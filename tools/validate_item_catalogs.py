#!/usr/bin/env python3
"""Valida posição, referências e visibilidade dos catálogos de itens."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOGS = ROOT / "catalogs"
VALID_TIERS = {"COMMON", "UNCOMMON", "RARE", "ANCESTRAL"}
VALID_TARGETS = {"WEAPON", "ARMOR"}
EFFECT_FIELDS = {"durability", "damageBonus", "damageReduction", "pg", "pl", "categoryDieShift", "agilityLimit", "traitIds"}

def load(name):
    with (CATALOGS / name).open(encoding="utf-8") as stream:
        return json.load(stream)

def unique(entries, label):
    ids = [entry["id"] for entry in entries]
    repeated = sorted({item_id for item_id in ids if ids.count(item_id) > 1})
    if repeated:
        raise ValueError(f"{label}: ids repetidos: {', '.join(repeated)}")
    return set(ids)

def main():
    materials = load("materials.json")["materials"]
    traits = load("item_traits.json")["traits"]
    costs = load("item_economy.json")["costs"]
    modifications = load("modifications.json")["entries"]
    material_ids = unique(materials, "materiais")
    trait_ids = unique(traits, "traços")
    cost_ids = unique(costs, "custos")
    unique(modifications, "modificações")

    if len(materials) < 30 or len(modifications) < 30:
        raise ValueError("os catálogos devem conter pelo menos 30 materiais e 30 modificações")

    if material_ids != cost_ids:
        raise ValueError("todo material deve ter exatamente um custo em dinheiro e PH")

    for cost in costs:
        if set(cost) != {"id", "money", "ph"} or cost["money"] < 0 or cost["ph"] < 0:
            raise ValueError(f"custo inválido: {cost.get('id')}")

    ancestral_trait_ids = {t["id"] for t in traits if t.get("ancestral")}
    for trait in traits:
        if not trait.get("description", "").strip():
            raise ValueError(f"traço sem descrição: {trait['id']}")
        if not set(trait["appliesTo"]) <= VALID_TARGETS:
            raise ValueError(f"aplicação inválida no traço {trait['id']}")
        if trait.get("ancestral") and trait["characterCreationVisible"]:
            raise ValueError(f"traço ancestral exposto na criação: {trait['id']}")

    for material in materials:
        if material["tier"] not in VALID_TIERS:
            raise ValueError(f"raridade inválida: {material['id']}")
        if "price" in material or "money" in material or "ph" in material:
            raise ValueError(f"preço fora do catálogo econômico: {material['id']}")
        if set(material["effects"]) != {"weapon", "armor"}:
            raise ValueError(f"material sem efeitos separados: {material['id']}")
        if material["tier"] == "ANCESTRAL" and material["characterCreationVisible"]:
            raise ValueError(f"material ancestral exposto na criação: {material['id']}")
        for target, effect in material["effects"].items():
            unknown = set(effect) - EFFECT_FIELDS
            if unknown:
                raise ValueError(f"campos no lugar errado em {material['id']}/{target}: {sorted(unknown)}")
            missing_traits = set(effect.get("traitIds", [])) - trait_ids
            if missing_traits:
                raise ValueError(f"traços inexistentes em {material['id']}: {sorted(missing_traits)}")
            if material["characterCreationVisible"] and set(effect.get("traitIds", [])) & ancestral_trait_ids:
                raise ValueError(f"material de criação referencia traço ancestral: {material['id']}")

    legacy_items = load("items.json")["entries"]
    costs_by_id = {entry["id"]: entry for entry in costs}
    materials_by_id = {entry["id"]: entry for entry in materials}
    for entry in legacy_items:
        if entry.get("kind") not in {"WEAPON_MATERIAL", "ARMOR_MATERIAL"} or entry["id"] not in materials_by_id:
            continue
        material = materials_by_id[entry["id"]]
        target = "weapon" if entry["kind"] == "WEAPON_MATERIAL" else "armor"
        expected = material["effects"][target]
        economy = costs_by_id[entry["id"]]
        if entry["creationCost"] != economy["ph"] or entry["price"] != economy["money"]:
            raise ValueError(f"economia divergente no material legado: {entry['id']}/{target}")
        legacy_projection = {
            "durability": entry.get("durability", 0),
            "pg": entry.get("pg", 0),
            "pl": entry.get("pl", 0),
            "agilityLimit": entry.get("agilityLimit"),
            "damageReduction": entry.get("damageReduction", 0),
            "damageBonus": entry.get("damageBonus", 0),
            "categoryDieShift": entry.get("categoryDieShift", 0),
            "traitIds": entry.get("traitIds", []),
        }
        expected_projection = {
            "durability": expected.get("durability", 0),
            "pg": expected.get("pg", 0),
            "pl": expected.get("pl", 0),
            "agilityLimit": expected.get("agilityLimit"),
            "damageReduction": expected.get("damageReduction", 0),
            "damageBonus": expected.get("damageBonus", 0),
            "categoryDieShift": expected.get("categoryDieShift", 0),
            "traitIds": expected.get("traitIds", []),
        }
        if legacy_projection != expected_projection:
            raise ValueError(f"efeitos divergentes no material legado: {entry['id']}/{target}")

    for path in sorted(CATALOGS.glob("*.json")):
        if path.name.endswith(".schema.json"):
            continue
        document = json.loads(path.read_text(encoding="utf-8"))
        if "schemaVersion" not in document:
            raise ValueError(f"catálogo sem schemaVersion: {path.name}")
        for collection in ("entries", "materials", "traits", "costs"):
            values = document.get(collection)
            if isinstance(values, list) and values and all(isinstance(value, dict) and "id" in value for value in values):
                identities = [f"{value.get('kind', '')}:{value['id']}" for value in values]
                if len(identities) != len(set(identities)):
                    raise ValueError(f"ids repetidos no mesmo tipo: {path.name}/{collection}")

    print(f"OK: {len(materials)} materiais, {len(modifications)} modificações, {len(traits)} traços e {len(costs)} custos")

if __name__ == "__main__":
    main()
