#!/usr/bin/env python3
"""Generate canonical gem and technology catalogs and the Kotlin gem adapter."""
import argparse
import json
import re
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ITEM_TYPES = ["Arma", "Armadura", "Acessório", "Escudo"]
COST = {"COMMON": (2, 45, 4), "UNCOMMON": (3, 90, 5), "RARE": (5, 250, 6), "ANCESTRAL": (8, 600, 8)}


def slug(value):
    value = ''.join(c for c in unicodedata.normalize('NFD', value) if unicodedata.category(c) != 'Mn')
    return re.sub(r'[^a-z0-9]+', '_', value.lower()).strip('_')


def effect(kind, value, target, description, condition="WIELDED"):
    return {"type": kind, "value": value, "target": target, "condition": condition, "description": description}


def entry(identifier, name, rarity, category, origin, payload, technology=False, types=None, active=False):
    ph, money, durability = COST[rarity]
    return {
        "id": identifier, "name": name, "rarity": rarity, "category": category, "origin": origin,
        "characterCreationVisible": rarity != "ANCESTRAL", "compatibleItemTypes": types or ITEM_TYPES,
        "compatibleBaseGroups": [], "compatibleBaseIds": [], "creationCost": ph, "price": money,
        "load": 0, "durability": durability, "region": "", "pg": 0, "pl": 0, "agilityLimit": None,
        "maxCharges": (6 if rarity == "ANCESTRAL" else 3) if active else 0,
        "recharge": "Recupera 1 carga com uma Ação; carga total após 8 horas" if active else "Nenhuma",
        "activation": "ACTION" if active else "PASSIVE",
        "installationKnowledge": "Engenharia ou Ofício apropriado" if technology else "Ourivesaria",
        "installationDifficulty": "Definida pela combinação entre aprimoramento e item",
        "removalDifficulty": "Definida pela combinação entre aprimoramento e item",
        "failureEnhancementDamage": 1 if rarity in {"COMMON", "UNCOMMON"} else 2,
        "failureItemDamage": 0 if rarity == "COMMON" else 1, "effect": payload,
    }


def build_gems():
    specs = [
        ("Força", "COMMON", "ATTRIBUTE", "Veio Rubro", effect("ATTRIBUTE", 1, "FOR", "+1 em Força.")),
        ("Vigor", "COMMON", "ATTRIBUTE", "Nódulo Bestial", effect("ATTRIBUTE", 1, "VIG", "+1 em Vigor.")),
        ("Agilidade", "UNCOMMON", "ATTRIBUTE", "Cristal de Vendaval", effect("ATTRIBUTE", 1, "AGI", "+1 em Agilidade.")),
        ("Poder", "RARE", "ATTRIBUTE", "Geodo Onírico", effect("ATTRIBUTE", 1, "POD", "+1 em Poder.")),
        ("Intelecto", "UNCOMMON", "ATTRIBUTE", "Quartzo Mnemônico", effect("ATTRIBUTE", 1, "INT", "+1 em Intelecto.")),
        ("Carisma", "RARE", "ATTRIBUTE", "Pérola de Eco", effect("ATTRIBUTE", 1, "CAR", "+1 em Carisma.")),
        ("Saber Errante", "COMMON", "KNOWLEDGE", "Ruína Onírica", effect("KNOWLEDGE", 1, "*", "+1 em um Conhecimento sorteado ao receber a gema.")),
        ("Ourives", "UNCOMMON", "KNOWLEDGE", "Oficina Afundada", effect("KNOWLEDGE", 2, "INT:Ourivesaria", "+2 em Ourivesaria.")),
        ("Engenho", "RARE", "KNOWLEDGE", "Núcleo Kaltoch", effect("KNOWLEDGE", 2, "INT:Engenharia", "+2 em Engenharia.")),
        ("Vigília", "COMMON", "KNOWLEDGE", "Olho Mineral", effect("KNOWLEDGE", 1, "POD:Sentidos", "+1 em Sentidos.")),
        ("Lâmina Onírica", "UNCOMMON", "OFFENSIVE", "Obsidiana Sonhadora", effect("MAGIC_DAMAGE", 1, "damage", "+1 de dano mágico.")),
        ("Mira Cintilante", "COMMON", "OFFENSIVE", "Areia Estelar", effect("ATTACK", 1, "attack", "+1 em Ataque.")),
        ("Impacto Rubro", "RARE", "OFFENSIVE", "Coração de Colosso", effect("PHYSICAL_DAMAGE", 2, "damage", "+2 de dano físico.")),
        ("Corte Fantasma", "RARE", "OFFENSIVE", "Fenda Onírica", effect("DIE_CATEGORY", 1, "damage", "+1 Categoria de Dado no dano.")),
        ("Ruína Ancestral", "ANCESTRAL", "OFFENSIVE", "Arsenal Kaltoch", effect("MAGIC_DAMAGE", 3, "damage", "+3 de dano mágico.")),
        ("Barreira", "COMMON", "DEFENSIVE", "Cristal Bastilha", effect("PG", 1, "protection", "+1 PG.", "EQUIPPED")),
        ("Bastião", "UNCOMMON", "DEFENSIVE", "Casco de Leviatã", effect("PL", 2, "protection", "+2 PL.", "EQUIPPED")),
        ("Resistência", "RARE", "DEFENSIVE", "Âmbar de Monstro", effect("DAMAGE_REDUCTION", 1, "all", "Redução de Dano 1.", "EQUIPPED")),
        ("Integridade", "COMMON", "DEFENSIVE", "Mina Profunda", effect("DURABILITY", 2, "durability", "+2 de Durabilidade máxima.")),
        ("Escudo Sólido", "ANCESTRAL", "DEFENSIVE", "Luz Sólida", effect("PG", 3, "protection", "+3 PG.", "EQUIPPED")),
        ("Pulso", "UNCOMMON", "POWER", "Tempestade Onírica", effect("GEM_POWER", 0, "push", "Com uma Ação, empurre um alvo próximo em 1 quadrado.")),
        ("Véu", "RARE", "POWER", "Névoa Mineral", effect("GEM_POWER", 0, "veil", "Com uma Ação, fique obscurecido até seu próximo turno.")),
        ("Eco", "RARE", "POWER", "Caverna Ressonante", effect("GEM_POWER", 0, "echo", "Com uma Ação, repita o último som ouvido.")),
        ("Restauro", "RARE", "POWER", "Fonte Onírica", effect("GEM_POWER", 0, "repair", "Com uma Ação, restaure 1 de Durabilidade do item.")),
        ("Portal Breve", "ANCESTRAL", "POWER", "Portal Kaltoch", effect("GEM_POWER", 0, "teleport", "Com uma Ação, desloque-se até 3 quadrados visíveis.")),
        ("Luz", "COMMON", "UTILITY", "Geodo Solar", effect("RULE", 0, "light", "Com uma Ação, ilumine a área próxima.")),
        ("Bússola", "COMMON", "UTILITY", "Magnetita Onírica", effect("RULE", 0, "direction", "Aponta para um destino gravado ao ser instalada.")),
        ("Memória", "UNCOMMON", "UTILITY", "Cristal Mnemônico", effect("RULE", 0, "memory", "Armazena e reproduz uma lembrança sensorial.")),
        ("Chave", "RARE", "UTILITY", "Fechadura Perdida", effect("RULE", 0, "key", "Pode ser vinculada a um mecanismo como chave onírica.")),
        ("Oráculo", "ANCESTRAL", "UTILITY", "Observatório Kaltoch", effect("RULE", 0, "oracle", "Com uma Ação, revela energia onírica próxima.")),
    ]
    result = [entry(f"gema_{slug(name)}", f"Gema de {name}", rarity, category, origin, payload,
                    active=category in {"POWER", "UTILITY"}) for name, rarity, category, origin, payload in specs]
    assert len(result) == len({item["id"] for item in result}) == 30
    return result


def build_technologies():
    themes = [("Térmico", "Oficina Industrial"), ("Tesla", "Laboratório de Bobinas"),
              ("Vapor", "Forja Pressurizada"), ("Kaltoch", "Ruína Kaltoch"),
              ("Gravitacional", "Observatório Ancestral"), ("Onírico", "Instituto de Sonhos")]
    forms = [
        ("Conversor", "RARE", "OFFENSIVE", effect("MAGIC_DAMAGE", 1, "damage", "+1 de dano mágico enquanto houver carga."), ["Arma"]),
        ("Barreira", "RARE", "DEFENSIVE", effect("PG", 1, "protection", "+1 PG enquanto houver carga.", "EQUIPPED"), ["Armadura", "Escudo", "Acessório"]),
        ("Atuador", "RARE", "AUTOMATION", effect("ATTACK", 1, "attack", "+1 em Ataque enquanto houver carga."), ["Arma"]),
        ("Reserva", "RARE", "STORAGE", effect("RULE", 0, "storage", "Com uma Ação, recupera uma carga de outro mecanismo instalado."), ITEM_TYPES),
        ("Núcleo", "ANCESTRAL", "POWER", effect("RULE", 0, "power", "Com uma Ação, ativa o efeito singular do núcleo."), ITEM_TYPES),
    ]
    result = []
    for theme, origin in themes:
        for form, rarity, category, payload, types in forms:
            result.append(entry(f"tech_{slug(theme)}_{slug(form)}", f"{form} {theme}", rarity, category,
                                origin, payload, technology=True, types=types, active=True))
    assert len(result) == len({item["id"] for item in result}) == 30
    return result


def document(kind, entries, version):
    return {"$schema": "./item-components.schema.json", "schemaVersion": 1,
            "catalogVersion": version, "kind": kind, "entries": entries}


def kotlin(doc):
    def q(value): return json.dumps(value, ensure_ascii=False).replace('$', '\\$')
    lines = ['// Generated by tools/generate_gem_catalog.py. Do not edit.',
             'package com.kinderman.sdo.domain.catalog', '',
             'import com.kinderman.sdo.domain.model.ItemEffect',
             'import com.kinderman.sdo.domain.model.ItemEffectCondition',
             'import com.kinderman.sdo.domain.model.ItemEffectType',
             'import com.kinderman.sdo.domain.model.ItemPart', '',
             'internal enum class GemTier { COMMON, UNCOMMON, RARE, ANCESTRAL }',
             'internal data class GemDefinition(val part: ItemPart, val tier: GemTier, val effect: ItemEffect)',
             'internal object GeneratedGemCatalog {', '    val entries = listOf(']
    for item in doc['entries']:
        payload = item['effect']
        lines += ['        GemDefinition(',
                  f'            ItemPart({q(item["id"])}, {q(item["name"])}, "Gema", {item["creationCost"]}, {item["price"]}, durability = {item["durability"]}, effect = {q(payload["description"])}, materialTier = {q(item["rarity"])}, characterCreationVisible = {str(item["characterCreationVisible"]).lower()}),',
                  f'            GemTier.{item["rarity"]},',
                  f'            ItemEffect({q(item["id"])}, ItemEffectType.{payload["type"]}, {payload["value"]}, {q(payload["target"])}, ItemEffectCondition.{payload["condition"]}, {q(payload["description"])}),',
                  '        ),']
    lines += ['    )', '    fun effect(id: String) = entries.firstOrNull { it.part.id == id }?.effect', '}', '']
    return '\n'.join(lines)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--catalog-dir', type=Path, default=ROOT / 'catalogs')
    args = parser.parse_args()
    gems = document("GEM", build_gems(), 3)
    technologies = document("TECHNOLOGY", build_technologies(), 2)
    outputs = {args.catalog_dir / 'gems.json': json.dumps(gems, ensure_ascii=False, indent=2) + '\n',
               args.catalog_dir / 'technologies.json': json.dumps(technologies, ensure_ascii=False, indent=2) + '\n',
               ROOT / 'app/src/main/java/com/kinderman/sdo/domain/catalog/GeneratedGemCatalog.kt': kotlin(gems)}
    for path, content in outputs.items():
        if args.check:
            assert path.read_text() == content, f'{path.name} differs from generated catalog'
        else:
            path.write_text(content)
    print(f'{len(gems["entries"])} gems and {len(technologies["entries"])} technologies validated')
