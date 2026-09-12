package com.kinderman.sdo.presentation.character

import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ItemEffectPresentationTest {
    @Test fun `typed effect uses a readable label`() {
        assertEquals("Proteção geral +2", ItemEffect("pg", ItemEffectType.PG, 2).presentationLabel())
    }

    @Test fun `rule effect shows its description without leaking the enum`() {
        val label = ItemEffect("rule", ItemEffectType.RULE, description = "Ignora terreno difícil").presentationLabel()
        assertEquals("Ignora terreno difícil", label)
        assertFalse(label.contains("RULE"))
    }
}
