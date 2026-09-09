package com.kinderman.sdo.ui

val LocalSdoPreferences = androidx.compose.runtime.compositionLocalOf { SdoPreferences() }

enum class SdoThemeVariant(val label: String, val description: String) {
    CYAN_INDUSTRIAL("Ciano industrial", "Ciano, azul e magenta do HUD original."),
    GREEN_TERMINAL("Verde terminal", "Verde fosforescente sobre superfícies técnicas."),
    CRIMSON_ARCANE("Rubro arcano", "Vermelho ritual com sinais âmbar."),
    VIOLET_DREAM("Violeta Rúnico", "Violeta profundo, lavanda e ciano rúnico."),
    EDGERUNNERS("Edgerunners", "Amarelo elétrico, ciano e vermelho sobre azul noturno."),
    MAGENTA_DREAM("Magenta Onírico", "Rosa ácido e violeta sobre superfícies escuras."),
    APERTURE_WHITE("Aperture White", "Modo claro em branco e azul dos portais de Portal."),
    HIGH_CONTRAST("Alto contraste", "Superfícies mais escuras e sinais mais claros."),
    SYSTEM("Tema do sistema", "Segue a preferência de contraste claro/escuro do aparelho."),
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
    val showValueAudit: Boolean = false,
    val autoSync: Boolean = true,
    val notifications: Boolean = true,
    val compactCards: Boolean = false,
    val collapseLongSections: Boolean = true,
    val showArchivedCampaigns: Boolean = true,
)
