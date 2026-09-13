package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SourceKind
import com.kinderman.sdo.domain.model.allCanonicalAbilitiesSafely
import com.kinderman.sdo.domain.model.withRemovedCanonicalAbility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaceSelectionTest {
    @Test fun catalogContainsCanonicalRacesAndRestrictsOrganicSubRaces() {
        assertEquals(18, RaceCatalog.races.size)
        assertTrue(RaceCatalog.race("Humanos") != null)
        assertTrue(RaceCatalog.race("Lúmens") != null)

        val humanSubRaces = RaceCatalog.subRacesFor(RaceCatalog.race("Humanos")!!).map { it.name }
        val golmSubRaces = RaceCatalog.subRacesFor(RaceCatalog.race("Golms")!!).map { it.name }
        assertTrue("Bestial — Contaminado" in humanSubRaces)
        assertTrue("Aumentado" in humanSubRaces)
        assertFalse("Bestial — Contaminado" in golmSubRaces)
        assertTrue("Aumentado" in golmSubRaces)
        assertTrue("Oráculo" in golmSubRaces)
        assertEquals(listOf("Elfos do Crepúsculo", "Aumentado", "Oráculo", "Bestial — Contaminado", "Bestial — Completo"),
            RaceCatalog.subRacesFor(RaceCatalog.race("Elfos")!!).map { it.name })
        assertEquals(listOf("Aumentado", "Oráculo"),
            RaceCatalog.subRacesFor(RaceCatalog.race("Kaltoch")!!).map { it.name })
        assertEquals("Kaltoch", RaceCatalog.race("Kaltoch — Aumentado")?.name)
        assertEquals("Aumentado", RaceCatalog.legacySubRace("Kaltoch — Aumentado"))
        assertEquals("Elfos", RaceCatalog.race("Elfos do Crepúsculo")?.name)
        assertEquals("Elfos do Crepúsculo", RaceCatalog.legacySubRace("Elfos do Crepúsculo"))
    }

    @Test fun selectingAndChangingRaceDoesNotStackBonusesOrPowers() {
        val human = RaceCatalog.race("Humanos")!!
        val elf = RaceCatalog.race("Elfos")!!
        val selectedHuman = Character().withRaceSelection(human, null, "FOR", human.powers, null)
        val selectedElf = selectedHuman.withRaceSelection(elf, null, "POD", elf.powers, null)

        assertEquals(0, selectedElf.attributes.first { it.acronym == "FOR" }.value)
        assertEquals(0, selectedElf.attributes.first { it.acronym == "POD" }.value)
        assertEquals(1, selectedElf.attributeTotal("POD"))
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
        assertEquals(0, selected.attributes.first { it.acronym == "AGI" }.value)
        assertEquals(1, selected.attributeTotal("AGI"))
        assertEquals(1, selected.powers.count { it.origin.startsWith("Raça — ") })
        assertEquals(1, selected.powers.count { it.origin.startsWith("Sub-raça — ") })
    }

    @Test fun racialPowersEnterCanonicalAbilitiesAndCannotBeRemoved() {
        val race = RaceCatalog.race("Humanos")!!
        val selected = Character().withRaceSelection(race, null, "FOR", race.powers, null)
        val racial = selected.allCanonicalAbilitiesSafely().filter { it.source?.kind == SourceKind.Race }

        assertEquals(2, racial.size)
        val failure = runCatching { selected.withRemovedCanonicalAbility(racial.first().id) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure?.message.orEmpty().contains("raciais"))
    }
}
