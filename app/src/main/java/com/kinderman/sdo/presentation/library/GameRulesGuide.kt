package com.kinderman.sdo.presentation.library

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
internal data class AtomicRulesDocument(
    val versao: Int = 2,
    val escopo: String = "",
    val fonte: String = "",
    val total_regras: Int = 0,
    val regras: List<AtomicRuleEntry> = emptyList(),
)

@Serializable
internal data class AtomicRuleEntry(
    val id: String,
    val caminho: String = "",
    val tipo: String = "regra",
    val texto: String = "",
    val formula: String? = null,
    val campos: Map<String, JsonElement> = emptyMap(),
    val tags: List<String> = emptyList(),
    val ver_tambem: List<String> = emptyList(),
    val origem: String = "",
)

internal data class RuleBlock(
    val heading: String = "",
    val paragraphs: List<String> = emptyList(),
    val bullets: List<String> = emptyList(),
)

internal data class GameRuleTopic(
    val id: String,
    val part: String,
    val chapter: String,
    val category: String,
    val title: String,
    val summary: String = "",
    val keywords: List<String> = emptyList(),
    val formulas: List<String> = emptyList(),
    val blocks: List<RuleBlock> = emptyList(),
    val scales: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
    val isQuickReference: Boolean = false,
) {
    fun effectiveKeywords(): List<String> =
        if (keywords.isNotEmpty()) keywords else GameRulesGuide.deriveKeywords(this)
}

internal object GameRulesGuide {
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cachedTopics: List<GameRuleTopic>? = null

    val categories: List<String> = listOf(
        "Todas",
        "Combate",
        "Testes",
        "Criação & Base",
        "Recuperação & Sanidade",
        "Inventário & Carga",
        "Itens & Ofício",
        "Poderes & Arcano",
        "Viagem",
    )

    private val domainToCategory: Map<String, String> = mapOf(
        "combate" to "Combate",
        "testes_e_dificuldades" to "Testes",
        "criacao_de_personagem" to "Criação & Base",
        "racas_e_sub_racas" to "Criação & Base",
        "bem_vindo" to "Criação & Base",
        "recuperacao_sanidade_e_condicoes" to "Recuperação & Sanidade",
        "efeitos_de_insanidade" to "Recuperação & Sanidade",
        "inventario_e_carga" to "Inventário & Carga",
        "regras_gerais_de_itens" to "Itens & Ofício",
        "armas" to "Itens & Ofício",
        "armaduras_e_acessorios" to "Itens & Ofício",
        "fabricacao_e_reparo" to "Itens & Ofício",
        "implantes_e_exocorpos" to "Itens & Ofício",
        "precos_e_economia" to "Itens & Ofício",
        "magia" to "Poderes & Arcano",
        "cinzas" to "Poderes & Arcano",
        "runas" to "Poderes & Arcano",
        "corrupcao_divina" to "Poderes & Arcano",
        "energia_onirica" to "Poderes & Arcano",
        "materia_onirica" to "Poderes & Arcano",
        "tamanho_viagem_e_montarias" to "Viagem",
    )

    private val prettyWords: Map<String, String> = mapOf(
        "acao" to "Ação",
        "acoes" to "Ações",
        "acessorio" to "Acessório",
        "acessorios" to "Acessórios",
        "agilidade" to "Agilidade",
        "armadura" to "Armadura",
        "armaduras" to "Armaduras",
        "basico" to "Básico",
        "basicos" to "Básicos",
        "calculo" to "Cálculo",
        "capacidade" to "Capacidade",
        "caracteristica" to "Característica",
        "caracteristicas" to "Características",
        "combate" to "Combate",
        "condicao" to "Condição",
        "condicoes" to "Condições",
        "conjuracao" to "Conjuração",
        "corrupcao" to "Corrupção",
        "criacao" to "Criação",
        "critico" to "Crítico",
        "criticos" to "Críticos",
        "definicao" to "Definição",
        "distancia" to "Distância",
        "duracao" to "Duração",
        "economia" to "Economia",
        "exaustao" to "Exaustão",
        "fabricacao" to "Fabricação",
        "funcao" to "Função",
        "heranca" to "Herança",
        "insanidade" to "Insanidade",
        "inventario" to "Inventário",
        "limiar" to "Limiar",
        "magica" to "Mágica",
        "magicas" to "Mágicas",
        "materia" to "Matéria",
        "mecanica" to "Mecânica",
        "modificacao" to "Modificação",
        "modificacoes" to "Modificações",
        "municao" to "Munição",
        "nivel" to "Nível",
        "niveis" to "Níveis",
        "oficio" to "Ofício",
        "onirica" to "Onírica",
        "onirico" to "Onírico",
        "orgao" to "Órgão",
        "orgaos" to "Órgãos",
        "padrao" to "Padrão",
        "pericia" to "Perícia",
        "preco" to "Preço",
        "precos" to "Preços",
        "progressao" to "Progressão",
        "protecao" to "Proteção",
        "protecoes" to "Proteções",
        "raca" to "Raça",
        "racas" to "Raças",
        "rapida" to "Rápida",
        "rapidas" to "Rápidas",
        "reacao" to "Reação",
        "reacoes" to "Reações",
        "recuperacao" to "Recuperação",
        "reducao" to "Redução",
        "referencia" to "Referência",
        "regiao" to "Região",
        "regioes" to "Regiões",
        "resistencia" to "Resistência",
        "sub" to "Sub",
        "tecnica" to "Técnica",
        "tecnicas" to "Técnicas",
        "tecnologia" to "Tecnologia",
        "tecnologias" to "Tecnologias",
        "uniao" to "União",
    )

    private val lowercaseConnectors = setOf("de", "da", "do", "das", "dos", "e", "em", "a", "o", "para", "por", "com", "sem")

    private val keywordMatchers: List<Pair<String, List<String>>> = listOf(
        "Combate" to listOf("combate", "iniciativa", "turno", "rodada", "encontro"),
        "Ataque" to listOf("ataque", "atacar", "acerto", "crítico", "critico", "flanquear", "mirar", "disparo"),
        "Ações" to listOf("ação", "acoes", "ações", "reação", "reações", "movimento"),
        "Defesa" to listOf("defesa", "esquiva", "bloqueio", "proteção", "protecao", "cobertura", "pg", "pl"),
        "Dano" to listOf("dano", "ferimento", "morte", "inconsciente", "redução de dano", "letal"),
        "Testes" to listOf("teste", "rolagem", "cd", "dificuldade", "vantagem", "desvantagem", "sucesso", "falha"),
        "Condições" to listOf("condição", "condicao", "condições", "abalado", "sangramento", "atordoado", "paralisado", "envenenado"),
        "Sanidade" to listOf("sanidade", "insanidade", "trauma", "descanso", "recuperação", "recuperacao", "exaustão"),
        "Armas" to listOf("arma", "armas", "empunhadura", "munição", "municao", "recarga", "alcance"),
        "Armaduras" to listOf("armadura", "armaduras", "escudo", "acessório", "acessorio", "limite de agilidade"),
        "Itens" to listOf("item", "itens", "durabilidade", "qualidade", "sucata", "gema", "tecnologia", "implante"),
        "Carga" to listOf("carga", "inventário", "inventario", "sobrecarga", "mochila", "recipiente"),
        "Ofício" to listOf("fabricação", "fabricacao", "reparo", "forja", "ofício", "oficio", "liga", "material"),
        "Magia" to listOf("magia", "arcano", "feitiço", "conjuração", "conjuracao"),
        "Runas" to listOf("runa", "runas", "inscrição", "inscricao"),
        "Cinzas" to listOf("cinza", "cinzas", "pureza", "dose"),
        "Onírico" to listOf("onírico", "onirico", "onírica", "onirica", "corrupção", "corrupcao"),
        "Criação" to listOf("criação", "criacao", "herança", "heranca", "raça", "raca", "atributo", "conhecimento", "caminho"),
        "Viagem" to listOf("viagem", "montaria", "transporte", "tamanho", "deslocamento"),
    )

    val featuredKeywords: List<String> = listOf("Todas") + keywordMatchers.map { it.first }

    fun deriveKeywords(topic: GameRuleTopic): List<String> {
        val haystack = buildString {
            append(topic.title).append(' ')
            append(topic.chapter).append(' ')
            append(topic.category).append(' ')
            append(topic.summary).append(' ')
            topic.formulas.forEach { append(it).append(' ') }
            topic.blocks.forEach { block ->
                append(block.heading).append(' ')
                block.paragraphs.forEach { append(it).append(' ') }
                block.bullets.forEach { append(it).append(' ') }
            }
            topic.scales.forEach { append(it).append(' ') }
        }.lowercase()

        val matched = linkedSetOf<String>()
        when (topic.category) {
            "Combate" -> matched += "Combate"
            "Testes" -> matched += "Testes"
            "Recuperação & Sanidade" -> matched += "Sanidade"
            "Inventário & Carga" -> matched += "Carga"
            "Viagem" -> matched += "Viagem"
            "Criação & Base" -> matched += "Criação"
        }
        keywordMatchers.forEach { (keyword, terms) ->
            if (terms.any { term -> haystack.contains(term) }) {
                matched += keyword
            }
        }
        if (matched.isEmpty() && topic.chapter.isNotBlank()) {
            matched += topic.chapter
        }
        return matched.take(5).toList()
    }

    fun load(context: Context): List<GameRuleTopic> {
        cachedTopics?.let { return it }
        return synchronized(this) {
            cachedTopics?.let { return it }
            val loaded = runCatching {
                context.assets.open("player_handbook_rules.json").bufferedReader().use { reader ->
                    parseAtomicRules(json.decodeFromString<AtomicRulesDocument>(reader.readText()))
                }
            }.getOrDefault(emptyList())
            cachedTopics = loaded
            loaded
        }
    }

    internal fun parseAtomicRules(doc: AtomicRulesDocument): List<GameRuleTopic> {
        if (doc.regras.isEmpty()) return emptyList()
        val grouped = linkedMapOf<String, MutableList<AtomicRuleEntry>>()
        doc.regras.forEach { entry ->
            val segments = entry.caminho.split('.').filter(String::isNotBlank)
            val key = when {
                segments.size >= 2 -> "${segments[0]}.${segments[1]}"
                entry.origem.isNotBlank() -> entry.origem
                else -> entry.id
            }
            grouped.getOrPut(key) { mutableListOf() }.add(entry)
        }
        return grouped.entries.map { (groupKey, entries) ->
            val segments = groupKey.split('.').filter(String::isNotBlank)
            val domainSlug = segments.firstOrNull().orEmpty()
            val topicSlug = segments.getOrNull(1) ?: domainSlug
            val category = domainToCategory[domainSlug] ?: formatSlug(domainSlug).ifBlank { "Regras" }
            val chapter = formatSlug(domainSlug)
            val title = formatSlug(topicSlug)

            val formulas = entries.mapNotNull { entry ->
                val f = entry.formula?.trim().orEmpty()
                when {
                    f.isNotBlank() -> if (entry.texto.isNotBlank() && !entry.texto.contains(f)) "${entry.texto} ($f)" else f
                    entry.tipo == "formula" && entry.texto.isNotBlank() -> entry.texto.trim()
                    else -> null
                }
            }.distinct()

            val ruleLines = entries
                .filter { it.tipo == "regra" && it.texto.isNotBlank() }
                .map { it.texto.trim() }
                .distinct()

            val quickRefLines = entries
                .filter { it.tipo == "referencia" && it.texto.isNotBlank() }
                .map { it.texto.trim() }
                .distinct()

            val tableLines = entries
                .filter { it.tipo == "tabela" }
                .mapNotNull(::formatTableEntry)
                .distinct()

            val exampleLines = entries
                .filter { it.tipo == "exemplo" && it.texto.isNotBlank() }
                .map { it.texto.trim() }
                .distinct()

            val blocks = buildList {
                if (ruleLines.isNotEmpty()) {
                    add(RuleBlock(heading = "Regras", bullets = ruleLines))
                }
                if (quickRefLines.isNotEmpty()) {
                    add(RuleBlock(heading = "Referência rápida", bullets = quickRefLines))
                }
            }

            val summary = ruleLines.firstOrNull()
                ?: formulas.firstOrNull()
                ?: quickRefLines.firstOrNull()
                ?: tableLines.firstOrNull().orEmpty()

            val isQuickReference = entries.all { it.tipo == "referencia" }

            val baseTopic = GameRuleTopic(
                id = groupKey,
                part = doc.fonte.ifBlank { "Livro do Jogador" },
                chapter = chapter,
                category = category,
                title = title,
                summary = summary,
                formulas = formulas,
                blocks = blocks,
                scales = tableLines,
                examples = exampleLines,
                isQuickReference = isQuickReference,
            )

            val derived = deriveKeywords(baseTopic)
            val explicitTags = entries.flatMap { it.tags }
                .filter { it.isNotBlank() }
                .map(::formatSlug)
                .distinct()
            val mergedKeywords = (derived + explicitTags).distinct().take(6)

            baseTopic.copy(keywords = mergedKeywords)
        }
    }

    private fun formatTableEntry(entry: AtomicRuleEntry): String? {
        if (entry.campos.isEmpty()) return entry.texto.trim().takeIf(String::isNotBlank)
        val entryLabel = elementText(entry.campos["entrada"])
        val rest = entry.campos.entries
            .filter { it.key != "entrada" }
            .mapNotNull { (k, v) ->
                val text = elementText(v)
                if (text.isBlank()) null
                else if (k == "valor_ou_descricao") text
                else "${formatSlug(k)}: $text"
            }
        return when {
            entryLabel.isNotBlank() && rest.isNotEmpty() -> "$entryLabel — ${rest.joinToString(" • ")}"
            entryLabel.isNotBlank() -> entryLabel
            rest.isNotEmpty() -> rest.joinToString(" • ")
            else -> entry.texto.trim().takeIf(String::isNotBlank)
        }
    }

    private fun elementText(element: JsonElement?): String = when (element) {
        null, is JsonNull -> ""
        is JsonPrimitive -> element.contentOrNull.orEmpty().trim()
        is JsonArray -> element.map(::elementText).filter(String::isNotBlank).joinToString(", ")
        is JsonObject -> element.entries.mapNotNull { (k, v) ->
            val txt = elementText(v)
            if (txt.isBlank()) null else "${formatSlug(k)}: $txt"
        }.joinToString(" • ")
    }

    private fun formatSlug(raw: String): String {
        val parts = raw.trim().split('_', '-').filter(String::isNotBlank)
        if (parts.isEmpty()) return ""
        return parts.mapIndexed { idx, token ->
            val lower = token.lowercase()
            prettyWords[lower]
                ?: if (idx > 0 && lower in lowercaseConnectors) lower
                else lower.replaceFirstChar { it.uppercase() }
        }.joinToString(" ")
    }
}
