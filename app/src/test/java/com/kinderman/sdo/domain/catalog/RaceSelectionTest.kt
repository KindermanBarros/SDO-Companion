package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ResourceValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaceSelectionTest {
    @Test fun catalogContainsCanonicalRacesAndRestrictsOrganicSubRaces() {
        assertEquals(20, RaceCatalog.races.size)
        assertTrue(RaceCatalog.race("Humanos") != null)
        assertTrue(RaceCatalog.race("Lúmens") != null)

        val humanSubRaces = RaceCatalog.subRacesFor(RaceCatalog.race("Humanos")!!).map { it.name }
        val golmSubRaces = RaceCatalog.subRacesFor(RaceCatalog.race("Golms")!!).map { it.name }
        assertTrue("Bestial — Contaminado" in humanSubRaces)
        assertFalse("Bestial — Contaminado" in golmSubRaces)
        assertTrue("Oráculo" in golmSubRaces)
    }

    @Test fun selectingAndChangingRaceDoesNotStackBonusesOrPowers() {
        val human = RaceCatalog.race("Humanos")!!
        val elf = RaceCatalog.race("Elfos")!!
        val selectedHuman = Character().withRaceSelection(human, null, "FOR", human.powers, null)
        val selectedElf = selectedHuman.withRaceSelection(elf, null, "POD", elf.powers, null)

        assertEquals(0, selectedElf.attributes.first { it.acronym == "FOR" }.value)
        assertEquals(1, selectedElf.attributes.first { it.acronym == "POD" }.value)
        assertEquals(0, selectedElf.life.adjustment)
        assertEquals(1, selectedElf.sanity.adjustment)
        assertEquals(2, selectedElf.arcane.adjustment)
        assertEquals(1, selectedElf.energy.adjustment)
        assertEquals(2, selectedElf.powers.size)
        assertTrue(selectedElf.powers.all { it.origin == "Raça — Elfos" })
    }

    @Test fun subRaceReplacesOneBasePowerAndPreservesUnmanagedLegacyAdjustments() {
        val human = RaceCatalog.race("Humanos")!!
        val oracle = RaceCatalog.subRaces.first { it.name == "Oráculo" }
        val legacy = Character(
            race = "Humanos",
            life = ResourceValue(adjustment = 4),
            sanity = ResourceValue(adjustment = 4),
            arcane = ResourceValue(adjustment = 4),
            energy = ResourceValue(adjustment = 4),
        )

        val selected = legacy.withRaceSelection(
            human,
            oracle,
            "AGI",
            listOf(human.powers.last()),
            oracle.powers.last(),
        )

        assertEquals(listOf(5, 5, 5, 5), listOf(
            selected.life.adjustment,
            selected.sanity.adjustment,
            selected.arcane.adjustment,
            selected.energy.adjustment,
        ))
        assertEquals("AGI", selected.raceAttribute)
        assertEquals(1, selected.attributes.first { it.acronym == "AGI" }.value)
        assertEquals(1, selected.powers.count { it.origin.startsWith("Raça — ") })
        assertEquals(1, selected.powers.count { it.origin.startsWith("Sub-raça — ") })
    }
}
