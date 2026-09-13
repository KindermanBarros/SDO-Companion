#!/usr/bin/env python3
"""Valida posição, referências e visibilidade dos catálogos de itens."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOGS = ROOT / "catalogs"
VALID_TIERS = {"COMMON", "UNCOMMON", "RARE", "ANCESTRAL"}
VALID_TARGETS = {"WEAPON", "ARMOR"}
EFFECT_FIELDS = {"durability", "damageBonus", "damageReduction", "agilityLimit", "traitIds"}

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
    material_ids = unique(materials, "materiais")
    trait_ids = unique(traits, "traços")
    cost_ids = unique(costs, "custos")

    if material_ids != cost_ids:
        raise ValueError("todo material deve ter exatamente um custo em dinheiro e PH")

    for cost in costs:
        if set(cost) != {"id", "money", "ph"} or cost["money"] < 0 or cost["ph"] < 0:
            raise ValueError(f"custo inválido: {cost.get('id')}")

    ancestral_trait_ids = {t["id"] for t in traits if t.get("ancestral")}
    for trait in traits:
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

    print(f"OK: {len(materials)} materiais, {len(traits)} traços e {len(costs)} custos")

if __name__ == "__main__":
    main()
