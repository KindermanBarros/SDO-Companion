package com.kinderman.sdo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CompactControlLabelTest(private val mode: SdoVisualMode, private val theme: SdoThemeVariant) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun narrowActionLabelKeepsCompleteWordsWithoutTruncation() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode, theme = theme)) {
                Box(Modifier.width(56.dp)) { AdaptiveActionLabel("Selecionar raça") }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Selecionar raça").performSemanticsAction(
            SemanticsActions.GetTextLayoutResult,
        ) { it(layouts) }
        val layout = layouts.single()
        val text = "Selecionar raça"
        repeat(layout.lineCount) { line ->
            assertFalse(layout.isLineEllipsized(line))
            if (line < layout.lineCount - 1) {
                val end = layout.getLineEnd(line, visibleEnd = true)
                assertTrue(text.getOrNull(end)?.isWhitespace() == true || text.getOrNull(end - 1)?.isWhitespace() == true)
            }
        }
    }

    @Test
    fun metadataRetainsNeutralSeparatorsAndThemeAccents() {
        var neutral = Color.Unspecified
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode, theme = theme)) {
                neutral = MaterialTheme.colorScheme.onSurfaceVariant
                SdoMetadataText("Vida // Energia // Arcano")
            }
        }
        val text = compose.onNodeWithText("Vida // Energia // Arcano")
            .fetchSemanticsNode().config[SemanticsProperties.Text].single()
        assertEquals("Vida // Energia // Arcano", text.text)
        val separators = text.spanStyles.filter { text.text.substring(it.start, it.end) == "//" }
        assertEquals(2, separators.size)
        separators.forEach { assertEquals(neutral, it.item.color) }
        val chunks = text.spanStyles.filterNot { it in separators }
        assertNotEquals(chunks[0].item.color, chunks[1].item.color)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-{1}")
        fun modes(): List<Array<Any>> = SdoThemeVariant.entries.map {
            arrayOf<Any>(SdoVisualMode.STANDARD, it)
        } + listOf(arrayOf<Any>(SdoVisualMode.CYBERGRUNGE, SdoThemeVariant.CYAN_INDUSTRIAL))
    }
}
