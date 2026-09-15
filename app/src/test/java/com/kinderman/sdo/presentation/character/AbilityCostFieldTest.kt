package com.kinderman.sdo.presentation.character

import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.AbilityCost
import com.kinderman.sdo.domain.model.AbilityKind
import com.kinderman.sdo.domain.model.Source
import com.kinderman.sdo.domain.model.NarrativeSourceId
import org.junit.Assert.assertEquals
import org.junit.Test

class AbilityCostFieldTest {
    @Test
    fun `spell and rune accept zero cost while ash keeps one dose minimum`() {
        val source = Source.narrative(NarrativeSourceId("test"))
        val spell = Ability(name = "Magia", kind = AbilityKind.SPELL, source = source, cost = AbilityCost.SpellCost())
        val rune = Ability(name = "Relógio Analógico", kind = AbilityKind.RUNE, source = source, cost = AbilityCost.RuneCost())
        val ash = Ability(name = "Cinza", kind = AbilityKind.ASH, cost = AbilityCost.AshCost())

        assertEquals(0, abilityCostWithAmount(spell, 0).amount)
        assertEquals(0, abilityCostWithAmount(rune, 0).amount)
        assertEquals(1, abilityCostWithAmount(ash, 0).amount)
    }
}
