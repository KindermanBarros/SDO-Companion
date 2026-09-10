package com.kinderman.sdo.data.local

import com.kinderman.sdo.domain.model.CampaignContentKind
import com.kinderman.sdo.domain.model.CampaignLibraryEntry
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.Power

data class DecodedCampaignPayload(
    val text: String,
    val item: InventoryItem? = null,
    val power: Power? = null,
    val condition: ConditionEffect? = null,
)

object CampaignPayloadCodec {
    private const val PREFIX = "SDO_STRUCTURED_V1:"
    private const val TEXT_SEPARATOR = "\u0016"
    private val converters = CharacterConverters()

    fun encode(entry: CampaignLibraryEntry): String {
        val encoded = when (entry.kind) {
            CampaignContentKind.ITEM -> entry.itemSnapshot?.let { converters.inventoryToString(listOf(it)) }
            CampaignContentKind.POWER -> entry.powerSnapshot?.let { converters.powersToString(listOf(it)) }
            CampaignContentKind.CONDITION -> entry.conditionSnapshot?.let { converters.conditionsToString(listOf(it)) }
            else -> null
        } ?: return entry.payload
        return "$PREFIX${entry.kind.name}:$encoded$TEXT_SEPARATOR${entry.payload}"
    }

    fun decode(kind: CampaignContentKind, payload: String): DecodedCampaignPayload? {
        val expectedPrefix = "$PREFIX${kind.name}:"
        if (!payload.startsWith(expectedPrefix)) return null
        val body = payload.removePrefix(expectedPrefix)
        val encoded = body.substringBefore(TEXT_SEPARATOR)
        val text = body.substringAfter(TEXT_SEPARATOR, "")
        return runCatching {
            when (kind) {
                CampaignContentKind.ITEM -> DecodedCampaignPayload(text, item = converters.stringToInventory(encoded).single())
                CampaignContentKind.POWER -> DecodedCampaignPayload(text, power = converters.stringToPowers(encoded).single())
                CampaignContentKind.CONDITION -> DecodedCampaignPayload(text, condition = converters.stringToConditions(encoded).single())
                else -> DecodedCampaignPayload(text)
            }
        }.getOrNull()
    }
}
