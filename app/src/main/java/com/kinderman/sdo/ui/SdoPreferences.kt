package com.kinderman.sdo.ui

enum class SdoThemeVariant(val label: String, val description: String) {
    NEON("Neon operacional", "Ciano, azul e magenta do HUD original."),
    HIGH_CONTRAST("Alto contraste", "Superfícies mais escuras e sinais mais claros."),
    ARCANE("Arcano", "Violeta e ciano para sessões de foco místico."),
}

enum class SdoContentDensity(val label: String, val description: String) {
    COMFORTABLE("Confortável", "Mais espaço entre controles e painéis."),
    COMPACT("Compacta", "Mais informações visíveis durante a sessão."),
}

enum class SdoFontScale(val label: String, val multiplier: Float) {
    STANDARD("Padrão", 1f),
    LARGE("Texto ampliado", 1.16f),
}

data class SdoPreferences(
    val theme: SdoThemeVariant = SdoThemeVariant.NEON,
    val density: SdoContentDensity = SdoContentDensity.COMFORTABLE,
    val fontScale: SdoFontScale = SdoFontScale.STANDARD,
)
