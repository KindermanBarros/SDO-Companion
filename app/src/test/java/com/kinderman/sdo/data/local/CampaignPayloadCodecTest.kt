package com.kinderman.sdo.data.local

import com.kinderman.sdo.domain.model.AbilityCostType
import com.kinderman.sdo.domain.model.AbilityModifier
import com.kinderman.sdo.domain.model.AbilityModifierTarget
import com.kinderman.sdo.domain.model.CampaignContentKind
import com.kinderman.sdo.domain.model.CampaignLibraryEntry
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemBonus
import com.kinderman.sdo.domain.model.ItemBonusType
import com.kinderman.sdo.domain.model.Power
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CampaignPayloadCodecTest {
    @Test
    fun `item structure and notes survive the room and delivery payload round trip`() {
        val item = InventoryItem(
            id = "item-1", state = "E", name = "Armadura", load = 4, durability = "3/5",
            region = "Torso", effect = "Reduz dano", pg = 2, pl = 5, category = "Armadura",
            agilityLimit = 3, quality = "Raro",
            bonuses = listOf(ItemBonus(ItemBonusType.ATTRIBUTE, "FOR", 2)), quantity = 1,
        )
        val entry = CampaignLibraryEntry(
            kind = CampaignContentKind.ITEM,
            name = item.name,
            payload = "Notas do historiador",
            itemSnapshot = item,
        )

        val restored = entry.toRecord().toDomain()
        val deliveryDecoded = CampaignPayloadCodec.decode(entry.kind, CampaignPayloadCodec.encode(entry))

        assertEquals(item, restored.itemSnapshot)
        assertEquals("Notas do historiador", restored.payload)
        assertEquals(item, deliveryDecoded?.item)
        assertEquals("Notas do historiador", deliveryDecoded?.text)
    }

    @Test
    fun `power and condition retain their structured fields`() {
        val power = Power(
            id = "power-1", name = "Pulso", cost = "2 Energia", action = "Ação",
            range = "Curto", duration = "Cena", effect = "Empurra", category = "Cinética",
            costType = AbilityCostType.ENERGY, costValue = 2,
            modifiers = listOf(AbilityModifier(targetType = AbilityModifierTarget.ATTRIBUTE, targetId = "FOR", value = 1)),
        )
        val condition = ConditionEffect("condition-1", "Abalado", "2", "1 cena", "Ritual", "-2 em testes")

        val restoredPower = CampaignLibraryEntry(
            kind = CampaignContentKind.POWER, name = power.name, powerSnapshot = power,
        ).toRecord().toDomain().powerSnapshot
        val restoredCondition = CampaignLibraryEntry(
            kind = CampaignContentKind.CONDITION, name = condition.name, conditionSnapshot = condition,
        ).toRecord().toDomain().conditionSnapshot

        assertEquals(power, restoredPower)
        assertEquals(condition, restoredCondition)
    }

    @Test
    fun `legacy free text remains readable without fabricating structured content`() {
        val legacy = CampaignLibraryRecord(kind = CampaignContentKind.ITEM.name, payload = "texto antigo").toDomain()

        assertEquals("texto antigo", legacy.payload)
        assertNull(legacy.itemSnapshot)
    }
}
