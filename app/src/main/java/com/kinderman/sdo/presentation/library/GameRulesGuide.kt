package com.kinderman.sdo.presentation.library

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class PlayerHandbookDocument(
    val version: Int = 2,
    val source: String = "",
    val sections: List<GameRuleTopic> = emptyList(),
)

@Serializable
internal data class RuleBlock(
    val heading: String = "",
    val paragraphs: List<String> = emptyList(),
    val bullets: List<String> = emptyList(),
)

@Serializable
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
        "Criação & Base",
        "Testes",
        "Combate",
        "Recuperação & Sanidade",
        "Inventário & Carga",
        "Itens & Ofício",
        "Poderes & Arcano",
        "Viagem",
    )

    private val keywordMatchers: List<Pair<String, List<String>>> = listOf(
        "Combate" to listOf("combate", "iniciativa", "turno", "rodada", "encontro"),
        "Ataque" to listOf("ataque", "atacar", "acerto", "crítico", "critico", "flanquear", "mirar", "disparo"),
        "Ações" to listOf("ação", "acoes", "ações", "ação padrão", "ação rápida", "reação", "reações", "movimento"),
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
                    json.decodeFromString<PlayerHandbookDocument>(reader.readText()).sections.map { topic ->
                        if (topic.keywords.isNotEmpty()) topic else topic.copy(keywords = deriveKeywords(topic))
                    }
                }
            }.getOrDefault(emptyList())
            cachedTopics = loaded
            loaded
        }
    }
}
