package com.kinderman.sdo.data.sync

import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.PersonalNote
import com.kinderman.sdo.domain.model.ResourceValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetWireCodecTest {
    @Test fun recordRoundTripPreservesCanonicalPayloadAndLegacyFields() {
        val record = CharacterRecord(
            id = "sheet", ownerId = "owner", campaignId = "campaign", name = "Árvore",
            inventory = listOf(InventoryItem(id = "item", name = "Arma")),
            personalNotes = listOf(PersonalNote(id = "note", title = "Título", text = "Texto")),
            life = ResourceValue(4, 10, 2), photoUri = "data:image/jpeg;base64,aA==",
            canonicalItemsPayload = "[{\"id\":\"item\"}]",
            fieldUpdatedAt = mapOf("@revision" to 18, "@deleted/inventory/aA" to 17),
        )
        assertEquals(record, SheetWireCodec.decode(SheetWireCodec.json(SheetWireCodec.state(record))))
    }

    @Test fun missingRtdbEmptyCollectionsRestoreDefaults() {
        val restored = SheetWireCodec.decode("{\"id\":\"sheet\",\"ownerId\":\"owner\"}")
        assertEquals(emptyList<PersonalNote>(), restored.personalNotes)
        assertEquals(ResourceValue(), restored.life)
        assertEquals("sheet", restored.id)
    }

    @Test fun patchContainsOnlyChangedEditableFields() {
        val before = CharacterRecord(id = "sheet", ownerId = "owner")
        val changes = SheetWireCodec.changes(before, before.copy(name = "Edited", ownerId = "attacker", dirty = true, updatedAt = 99))
        assertEquals(mapOf("name" to "Edited"), changes)
    }

    @Test fun slashMetadataIsAValueNotADatabasePath() {
        val wire = SheetWireCodec.state(CharacterRecord(fieldUpdatedAt = mapOf("@operation/a/b" to 3)))
        assertTrue(wire["fieldUpdatedAt"] is String)
        assertFalse(wire.containsKey("dirty"))
        assertFalse(wire.containsKey("lastSyncedAt"))
    }
}
