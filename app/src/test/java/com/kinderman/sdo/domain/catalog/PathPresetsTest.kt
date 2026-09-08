package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.Power
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathPresetsTest {
    @Test fun everyBuiltInPathHasACompletePreset() {
        val paths = BuiltInCatalog.entries.filter { it.kind == CatalogKind.PATH }

        assertEquals(paths.size, PathPresets.entries.size)
        paths.forEach { entry ->
            val preset = PathPresets.find(entry.id)
            assertTrue("Preset ausente: ${entry.id}", preset != null)
            assertTrue("Lema ausente: ${entry.id}", preset!!.motto.isNotBlank())
            assertEquals("Palavras-chave inválidas: ${entry.id}", 3, preset.keywords.size)
            assertEquals("Pilares inválidos: ${entry.id}", 3, preset.pillars.size)
            assertEquals("Poderes inválidos: ${entry.id}", 2, preset.powers.size)
            assertTrue(preset.keywords.all(String::isNotBlank))
            assertTrue(preset.pillars.all(String::isNotBlank))
            assertTrue(preset.powers.all { it.name.isNotBlank() && it.effect.isNotBlank() })
        }
    }

    @Test fun selectingAPathFillsEverythingAndDoesNotDuplicateManagedPowers() {
        val manualPower = Power(name = "Poder manual", origin = "Recompensa narrativa")
        val gears = BuiltInCatalog.entries.first { it.id == "path.engrenagens" }
        val ruler = BuiltInCatalog.entries.first { it.id == "path.governante" }
        val selected = Character(powers = listOf(manualPower)).withPathPreset(gears)
        val changed = selected.withPathPreset(ruler)
        val rulerPreset = PathPresets.find(ruler.id)!!

        assertEquals(ruler.name, changed.pathName)
        assertEquals(rulerPreset.motto, changed.pathMotto)
        assertEquals(rulerPreset.keywords, changed.pathKeywords)
        assertEquals(rulerPreset.pillars, changed.pathPillars)
        assertEquals(2, changed.powers.count { it.origin == "Caminho — ${ruler.name}" })
        assertEquals(0, changed.powers.count { it.origin == "Caminho — ${gears.name}" })
        assertTrue(manualPower in changed.powers)
    }
}
