package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.SpecialKnowledge

object KnowledgeCatalog {
    val entries: List<CatalogEntry> = listOf(
        acquired(
            id = "knowledge.acquired.engenhocaria",
            name = "Engenhocaria",
            category = "Ofício",
            attribute = "INT",
            description = "Conhecimento aplicado à análise, construção, adaptação e reparo de engenhocas e mecanismos.",
            effect = "Pode ser utilizado em testes relacionados a dispositivos, mecanismos e projetos tecnológicos quando a situação exigir especialização.",
            keywords = listOf("ofício", "tecnologia", "mecanismos", "reparo"),
        ),
        acquired(
            id = "knowledge.acquired.linguistica",
            name = "Linguística",
            category = "Conhecimento científico e social",
            attribute = "INT",
            description = "Estudo de idiomas, escritas, padrões linguísticos e interpretação de registros.",
            effect = "Permite reconhecer, comparar e interpretar idiomas e sistemas de escrita de acordo com a dificuldade definida pelas regras ou pela Historiadora.",
            keywords = listOf("idiomas", "escrita", "tradução", "cultura"),
        ),
        acquired(
            id = "knowledge.acquired.culinaria",
            name = "Culinária",
            category = "Ofício",
            attribute = "INT",
            description = "Preparo de alimentos, técnicas culinárias e uso apropriado de ingredientes.",
            effect = "Aplica-se a testes de preparo, conservação, combinação e execução de receitas.",
            keywords = listOf("ofício", "comida", "receita", "ingredientes"),
        ),
        acquired(
            id = "knowledge.acquired.degustacao",
            name = "Degustação",
            category = "Especialização narrativa",
            attribute = "INT",
            description = "Análise sensorial de ingredientes, bebidas e preparos.",
            effect = "Aplica-se à identificação de propriedades perceptíveis de alimentos, bebidas e ingredientes.",
            keywords = listOf("culinária", "ingrediente", "análise", "sensorial"),
        ),
        acquired(
            id = "knowledge.acquired.reparo",
            name = "Reparo",
            category = "Ofício",
            attribute = "INT",
            description = "Diagnóstico e restauração de mecanismos, equipamentos e estruturas danificadas.",
            effect = "Aplica-se a testes de manutenção e reparo quando houver ferramentas e materiais plausíveis.",
            keywords = listOf("manutenção", "máquinas", "equipamento", "ofício"),
        ),
        acquired(
            id = "knowledge.acquired.exomathis",
            name = "Exomathis",
            category = "Conhecimento cultural e religioso",
            attribute = "INT",
            description = "Conhecimento sobre doutrina, símbolos, ritos, história e práticas relacionadas a Exomathis.",
            effect = "Aplica-se a testes diretamente ligados à doutrina, símbolos, ritos, história e interpretação de práticas de Exomathis.",
            keywords = listOf("religião", "doutrina", "história", "símbolos"),
        ),
        arcane(
            id = "knowledge.arcane.runas",
            name = "Runas",
            category = "Estudo arcano",
            attribute = "POD",
            description = "Conhecimento teórico e prático de estruturas rúnicas e seus fenômenos.",
            effect = "Aplica-se à identificação, análise e interpretação de Runas quando a informação for acessível ao personagem.",
            keywords = listOf("runas", "arcano", "símbolos", "magia"),
        ),
        arcane(
            id = "knowledge.arcane.cinzas",
            name = "Cinzas",
            category = "Estudo arcano",
            attribute = "POD",
            description = "Conhecimento de Cinzas, aplicações tecnomágicas e fenômenos associados.",
            effect = "Aplica-se à análise de Cinzas e seus efeitos quando a informação for pública ou conhecida pelo personagem.",
            keywords = listOf("cinzas", "tecnomagia", "arcano", "fenômeno"),
        ),
        arcane(
            id = "knowledge.arcane.sangromancia",
            name = "Sangromancia",
            category = "Tradição arcana",
            attribute = "POD",
            description = "Estudo e prática de fenômenos arcanos relacionados ao sangue.",
            effect = "Serve como especialização para efeitos e testes que mencionem explicitamente Sangromancia.",
            keywords = listOf("sangue", "magia", "tradição", "sobrenatural"),
        ),
        arcane(
            id = "knowledge.arcane.planos",
            name = "Planos e Fenômenos Sobrenaturais",
            category = "Fenômenos sobrenaturais",
            attribute = "INT",
            description = "Estudo de manifestações, planos e ocorrências sobrenaturais acessíveis aos jogadores.",
            effect = "Aplica-se à investigação e interpretação de fenômenos sobrenaturais conhecidos pelo personagem.",
            keywords = listOf("planos", "fenômenos", "sobrenatural", "investigação"),
        ),
        technique(
            id = "knowledge.technique.laminas_gemeas",
            name = "Lâminas Gêmeas",
            category = "Técnica com armas",
            attribute = "AGI",
            description = "Técnica de batalha concedida por fontes que forneçam acesso explícito a Lâminas Gêmeas.",
            effect = "Segue as regras publicadas da técnica Lâminas Gêmeas. O Companion registra a aquisição sem substituir a fonte que a concedeu.",
            prerequisites = listOf("Acesso à técnica por Caminho, treino ou outra fonte válida"),
            keywords = listOf("arma", "duas lâminas", "combate", "técnica"),
        ),
        technique(
            id = "knowledge.technique.postura_defensiva",
            name = "Postura Defensiva",
            category = "Postura",
            attribute = "VIG",
            description = "Registro de postura ou treinamento defensivo quando concedido pelas regras ou por uma fonte narrativa válida.",
            effect = "A mecânica específica deve seguir a referência publicada da técnica selecionada.",
            keywords = listOf("defesa", "postura", "combate"),
        ),
    ) + ExpandedKnowledgeCatalog.entries

    private fun acquired(
        id: String,
        name: String,
        category: String,
        attribute: String,
        description: String,
        effect: String,
        prerequisites: List<String> = emptyList(),
        keywords: List<String> = emptyList(),
    ) = knowledge(id, CatalogKind.ACQUIRED_KNOWLEDGE, name, category, attribute, description, effect, prerequisites, keywords)

    private fun arcane(
        id: String,
        name: String,
        category: String,
        attribute: String,
        description: String,
        effect: String,
        prerequisites: List<String> = emptyList(),
        keywords: List<String> = emptyList(),
    ) = knowledge(id, CatalogKind.ARCANE_KNOWLEDGE, name, category, attribute, description, effect, prerequisites, keywords)

    private fun technique(
        id: String,
        name: String,
        category: String,
        attribute: String,
        description: String,
        effect: String,
        prerequisites: List<String> = emptyList(),
        keywords: List<String> = emptyList(),
        repeatable: Boolean = false,
    ) = knowledge(id, CatalogKind.BATTLE_TECHNIQUE, name, category, attribute, description, effect, prerequisites, keywords, repeatable)

    private fun knowledge(
        id: String,
        kind: CatalogKind,
        name: String,
        category: String,
        attribute: String,
        description: String,
        effect: String,
        prerequisites: List<String>,
        keywords: List<String>,
        repeatable: Boolean = false,
    ) = CatalogEntry(
        id = id,
        kind = kind,
        name = name,
        group = category,
        summary = description,
        relatedAttribute = attribute,
        initialValue = 1,
        prerequisites = prerequisites,
        mechanicalEffect = effect,
        source = "Catálogo oficial do Companion",
        ruleReference = "03 - Regras/Criação de Personagem/Criação de Personagem.md#4-conhecimentos-adquiridos",
        keywords = keywords,
        repeatable = repeatable,
        version = BuiltInCatalog.VERSION,
    )
}

fun CatalogEntry.toSpecialKnowledge(): SpecialKnowledge = SpecialKnowledge(
    name = name,
    attribute = relatedAttribute,
    value = initialValue ?: 0,
    catalogEntryId = id,
    catalogVersion = version,
    category = group,
    description = summary,
    prerequisites = prerequisites,
    mechanicalEffect = mechanicalEffect,
    source = source,
    ruleReference = ruleReference,
    keywords = keywords,
    repeatable = repeatable,
)

fun List<SpecialKnowledge>.canAddCatalogEntry(entry: CatalogEntry): Boolean =
    entry.repeatable || none { it.catalogEntryId == entry.id }
