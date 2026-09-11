#!/usr/bin/env python3
"""Generate the complete canonical gem JSON catalog from knowledge data."""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASIC = {
    'FOR': ['Atletismo', 'Brutalidade', 'Luta', 'Arremesso'],
    'VIG': ['Energia', 'Vitalidade', 'Tolerância', 'Regeneração'],
    'AGI': ['Furtividade', 'Reflexos', 'Movimento', 'Pontaria'],
    'POD': ['Arcano', 'Sentidos', 'Controle', 'Recuperação'],
    'INT': ['Sanidade', 'Intuição', 'Religião', 'Raciocínio'],
    'CAR': ['Política', 'Lábia', 'Enganação', 'Intimidação'],
}
ATTRIBUTES = {'FOR': 'Força', 'VIG': 'Vigor', 'AGI': 'Agilidade', 'POD': 'Poder', 'INT': 'Intelecto', 'CAR': 'Carisma'}
THEMES = ['Aurora', 'Basalto', 'Carmesim', 'Cobalto', 'Crepúsculo', 'Éter', 'Ferrugem', 'Marfim', 'Tempestade', 'Vidro']
FORMS = [
    ('Fio', 'MAGIC_DAMAGE', 1, 'damage', 'Converte parte do impacto em dano mágico sutil.'),
    ('Guarda', 'DURABILITY', 1, 'durability', 'Reduz em 1 a primeira perda de Durabilidade de cada cena.'),
    ('Pulso', 'GEM_POWER', 0, 'combat_pulse', 'Uma vez por cena, após acertar, empurre o alvo 1 quadrado.'),
    ('Véu', 'GEM_POWER', 0, 'combat_guard', 'Uma vez por cena, receba +2 Esquiva contra um ataque.'),
    ('Eco', 'GEM_POWER', 0, 'combat_echo', 'Uma vez por cena, repita o menor dado de dano do ataque.'),
]

def slug(value):
    import unicodedata, re
    value = ''.join(c for c in unicodedata.normalize('NFD', value) if unicodedata.category(c) != 'Mn')
    return re.sub(r'[^a-z0-9]+', '_', value.lower()).strip('_')

def effect(kind, value, target, description):
    return {'type': kind, 'value': value, 'target': target, 'condition': 'WIELDED', 'description': description}

def entry(identifier, name, tier, cost, price, payload):
    return {'id': identifier, 'name': name, 'tier': tier, 'compatibleItemTypes': ['Arma'], 'creationCost': cost, 'price': price, 'effect': payload}

def build(catalog_dir):
    knowledge = json.loads((catalog_dir / 'knowledge.json').read_text())['knowledges']
    entries = []
    for acronym, names in BASIC.items():
        for name in names:
            entries.append(entry(f'gema_conhecimento_basico_{slug(acronym + "_" + name)}', f'Gema de {name}', 'MINOR', 2, 45,
                effect('KNOWLEDGE', 1, f'{acronym}:{name}', f'+1 em {name} enquanto a arma estiver empunhada.')))
    for item in knowledge:
        entries.append(entry(f'gema_conhecimento_{slug(item["id"])}', f'Gema de {item["name"]}', 'MINOR', 2, 45,
            effect('KNOWLEDGE', 1, item['name'], f'+1 em {item["name"]} enquanto a arma estiver empunhada.')))
    for acronym, name in ATTRIBUTES.items():
        entries.append(entry(f'gema_atributo_{acronym.lower()}', f'Gema de {name}', 'MAJOR', 4, 150,
            effect('ATTRIBUTE', 1, acronym, f'+1 em {name} enquanto a arma estiver empunhada.')))
    for theme in THEMES:
        for form, kind, value, target, description in FORMS:
            tier = 'MINOR' if kind in ('MAGIC_DAMAGE', 'DURABILITY') else 'MAJOR'
            entries.append(entry(f'gema_aprimoramento_{slug(theme)}_{slug(form)}', f'{form} de {theme}', tier, 3 if tier == 'MINOR' else 6, 90 if tier == 'MINOR' else 400,
                effect(kind, value, target, description)))
    assert len(entries) == len({item['id'] for item in entries})
    assert len([item for item in entries if item['id'].startswith('gema_aprimoramento_')]) == 50
    return {'$schema': './item-components.schema.json', 'schemaVersion': 1, 'catalogVersion': 2, 'kind': 'GEM', 'entries': entries}

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--catalog-dir', type=Path, default=ROOT / 'catalogs')
    args = parser.parse_args()
    document = build(args.catalog_dir)
    json_text = json.dumps(document, ensure_ascii=False, indent=2) + '\n'
    json_path = args.catalog_dir / 'gems.json'
    if args.check:
        assert json_path.read_text() == json_text, 'gems.json differs from canonical knowledge data'
    else:
        json_path.write_text(json_text)
    print(f'{len(document["entries"])} gems validated')
