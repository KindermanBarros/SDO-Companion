package com.kinderman.sdo.presentation.library

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class PlayerHandbookDocument(
    val version: Int = 1,
    val source: String = "",
    val sections: List<GameRuleTopic> = emptyList(),
)

@Serializable
internal data class GameRuleTopic(
    val id: String,
    val part: String,
    val chapter: String,
    val category: String,
    val title: String,
    val paragraphs: List<String> = emptyList(),
    val bullets: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
)

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

    fun load(context: Context): List<GameRuleTopic> {
        cachedTopics?.let { return it }
        return synchronized(this) {
            cachedTopics?.let { return it }
            val loaded = runCatching {
                context.assets.open("player_handbook_rules.json").bufferedReader().use { reader ->
                    json.decodeFromString<PlayerHandbookDocument>(reader.readText()).sections
                }
            }.getOrDefault(emptyList())
            cachedTopics = loaded
            loaded
        }
    }
}
