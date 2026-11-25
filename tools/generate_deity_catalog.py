#!/usr/bin/env python3
"""Import and validate the 16 canonical SDO deity sheets from the RPG vault.

Enforces strict data-layer spoiler separation:
- Player-safe public entries (`Para Jogadores (O Que Se Sabe)` only) are written to
  `catalogs/deities.json` (which feeds `built_in_catalog.json`).
- GM-only content (`Para o Mestre (Origem, Motivos e Postura)`, Postura/Tabu, Pillars/Stats,
  Domain Acts, Herald Gifts, Contracts, Behavior, Table Hooks, Central Mysteries) is written
  exclusively to `firebase/deity_gm_catalog.json` outside bundled client assets.
"""
from __future__ import annotations

import argparse
import json
import re
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_VAULT = ROOT.parent / "solidao-dos-oprimidos"
PUBLIC_OUT = ROOT / "catalogs/deities.json"
GM_OUT = ROOT / "firebase/deity_gm_catalog.json"

STABLE_ID_BY_TITLE = {
    "Edris Vhal — O Primeiro a Voltar": "deity.edris_vhal",
    "Namaris — Senhora da Bruma": "deity.namaris",
    "Fergus — O Ferreiro do Último Prego": "deity.fergus",
    "Ilyra — A Pastora do Último Sono": "deity.ilyra",
    "Vespera — A Menina dos Mil Rostos": "deity.vespera",
    "Rhais — A Ferrugem Órfã": "deity.rhais",
    "Cartola Vermelha": "deity.cartola_vermelha",
    "Vael — O Arauto Amarelo": "deity.vael",
    "Ernest — O Anfitrião do Oásis": "deity.ernest",
    "Soren — O Faroleiro Sem Costa": "deity.soren",
    "Maera — A Senhora da Sobra": "deity.maera",
    "Kaelith — O Escriba das Margens": "deity.kaelith",
    "Tarek — O Vigia do Segundo Silêncio": "deity.tarek",
    "Liora — A Agulha de Latão": "deity.liora",
    "O Coro Sem Língua": "deity.coro_sem_lingua",
    "A Noiva": "deity.a_noiva",
}

FORBIDDEN_PUBLIC_TERMS = (
    "para o mestre",
    "kaltoch",
    "corvain ashtrell",
    "testemunha apagada",
    "apollyon",
    "ganchos de mesa",
    "mistério central",
)


def strip_wikilinks(text: str) -> str:
    cleaned = re.sub(r"\[\[([^\]|]+)\|([^\]]+)\]\]", r"\2", text)
    cleaned = re.sub(r"\[\[([^\]#]+)(?:#[^\]]+)?\]\]", r"\1", cleaned)
    return cleaned.strip()


def normalize_ws(text: str) -> str:
    lines = [line.rstrip() for line in text.replace("\r\n", "\n").split("\n")]
    out: list[str] = []
    blank = False
    for line in lines:
        if line.strip() == "---":
            continue
        if not line.strip():
            if not blank and out:
                out.append("")
            blank = True
        else:
            out.append(line)
            blank = False
    while out and not out[-1]:
        out.pop()
    return "\n".join(out)


def clean_block(text: str) -> str:
    return normalize_ws(strip_wikilinks(text))


def classify_tier(raw_category: str) -> tuple[str, str, str]:
    low = raw_category.lower()
    if "deus maior" in low:
        return "GREATER_GOD", "Deus Maior", "CONCEPTUAL_PILLARS"
    if "deus menor" in low:
        return "LESSER_GOD", "Deus Menor", "CONCEPTUAL_PILLARS"
    if "semideusa" in low:
        return "DEMIGOD", "Semideusa", "TACTICAL"
    if "semideus" in low:
        return "DEMIGOD", "Semideus", "TACTICAL"
    if "avatar" in low:
        return "AVATAR", "Avatar", "TACTICAL"
    raise ValueError(f"Unrecognized deity category: {raw_category}")


def parse_index(index_path: Path) -> list[dict[str, str]]:
    text = index_path.read_text(encoding="utf-8")
    rows: list[dict[str, str]] = []
    for line in text.splitlines():
        if not line.startswith("| [["):
            continue
        parts = [cell.strip() for cell in line.strip("|").split("|")]
        if len(parts) < 3:
            continue
        match = re.match(r"\[\[([^\]]+)\]\]", parts[0])
        if not match:
            continue
        title = match.group(1).strip()
        rows.append(
            {
                "title": title,
                "publicImage": strip_wikilinks(parts[1]),
                "centralMystery": strip_wikilinks(parts[2]),
            }
        )
    assert len(rows) == 16, f"Expected 16 index entries in Deidades.md, found {len(rows)}"
    return rows


def parse_markdown_kv_table(section_text: str) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in section_text.splitlines():
        striped = strip_wikilinks(line.strip())
        if not striped.startswith("|") or "---" in striped:
            continue
        cells = [c.strip() for c in striped.strip("|").split("|")]
        if len(cells) < 2 or cells[0].lower() == "campo":
            continue
        key = re.sub(r"\*+", "", cells[0]).strip()
        result[key] = " | ".join(cells[1:]).strip()
    return result


def parse_vertical_numeric_table(section_text: str) -> dict[str, int]:
    parsed: dict[str, int] = {}
    for line in section_text.splitlines():
        striped = strip_wikilinks(line.strip())
        if not striped.startswith("|") or "---" in striped:
            continue
        cells = [re.sub(r"\*+", "", c).strip() for c in striped.strip("|").split("|")]
        if len(cells) < 2 or cells[0].lower() in ("recurso", "proteção", "campo"):
            continue
        num_match = re.search(r"-?\d+", cells[1])
        if num_match:
            parsed[cells[0]] = int(num_match.group(0))
    return parsed
def parse_single_row_numeric_table(section_text: str) -> dict[str, int]:
    table_lines = [
        line.strip()
        for line in section_text.splitlines()
        if line.strip().startswith("|") and "---" not in line
    ]
    if len(table_lines) < 2:
        return {}
    headers = [re.sub(r"\*+", "", c).strip() for c in table_lines[0].strip("|").split("|")]
    values = [c.strip() for c in table_lines[1].strip("|").split("|")]
    parsed: dict[str, int] = {}
    for header, value in zip(headers, values):
        num_match = re.search(r"-?\d+", value)
        if num_match:
            parsed[header] = int(num_match.group(0))
    return parsed


def split_h2_sections(text: str) -> tuple[str, list[tuple[str, str]]]:
    matches = list(re.finditer(r"^##\s+(.+)$", text, flags=re.MULTILINE))
    preamble = text[: matches[0].start()] if matches else text
    sections: list[tuple[str, str]] = []
    for idx, match in enumerate(matches):
        heading = match.group(1).strip()
        start = match.end()
        end = matches[idx + 1].start() if idx + 1 < len(matches) else len(text)
        sections.append((heading, text[start:end]))
    return preamble, sections


def parse_h3_subsections(section_body: str) -> list[dict[str, str]]:
    matches = list(re.finditer(r"^###\s+(.+)$", section_body, flags=re.MULTILINE))
    items: list[dict[str, str]] = []
    for idx, match in enumerate(matches):
        title = strip_wikilinks(match.group(1).strip())
        start = match.end()
        end = matches[idx + 1].start() if idx + 1 < len(matches) else len(section_body)
        body = clean_block(section_body[start:end])
        items.append({"name": title, "body": body})
    return items


def parse_herald_gift(section_heading: str, section_body: str) -> dict[str, str]:
    subs = parse_h3_subsections(section_body)
    if not subs:
        return {
            "heading": strip_wikilinks(section_heading),
            "name": "",
            "execution": "Livre",
            "effect": clean_block(section_body),
            "downside": "",
        }
    gift = subs[0]
    raw = gift["body"]
    execution = "Livre"
    quote_lines: list[str] = []
    effect_lines: list[str] = []
    downside = ""
    for line in raw.splitlines():
        s = line.strip()
        if s.startswith(">"):
            quote_lines.append(re.sub(r"^>\s*", "", s).strip("* "))
        elif re.match(r"^[*-]\s*\*\*Contrapartida:\*\*", s):
            downside = re.sub(r"^[*-]\s*\*\*Contrapartida:\*\*\s*", "", s).strip()
        else:
            effect_lines.append(line)
    header_hint = " ".join(quote_lines).lower()
    if "reação" in header_hint:
        execution = "Reação"
    elif "passiva" in header_hint and "ação livre" not in header_hint:
        execution = "Passiva"
    elif "ação livre" in header_hint or "livre" in header_hint:
        execution = "Livre"
    return {
        "heading": strip_wikilinks(section_heading),
        "name": re.sub(r"\s*\*.*\*\s*$", "", gift["name"]).strip(),
        "execution": execution,
        "headerNote": " ".join(quote_lines),
        "effect": normalize_ws("\n".join(effect_lines)),
        "downside": downside,
    }


def parse_sheet(sheet_path: Path, order: int, index_meta: dict[str, str]) -> tuple[dict, dict]:
    raw = sheet_path.read_text(encoding="utf-8")
    preamble, sections = split_h2_sections(raw)

    title_match = re.search(r"^#\s+(.+)$", preamble, flags=re.MULTILINE)
    assert title_match, f"Missing H1 title in {sheet_path.name}"
    title = title_match.group(1).strip()
    assert title == index_meta["title"], f"Title mismatch: {title} != {index_meta['title']}"
    deity_id = STABLE_ID_BY_TITLE[title]

    tags: list[str] = []
    quote_lines: list[str] = []
    intro_lines: list[str] = []
    for line in preamble.splitlines():
        s = line.strip()
        if not s or s.startswith("# ") or s == "---":
            continue
        if s.startswith("#") and not s.startswith("##"):
            tags.extend(tok.lstrip("#") for tok in s.split() if tok.startswith("#"))
        elif s.startswith(">"):
            quote_lines.append(strip_wikilinks(re.sub(r"^>\s*", "", s)))
        else:
            intro_lines.append(strip_wikilinks(s))

    sec_map = {heading: body for heading, body in sections}
    identity_table = parse_markdown_kv_table(sec_map.get("Identidade", ""))
    raw_category = identity_table.get("Categoria", "")
    tier_code, public_tier_label, mechanical_scale = classify_tier(raw_category)

    para_jogadores_key = next((k for k in sec_map if k.startswith("Para Jogadores")), None)
    para_mestre_key = next((k for k in sec_map if k.startswith("Para o Mestre")), None)
    assert para_jogadores_key and para_mestre_key, f"{title} must separate Para Jogadores and Para o Mestre"

    para_jogadores = clean_block(sec_map[para_jogadores_key])
    para_mestre = clean_block(sec_map[para_mestre_key])
    assert para_jogadores, f"Empty Para Jogadores in {title}"
    assert para_mestre, f"Empty Para o Mestre in {title}"

    # Linha de Pensamento e Tabu
    linha_raw = clean_block(sec_map.get("Linha de Pensamento e Tabu", ""))
    tabu = ""
    linha_lines: list[str] = []
    for line in linha_raw.splitlines():
        s = line.strip()
        if re.match(r"^[*-]\s*\*\*Tabu:\*\*", s):
            tabu = re.sub(r"^[*-]\s*\*\*Tabu:\*\*\s*", "", s).strip()
        else:
            linha_lines.append(line)
    linha_de_pensamento = normalize_ws("\n".join(linha_lines))

    # Pillars vs Tactical Stats
    pillars = None
    tactical_stats = None
    destiny_points = 0
    if "Pilares Divinos e Destino" in sec_map:
        p_row = parse_single_row_numeric_table(sec_map["Pilares Divinos e Destino"])
        pillars = {
            "aspecto": p_row["Aspecto"],
            "dominio": p_row["Domínio"],
            "santuario": p_row["Santuário"],
            "presenca": p_row["Presença"],
        }
        destiny_points = p_row["Pontos de Destino"]
    else:
        res_row = parse_vertical_numeric_table(sec_map.get("Recursos", ""))
        attr_row = parse_single_row_numeric_table(sec_map.get("Atributos", ""))
        prot_row = parse_vertical_numeric_table(sec_map.get("Proteções", ""))
        destiny_points = res_row.get("Pontos de Destino", 0)
        tactical_stats = {
            "pv": res_row.get("Vida (PV)", 0),
            "pe": res_row.get("Energia (PE)", 0),
            "pm": res_row.get("Arcano (PM)", 0),
            "ps": res_row.get("Sanidade (PS)", 0),
            "pd": destiny_points,
            "attributes": attr_row,
            "protections": prot_row,
        }

    corruption_table = parse_markdown_kv_table(sec_map.get("Corrupção Divina Irradiada", ""))

    acts_key = next(
        (k for k in sec_map if k.startswith("Atos de Domínio") or k.startswith("Ações e ")),
        None,
    )
    domain_acts: list[dict] = []
    if acts_key:
        for act in parse_h3_subsections(sec_map[acts_key]):
            cost_match = re.search(r"\*\*Custo:\*\*\s*(\d+)\s*Pontos? de Destino", act["body"])
            cost_pd = int(cost_match.group(1)) if cost_match else 0
            dc_match = re.search(r"CD\s*(\d+)", act["body"])
            listed_dc = int(dc_match.group(1)) if dc_match else None
            domain_acts.append(
                {
                    "name": act["name"],
                    "costPd": cost_pd,
                    "listedDc": listed_dc,
                    "description": act["body"],
                }
            )

    passives_raw = clean_block(sec_map.get("Capacidades Passivas", ""))
    passives: list[dict[str, str]] = []
    for paragraph in re.split(r"\n\s*\n", passives_raw):
        p = paragraph.strip()
        if not p:
            continue
        m = re.match(r"^\*\*(.+?)\*\*\s*[—-]\s*(.+)$", p, flags=re.DOTALL)
        if m:
            passives.append({"name": m.group(1).strip(), "description": m.group(2).strip()})
        else:
            passives.append({"name": "Capacidade Passiva", "description": p})

    gift_key = next((k for k in sec_map if k.startswith("Dádiva de ")), "")
    herald_gift = parse_herald_gift(gift_key, sec_map.get(gift_key, ""))

    extra_sections: list[dict[str, str]] = []
    handled_prefixes = (
        "Identidade",
        "Para Jogadores",
        "Para o Mestre",
        "Linha de Pensamento e Tabu",
        "Pilares Divinos e Destino",
        "Recursos",
        "Atributos",
        "Proteções",
        "Corrupção Divina Irradiada",
        "Atos de Domínio",
        "Ações e ",
        "Capacidades Passivas",
        "Dádiva de ",
        "Comportamento",
        "Ganchos de Mesa",
    )
    for heading, body in sections:
        if not any(heading.startswith(pref) for pref in handled_prefixes):
            extra_sections.append(
                {"heading": strip_wikilinks(heading), "body": clean_block(body)}
            )
        elif heading.startswith("Contratos, Bênçãos ou Marcas"):
            extra_sections.append(
                {"heading": strip_wikilinks(heading), "body": clean_block(body)}
            )

    # Player-safe keywords (derived strictly from public tier + public tags without GM secrets)
    public_keywords = ["Deidades", public_tier_label]

    public_entry = {
        "id": deity_id,
        "navigationOrder": order,
        "kind": "DEITY",
        "name": title,
        "group": public_tier_label,
        "tierCode": tier_code,
        "paraJogadoresHeading": "Para Jogadores (O Que Se Sabe)",
        "paraJogadores": para_jogadores,
        "source": "Compêndio de Deidades",
        "ruleReference": "02 - Compêndio/Entidades/NPCs/Deidades/Deidades.md",
        "keywords": public_keywords,
    }

    gm_entry = {
        "id": deity_id,
        "navigationOrder": order,
        "name": title,
        "tierCode": tier_code,
        "publicTierLabel": public_tier_label,
        "rawCategory": raw_category,
        "mechanicalScale": mechanical_scale,
        "epigraph": "\n".join(quote_lines),
        "overview": "\n\n".join(intro_lines),
        "publicImageSummary": index_meta["publicImage"],
        "centralMystery": index_meta["centralMystery"],
        "identity": {
            "categoria": raw_category,
            "dominio": identity_table.get("Domínio", ""),
            "santuario": identity_table.get("Santuário", ""),
            "postura": identity_table.get("Postura", ""),
            "culto": identity_table.get("Culto", ""),
            "manifestacao": identity_table.get("Manifestação", ""),
        },
        "paraJogadoresHeading": "Para Jogadores (O Que Se Sabe)",
        "paraJogadores": para_jogadores,
        "paraOMestreHeading": "Para o Mestre (Origem, Motivos e Postura)",
        "paraOMestre": para_mestre,
        "linhaDePensamento": linha_de_pensamento,
        "tabu": tabu,
        "pillars": pillars,
        "tacticalStats": tactical_stats,
        "destinyPoints": destiny_points,
        "irradiatedCorruption": {
            "intensidade": corruption_table.get("Intensidade de exposição", ""),
            "efeitoPeculiar": corruption_table.get("Efeito peculiar", ""),
        },
        "actsHeading": strip_wikilinks(acts_key) if acts_key else "",
        "domainActs": domain_acts,
        "passiveCapabilities": passives,
        "heraldGift": herald_gift,
        "extraSections": extra_sections,
        "behavior": clean_block(sec_map.get("Comportamento", "")),
        "tableHooks": clean_block(sec_map.get("Ganchos de Mesa", "")),
        "tags": tags,
    }
    return public_entry, gm_entry


def validate_outputs(public_doc: dict, gm_doc: dict) -> None:
    assert public_doc.get("schemaVersion") == 1
    assert gm_doc.get("schemaVersion") == 1
    pub_entries = public_doc.get("deities", [])
    gm_entries = gm_doc.get("deities", [])
    assert len(pub_entries) == 16, f"Expected 16 public deities, got {len(pub_entries)}"
    assert len(gm_entries) == 16, f"Expected 16 GM deities, got {len(gm_entries)}"

    pub_ids = [e["id"] for e in pub_entries]
    gm_ids = [e["id"] for e in gm_entries]
    assert len(set(pub_ids)) == 16, "Duplicate public deity IDs"
    assert pub_ids == gm_ids, "Public and GM deity ID ordering mismatch"

    tiers = [e["tierCode"] for e in pub_entries]
    gods_count = sum(t in ("LESSER_GOD", "GREATER_GOD") for t in tiers)
    demigods_count = sum(t == "DEMIGOD" for t in tiers)
    avatars_count = sum(t == "AVATAR" for t in tiers)
    assert gods_count == 12, f"Expected 12 Lesser/Greater Gods, got {gods_count}"
    assert demigods_count == 3, f"Expected 3 Demigods, got {demigods_count}"
    assert avatars_count == 1, f"Expected 1 Avatar, got {avatars_count}"

    serialized_public = json.dumps(public_doc, ensure_ascii=False).lower()
    for term in FORBIDDEN_PUBLIC_TERMS:
        assert term not in serialized_public, f"Spoiler leak in public deity catalog: {term}"

    for pub, gm in zip(pub_entries, gm_entries):
        assert pub["paraJogadores"].strip(), f"Missing Para Jogadores in {pub['id']}"
        assert gm["paraOMestre"].strip(), f"Missing Para o Mestre in {gm['id']}"
        assert gm["tabu"].strip(), f"Missing Tabu in {gm['id']}"
        assert gm["identity"]["postura"].strip(), f"Missing Postura in {gm['id']}"
        if gm["mechanicalScale"] == "CONCEPTUAL_PILLARS":
            assert gm["pillars"] is not None, f"Missing pillars on god {gm['id']}"
            for p_name, p_val in gm["pillars"].items():
                assert 0 <= p_val <= 5, f"Pillar {p_name}={p_val} out of 0..5 on {gm['id']}"
        else:
            assert gm["tacticalStats"] is not None, f"Missing tactical stats on {gm['id']}"
            assert gm["tacticalStats"]["pv"] > 0, f"Invalid PV on {gm['id']}"


def build_from_vault(vault_dir: Path) -> tuple[dict, dict]:
    deities_dir = vault_dir / "02 - Compêndio/Entidades/NPCs/Deidades"
    index_path = deities_dir / "Deidades.md"
    assert index_path.is_file(), f"Missing vault index: {index_path}"
    md_files = sorted(deities_dir.glob("*.md"))
    assert len(md_files) == 17, f"Expected 17 Markdown files in {deities_dir}, found {len(md_files)}"

    index_rows = parse_index(index_path)
    public_entries: list[dict] = []
    gm_entries: list[dict] = []
    for order, row in enumerate(index_rows, start=1):
        # Match NFC/NFD normalized filename
        target_norm = unicodedata.normalize("NFC", f"{row['title']}.md")
        sheet_path = next(
            (p for p in md_files if unicodedata.normalize("NFC", p.name) == target_norm),
            None,
        )
        assert sheet_path is not None, f"Missing sheet file for {row['title']}"
        pub, gm = parse_sheet(sheet_path, order, row)
        public_entries.append(pub)
        gm_entries.append(gm)

    public_doc = {
        "schemaVersion": 1,
        "catalogVersion": 4,
        "name": "Compêndio de Deidades (Para Jogadores)",
        "source": "Compêndio de Deidades",
        "ruleReference": "02 - Compêndio/Entidades/NPCs/Deidades/Deidades.md",
        "deities": public_entries,
    }
    gm_doc = {
        "schemaVersion": 1,
        "catalogVersion": 4,
        "name": "Compêndio de Deidades — Segredos do Mestre",
        "source": "02 - Compêndio/Entidades/NPCs/Deidades",
        "ruleReference": "01 - Regras/04 - Magia e Habilidades/Deidades e Avatares.md",
        "deities": gm_entries,
    }
    validate_outputs(public_doc, gm_doc)
    return public_doc, gm_doc


def render(doc: dict) -> str:
    return json.dumps(doc, ensure_ascii=False, indent=2) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--vault-dir", type=Path, default=DEFAULT_VAULT)
    args = parser.parse_args()

    if args.vault_dir.is_dir():
        public_doc, gm_doc = build_from_vault(args.vault_dir)
        rendered_public = render(public_doc)
        rendered_gm = render(gm_doc)
        if args.check:
            assert PUBLIC_OUT.read_text(encoding="utf-8") == rendered_public, (
                "catalogs/deities.json differs from vault source. Run tools/generate_deity_catalog.py."
            )
            assert GM_OUT.read_text(encoding="utf-8") == rendered_gm, (
                "firebase/deity_gm_catalog.json differs from vault source. Run tools/generate_deity_catalog.py."
            )
            print("16 deities validated against vault source (public + GM split intact).")
        else:
            PUBLIC_OUT.parent.mkdir(parents=True, exist_ok=True)
            GM_OUT.parent.mkdir(parents=True, exist_ok=True)
            PUBLIC_OUT.write_text(rendered_public, encoding="utf-8")
            GM_OUT.write_text(rendered_gm, encoding="utf-8")
            print("Generated 16 deities into catalogs/deities.json and firebase/deity_gm_catalog.json.")
    else:
        assert args.check, f"Vault directory not found at {args.vault_dir}; cannot regenerate without vault."
        public_doc = json.loads(PUBLIC_OUT.read_text(encoding="utf-8"))
        gm_doc = json.loads(GM_OUT.read_text(encoding="utf-8"))
        validate_outputs(public_doc, gm_doc)
        print("16 deities validated from committed catalog artifacts (vault not mounted in CI).")


if __name__ == "__main__":
    main()
