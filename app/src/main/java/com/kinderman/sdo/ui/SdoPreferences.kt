package com.kinderman.sdo.ui

val LocalSdoPreferences = androidx.compose.runtime.compositionLocalOf { SdoPreferences() }

enum class SdoThemeVariant(
    val label: String,
    val description: String,
) {
    CYAN_INDUSTRIAL("Ciano Industrial", "Ciano, azul e magenta do HUD original."),
    GREEN_TERMINAL("Verde Terminal", "Verde fosforescente sobre superfícies técnicas."),
    CRIMSON_ARCANE("Rubro Arcano", "Vermelho ritual com sinais âmbar."),
    VIOLET_DREAM("Violeta Rúnico", "Violeta profundo, lavanda e ciano rúnico."),
    MAGENTA_DREAM("Magenta Onírico", "Rosa ácido e violeta sobre superfícies escuras."),
    EDGERUNNERS("Edgerunners", "Amarelo elétrico, ciano e vermelho sobre azul noturno."),
    APERTURE_WHITE("Aperture White", "Branco clínico e azul de portal, com sinalização laranja."),
    BLACK_LODGE_DREAM("Blue Rose Velvet", "Azul elétrico, violeta profundo e vermelho aveludado."),
    OLDEST_HOUSE_SIGNAL("Threshold Red", "Concreto abissal, ciano institucional e vermelho intenso."),
    HIGH_CONTRAST("Alto Contraste", "Superfícies mais escuras e sinais mais claros."),
    SYSTEM("Tema do Sistema", "Segue a preferência de contraste claro/escuro do aparelho."),
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
    val theme: SdoThemeVariant = SdoThemeVariant.CYAN_INDUSTRIAL,
    val density: SdoContentDensity = SdoContentDensity.COMFORTABLE,
    val fontScale: SdoFontScale = SdoFontScale.STANDARD,
    val autoSync: Boolean = true,
    val notifications: Boolean = true,
    val compactCards: Boolean = false,
    val collapseLongSections: Boolean = true,
    val showArchivedCampaigns: Boolean = true,
)
