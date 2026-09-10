package com.kinderman.sdo.data.repository

import com.kinderman.sdo.data.local.CampaignPayloadCodec
import com.kinderman.sdo.domain.model.CampaignContentKind
import com.kinderman.sdo.domain.model.CampaignDelivery
import com.kinderman.sdo.domain.model.CampaignLibraryEntry
import com.kinderman.sdo.domain.model.CampaignDeliveryState
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.SpecialKnowledge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryApplicationTest {
    @Test
    fun `the same delivery is applied exactly once across retries`() {
        val item = InventoryItem(id = "item-1", name = "Escudo", pg = 3, category = "Armadura")
        val entry = CampaignLibraryEntry(kind = CampaignContentKind.ITEM, name = item.name, itemSnapshot = item)
        val delivery = CampaignDelivery(
            id = "delivery-1", snapshotKind = CampaignContentKind.ITEM, snapshotName = item.name,
            snapshotPayload = CampaignPayloadCodec.encode(entry), state = CampaignDeliveryState.ACCEPTED,
            knowledgeMapping = "knowledge-1", knowledgeBonus = 2,
        )
        val initial = Character(
            learnedKnowledges = listOf(SpecialKnowledge(id = "knowledge-1", name = "Metalurgia", adjustment = 1)),
        )

        val first = applyDelivery(initial, delivery)
        val retried = applyDelivery(first, delivery)

        assertEquals(listOf(item), retried.inventory)
        assertEquals(3, retried.learnedKnowledges.single().adjustment)
        assertEquals(listOf(delivery.id), retried.appliedDeliveryIds)
        assertEquals(first, retried)
    }

    @Test
    fun `a removed knowledge mapping cannot silently create a duplicate`() {
        val delivery = CampaignDelivery(
            id = "delivery-2", snapshotKind = CampaignContentKind.NOTE,
            knowledgeMapping = "removed", knowledgeBonus = 1,
        )

        val failure = runCatching { applyDelivery(Character(), delivery) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }
}
