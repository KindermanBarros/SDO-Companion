#!/usr/bin/env python3
"""Validate the canonical JSON contract and generate Kotlin with no textual inference."""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FILES = ('powers', 'magic', 'ashes', 'runes', 'knowledge')
ABILITY_FILES = {'magic': 'MAGIC', 'ashes': 'ASH', 'runes': 'RUNE'}
FIELDS = {
    'id': str, 'kind': str, 'name': str, 'group': str, 'summary': str,
    'cost': str, 'action': str, 'range': str, 'duration': str, 'source': str,
    'version': int, 'creationCost': str, 'price': int, 'load': int,
    'durability': str, 'region': str, 'relatedAttribute': str,
    'initialValue': (int, type(None)), 'prerequisites': list,
    'mechanicalEffect': str, 'ruleReference': str, 'keywords': list,
    'repeatable': bool, 'limit': str, 'activationCondition': str,
    'enhancements': str, 'deactivationCondition': str,
    'abilitySource': str, 'sourceKnowledge': str,
    'sourceLevel': (int, type(None)), 'abilityCostType': str,
    'abilityCostValue': int, 'abilityExecution': str, 'executionValue': int,
    'executionUnit': str, 'abilityRange': str, 'targetArea': str,
    'abilityDuration': str, 'durationValue': int, 'durationUnit': str,
    'abilityResistance': str,
}
KNOWLEDGE_FIELDS = {
    'id': str, 'type': str, 'name': str, 'category': str, 'description': str,
    'attribute': str, 'initialLevel': int, 'maxLevel': int,
    'prerequisites': list, 'effect': str, 'source': str, 'ruleReference': str,
    'keywords': list, 'repeatable': bool,
}
ABILITY_REQUIRED_FIELDS = {
    'id': str, 'type': str, 'name': str, 'costType': str, 'costValue': int,
    'execution': str, 'range': str, 'targetArea': str, 'duration': str,
    'resistance': str, 'effect': str,
}
ABILITY_OPTIONAL_FIELDS = {
    'source': str, 'knowledge': str, 'level': int, 'purity': str, 'package': str,
    'executionValue': int, 'executionUnit': str, 'durationValue': int,
    'durationUnit': str,
}
COUNTS = {'POWER': 100, 'MAGIC': 50, 'ASH': 150, 'RUNE': 50,
          'ACQUIRED_KNOWLEDGE': 50, 'ARCANE_KNOWLEDGE': 50, 'BATTLE_TECHNIQUE': 50}

SOURCE_VALUES = {'Conhecimento': 'KNOWLEDGE', 'Raça': 'RACE', 'Caminho': 'PATH',
                 'Item': 'ITEM', 'Histórico': 'HISTORY', 'Profissão': 'PROFESSION',
                 'Narrativa': 'NARRATIVE'}
COST_VALUES = {'Nenhum': 'NONE', 'Arcano': 'ARCANE', 'Energia': 'ENERGY',
               'Destino': 'DESTINY', 'Vida': 'LIFE', 'Sanidade': 'SANITY', 'Dose': 'DOSE'}
EXECUTION_VALUES = {'Ação': 'ACTION', 'Turno': 'TURN', 'Livre': 'FREE',
                    'Reação': 'REACTION', 'Passiva': 'PASSIVE', 'Tempo': 'TIME'}
RANGE_VALUES = {'Pessoal': 'PERSONAL', 'Curto': 'SHORT', 'Médio': 'MEDIUM',
                'Longo': 'LONG', 'Indefinido': 'INDEFINITE'}
DURATION_VALUES = {'Instantânea': 'INSTANT', 'Turnos': 'TURNS', 'Cena': 'SCENE',
                   'Sessão': 'SESSION', 'Tempo': 'TIME'}
RESISTANCE_VALUES = {'Nenhuma': 'NONE', 'Geral': 'GENERAL', 'Esquiva': 'DODGE',
                     'Postura': 'POSTURE', 'Mental': 'MENTAL', 'Arcana': 'ARCANE'}
TIME_UNIT_VALUES = {'Minutos': 'MINUTES', 'Horas': 'HOURS', 'Dias': 'DAYS'}
ASH_SOURCE_VALUES = {'Fogo': 'FIRE', 'Frio': 'COLD', 'Raio': 'LIGHTNING', 'Cura': 'HEALING',
                     'Onírico': 'ONEIRIC', 'Ácido': 'ACID', 'Concussivo': 'CONCUSSIVE',
                     'Terra': 'EARTH', 'Natureza': 'NATURE', 'Venenoso': 'POISON',
                     'Água': 'WATER', 'Ar': 'AIR', 'Som': 'SOUND', 'Luz': 'LIGHT',
                     'Trevas': 'DARKNESS', 'Mental': 'MENTAL', 'Ilusão': 'ILLUSION',
                     'Sangue': 'BLOOD', 'Tecnologia': 'TECHNOLOGY', 'Decadência': 'DECAY',
                     'Morte': 'DEATH', 'Divino': 'DIVINE'}
ASH_PURITY_VALUES = {'Bruta': 'RAW', 'Refinada': 'REFINED', 'Pura': 'PURE'}

def knowledge_to_catalog(entry, version):
    return {
        'id': entry['id'], 'kind': entry['type'], 'name': entry['name'],
        'group': entry['category'], 'summary': entry['description'],
        'cost': '', 'action': '', 'range': '', 'duration': '',
        'source': entry['source'], 'version': version, 'creationCost': '',
        'price': 0, 'load': 0, 'durability': '', 'region': '',
        'relatedAttribute': entry['attribute'], 'initialValue': entry['initialLevel'],
        'prerequisites': entry['prerequisites'], 'mechanicalEffect': entry['effect'],
        'ruleReference': entry['ruleReference'], 'keywords': entry['keywords'],
        'repeatable': entry['repeatable'], 'limit': '', 'activationCondition': '',
        'enhancements': '', 'deactivationCondition': '',
    }

def load_knowledge(doc):
    assert doc['schemaVersion'] == 2
    assert doc['name'].strip()
    entries = []
    for entry in doc['knowledges']:
        assert set(entry) == set(KNOWLEDGE_FIELDS), (entry.get('id'), 'knowledge fields')
        for key, expected in KNOWLEDGE_FIELDS.items():
            assert isinstance(entry[key], expected), (entry['id'], key)
            if expected is list:
                assert all(isinstance(value, str) for value in entry[key])
        assert entry['type'] in ('ACQUIRED_KNOWLEDGE', 'ARCANE_KNOWLEDGE', 'BATTLE_TECHNIQUE')
        assert entry['attribute'] in ('FOR', 'AGI', 'VIG', 'INT', 'POD', 'CAR')
        assert entry['initialLevel'] == 1
        assert entry['maxLevel'] == 5
        for key in ('id', 'name', 'category', 'description', 'source', 'effect', 'ruleReference'):
            assert entry[key].strip(), (entry['id'], key)
        entries.append(knowledge_to_catalog(entry, doc['catalogVersion']))
    assert all(sum(entry['kind'] == kind for entry in entries) > 0 for kind in (
        'ACQUIRED_KNOWLEDGE', 'ARCANE_KNOWLEDGE', 'BATTLE_TECHNIQUE'))
    return entries

def display_timed(value, amount, unit):
    return f'{value} — {amount} {unit}' if amount else value

def ability_to_catalog(entry, doc):
    kind = entry['type']
    is_ash = kind == 'ASH'
    group = (f"{entry['source']} / {entry['purity']}" if is_ash else
             f"{entry['knowledge']} {entry['level']}" if kind == 'MAGIC' else
             entry.get('package') or f"Nível {entry['level']}")
    return {
        'id': entry['id'], 'kind': kind, 'name': entry['name'], 'group': group,
        'summary': entry['effect'],
        'cost': f"{entry['costType']} — {entry['costValue']}",
        'action': display_timed(entry['execution'], entry.get('executionValue', 0), entry.get('executionUnit', '')),
        'range': entry['range'],
        'duration': display_timed(entry['duration'], entry.get('durationValue', 0), entry.get('durationUnit', '')),
        'source': doc['sourceDocument'], 'version': doc['catalogVersion'],
        'creationCost': '', 'price': 0, 'load': 0, 'durability': '', 'region': '',
        'relatedAttribute': '', 'initialValue': None, 'prerequisites': [],
        'mechanicalEffect': entry['effect'], 'ruleReference': doc['ruleReference'],
        'keywords': [entry['name'], group], 'repeatable': False, 'limit': '',
        'activationCondition': '', 'enhancements': '', 'deactivationCondition': '',
        'abilitySource': None if is_ash else SOURCE_VALUES[entry['source']],
        'sourceKnowledge': entry.get('knowledge', ''), 'sourceLevel': entry.get('level'),
        'abilityCostType': COST_VALUES[entry['costType']], 'abilityCostValue': entry['costValue'],
        'abilityExecution': EXECUTION_VALUES[entry['execution']],
        'executionValue': entry.get('executionValue', 0),
        'executionUnit': TIME_UNIT_VALUES.get(entry.get('executionUnit', 'Minutos'), 'MINUTES'),
        'abilityRange': RANGE_VALUES[entry['range']], 'targetArea': entry['targetArea'],
        'abilityDuration': DURATION_VALUES[entry['duration']],
        'durationValue': entry.get('durationValue', 0),
        'durationUnit': TIME_UNIT_VALUES.get(entry.get('durationUnit', 'Horas'), 'HOURS'),
        'abilityResistance': RESISTANCE_VALUES[entry['resistance']],
        'catalogAshSource': ASH_SOURCE_VALUES[entry['source']] if is_ash else None,
        'catalogAshPurity': ASH_PURITY_VALUES[entry['purity']] if is_ash else None,
        'runePackage': entry.get('package', ''),
    }

def load_abilities(name, doc):
    assert doc['schemaVersion'] == 2
    assert doc['name'].strip() and doc['sourceDocument'].strip() and doc['ruleReference'].strip()
    entries = []
    allowed = set(ABILITY_REQUIRED_FIELDS) | set(ABILITY_OPTIONAL_FIELDS)
    for entry in doc['abilities']:
        assert set(ABILITY_REQUIRED_FIELDS) <= set(entry) <= allowed, (entry.get('id'), 'ability fields')
        for key, expected in {**ABILITY_REQUIRED_FIELDS, **ABILITY_OPTIONAL_FIELDS}.items():
            if key in entry:
                assert isinstance(entry[key], expected), (entry['id'], key)
        assert entry['type'] == ABILITY_FILES[name]
        assert entry['costType'] in COST_VALUES and entry['costValue'] >= 0
        assert entry['execution'] in EXECUTION_VALUES
        assert entry['range'] in RANGE_VALUES
        assert entry['duration'] in DURATION_VALUES
        assert entry['resistance'] in RESISTANCE_VALUES
        assert entry['name'].strip() and entry['targetArea'].strip() and entry['effect'].strip()
        assert not entry['effect'].lower().startswith('suporte e gatilho:'), (entry['id'], 'effect metadata')
        if entry['execution'] == 'Tempo':
            assert entry.get('executionValue', 0) > 0 and entry.get('executionUnit') in TIME_UNIT_VALUES
        else:
            assert 'executionValue' not in entry and 'executionUnit' not in entry
        if entry['duration'] == 'Turnos':
            assert entry.get('durationValue', 0) > 0 and 'durationUnit' not in entry
        elif entry['duration'] == 'Tempo':
            assert entry.get('durationValue', 0) > 0 and entry.get('durationUnit') in ('Horas', 'Dias')
        else:
            assert 'durationValue' not in entry and 'durationUnit' not in entry
        if entry['type'] == 'ASH':
            assert entry['source'] in ASH_SOURCE_VALUES and entry['purity'] in ASH_PURITY_VALUES
            assert entry['costType'] == 'Dose'
            assert 'knowledge' not in entry and 'level' not in entry
        else:
            assert entry['source'] in SOURCE_VALUES and entry['knowledge'].strip()
            assert 0 <= entry['level'] <= 5
            assert entry['costType'] == 'Arcano'
        if entry['type'] == 'RUNE':
            assert entry['costType'] == 'Arcano' and entry['execution'] == 'Tempo'
        entries.append(ability_to_catalog(entry, doc))
    return entries

def load_catalog(directory):
    entries = []
    for name in FILES:
        doc = json.loads((directory / f'{name}.json').read_text())
        if name == 'knowledge':
            entries.extend(load_knowledge(doc))
            continue
        if name in ABILITY_FILES:
            entries.extend(load_abilities(name, doc))
            continue
        assert doc['schemaVersion'] == 1
        for entry in doc['entries']:
            assert set(entry) == set(FIELDS), (entry.get('id'), 'fields')
            for key, expected in FIELDS.items():
                assert isinstance(entry[key], expected), (entry['id'], key)
                if expected is list:
                    assert all(isinstance(value, str) for value in entry[key])
            assert entry['version'] == doc['catalogVersion']
            for key in ('id', 'name', 'group', 'summary', 'source', 'mechanicalEffect', 'ruleReference'):
                assert entry[key].strip(), (entry['id'], key)
            if entry['kind'] in ('POWER', 'MAGIC', 'ASH', 'RUNE'):
                for key in ('cost', 'action', 'range', 'duration', 'activationCondition', 'deactivationCondition'):
                    assert entry[key].strip(), (entry['id'], key)
                assert entry['activationCondition'].strip() != entry['mechanicalEffect'].strip(), (entry['id'], 'duplicated activation')
                assert 'Profissão:' not in entry['mechanicalEffect'] and 'Categoria:' not in entry['mechanicalEffect'], (entry['id'], 'effect metadata')
                assert entry['targetArea'].strip(), (entry['id'], 'targetArea')
                assert entry['abilitySource'] in SOURCE_VALUES.values(), (entry['id'], 'abilitySource')
                assert entry['abilityCostType'] in COST_VALUES.values(), (entry['id'], 'abilityCostType')
                assert entry['abilityExecution'] in EXECUTION_VALUES.values(), (entry['id'], 'abilityExecution')
                assert entry['abilityRange'] in RANGE_VALUES.values(), (entry['id'], 'abilityRange')
                assert entry['abilityDuration'] in DURATION_VALUES.values(), (entry['id'], 'abilityDuration')
                assert entry['abilityResistance'] in RESISTANCE_VALUES.values(), (entry['id'], 'abilityResistance')
                assert entry['abilityCostType'] in {'ENERGY', 'LIFE', 'SANITY', 'DESTINY'}, (entry['id'], 'power cost type')
                if entry['abilityCostType'] == 'DESTINY':
                    destiny_context = ' '.join([
                        entry['name'], entry['group'], entry['summary'],
                        entry['mechanicalEffect'], *entry['keywords'],
                    ]).lower()
                    assert any(term in destiny_context for term in (
                        'divin', 'sorte', 'destino', 'probabil', 'porcent',
                    )), (entry['id'], 'destiny cost eligibility')
                if entry['abilityExecution'] == 'PASSIVE':
                    assert entry['abilityCostValue'] == 0, (entry['id'], 'passive power cost')
                else:
                    assert entry['abilityCostValue'] > 0, (entry['id'], 'active power cost')
            else:
                assert entry['relatedAttribute'] in ('FOR', 'AGI', 'VIG', 'INT', 'POD', 'CAR')
                assert entry['initialValue'] == 1
                assert not entry['name'].lower().startswith('estudo de ')
            entries.append(entry)
    assert len({entry['id'] for entry in entries}) == len(entries), 'Duplicate IDs'
    actual_counts = {kind: sum(e['kind'] == kind for e in entries) for kind in COUNTS}
    assert all(actual_counts[kind] == count for kind, count in COUNTS.items() if kind not in (
        'ACQUIRED_KNOWLEDGE', 'ARCANE_KNOWLEDGE', 'BATTLE_TECHNIQUE'))
    return entries

def literal(value):
    if value is None: return 'null'
    if isinstance(value, bool): return str(value).lower()
    if isinstance(value, int): return str(value)
    if isinstance(value, list): return 'listOf(' + ', '.join(map(literal, value)) + ')'
    return json.dumps(value, ensure_ascii=False).replace('$', '\\$')

def render(entries):
    result = ['// Generated by tools/generate_catalog.py from catalogs/*.json. Do not edit.',
              'package com.kinderman.sdo.domain.catalog', '',
              'import com.kinderman.sdo.domain.model.CatalogEntry',
              'import com.kinderman.sdo.domain.model.CatalogKind',
              'import com.kinderman.sdo.domain.model.AbilitySource',
              'import com.kinderman.sdo.domain.model.AbilityCostType',
              'import com.kinderman.sdo.domain.model.AbilityExecution',
              'import com.kinderman.sdo.domain.model.AbilityRange',
              'import com.kinderman.sdo.domain.model.AbilityDuration',
              'import com.kinderman.sdo.domain.model.AbilityResistance',
              'import com.kinderman.sdo.domain.model.AbilityTimeUnit',
              'import com.kinderman.sdo.domain.model.AshSource',
              'import com.kinderman.sdo.domain.model.AshPurity', '',
              'internal object CanonicalCatalogData {']
    chunks = [entries[i:i + 20] for i in range(0, len(entries), 20)]
    result += ['    val entries: List<CatalogEntry> by lazy { ' + ' + '.join(f'part{i}()' for i in range(len(chunks))) + ' }']
    for index, chunk in enumerate(chunks):
        result += [f'    private fun part{index}(): List<CatalogEntry> = listOf(']
        for entry in chunk:
            enum_types = {
                'kind': 'CatalogKind', 'abilitySource': 'AbilitySource',
                'abilityCostType': 'AbilityCostType', 'abilityExecution': 'AbilityExecution',
                'executionUnit': 'AbilityTimeUnit', 'abilityRange': 'AbilityRange',
                'abilityDuration': 'AbilityDuration', 'durationUnit': 'AbilityTimeUnit',
                'abilityResistance': 'AbilityResistance', 'catalogAshSource': 'AshSource',
                'catalogAshPurity': 'AshPurity',
            }
            args = [f'{key} = ' + (f'{enum_types[key]}.{value}' if key in enum_types and value is not None else literal(value)) for key, value in entry.items()]
            result += ['        CatalogEntry(', *('            ' + arg + ',' for arg in args), '        ),']
        result += ['    )']
    return '\n'.join(result + ['}', ''])

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--catalog-dir', type=Path, default=ROOT / 'catalogs')
    args = parser.parse_args()
    entries = load_catalog(args.catalog_dir)
    output = ROOT / 'app/src/main/java/com/kinderman/sdo/domain/catalog/CanonicalCatalogData.kt'
    generated = render(entries)
    if args.check:
        assert output.read_text() == generated, 'Generated Kotlin differs from JSON. Run tools/generate_catalog.py.'
    else:
        output.write_text(generated)
    print(f'{len(entries)} structured records validated; Kotlin matches JSON.' if args.check else f'Generated {len(entries)} records.')
