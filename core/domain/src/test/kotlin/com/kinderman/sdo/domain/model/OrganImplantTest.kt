package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OrganImplantTest {
    @Test fun `restorative implant clears failures only when applied`() {
        val implant = InventoryItem(id = "implant", name = "Coração mecânico", category = "Implante", effect = "Substitui a função do órgão.")
        val character = Character(inventory = listOf(implant), bodyState = BodyState(organs = listOf(
            OrganState(organ = OrganSlot.HeartOrCore, failures = 3, state = BodyIntegrity.Damaged),
        )))
        val applied = character.withOrganImplant(0, implant.id)
        assertEquals(0, applied.canonicalBodyState().organs.single().failures)
        assertEquals(BodyIntegrity.Damaged, applied.canonicalBodyState().organs.single().state)
    }

    @Test fun `unlinking or relabelling an organ does not clear failures`() {
        val character = Character(bodyState = BodyState(organs = listOf(OrganState(failures = 2))))
        assertEquals(2, character.withOrganImplant(0, null).canonicalBodyState().organs.single().failures)
        assertEquals(2, character.withCanonicalBodyState(character.canonicalBodyState().copy(
            organs = listOf(character.canonicalBodyState().organs.single().copy(customName = "Outro")),
        )).canonicalBodyState().organs.single().failures)
    }
}
