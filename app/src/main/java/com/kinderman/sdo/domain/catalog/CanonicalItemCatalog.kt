package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectCondition
import com.kinderman.sdo.domain.model.ItemEffectType
import com.kinderman.sdo.domain.model.ItemPart
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal enum class ItemPartKind {
    WEAPON_MATERIAL,
    ARMOR_MATERIAL,
    WEAPON_BASE,
    ARMOR_BASE,
    CATALOG_ITEM,
}

internal enum class GemTier { MINOR, MAJOR }

internal data class CatalogItemDefinition(
    val part: ItemPart,
    val source: String,
    val ruleReference: String,
)

internal data class ItemComponentDefinition(
    val part: ItemPart,
    val compatibleItemTypes: Set<String>,
    val compatibleBaseGroups: Set<String>,
    val compatibleBaseIds: Set<String>,
    val effect: ItemEffect,
    val tier: GemTier? = null,
) {
    fun supports(base: ItemPart): Boolean =
        (compatibleBaseGroups.isEmpty() && compatibleBaseIds.isEmpty()) ||
            base.group in compatibleBaseGroups || base.id in compatibleBaseIds
}

/** Loads the shipped JSON catalogs directly, keeping them as the runtime source of truth. */
internal object CanonicalItemCatalog {
    private val itemDefinitions: List<Pair<ItemPartKind, CatalogItemDefinition>> by lazy {
        document("items.json").getValue("entries").jsonArray.map { element ->
            val entry = element.jsonObject
            val kind = ItemPartKind.valueOf(entry.string("kind"))
            kind to CatalogItemDefinition(
                part = entry.toItemPart(),
                source = entry.optionalString("source"),
                ruleReference = entry.optionalString("ruleReference"),
            )
        }
    }

    val weaponMaterials by lazy { parts(ItemPartKind.WEAPON_MATERIAL) }
    val armorMaterials by lazy { parts(ItemPartKind.ARMOR_MATERIAL) }
    val weaponBases by lazy { parts(ItemPartKind.WEAPON_BASE) }
    val armorBases by lazy { parts(ItemPartKind.ARMOR_BASE) }
    val catalogItems by lazy {
        itemDefinitions.filter { it.first == ItemPartKind.CATALOG_ITEM }.map { it.second }
    }
    val modifications by lazy { components("modifications.json", expectedKind = "MODIFICATION", expectTier = false) }
    val gems by lazy { components("gems.json", expectedKind = "GEM", expectTier = true) }

    private fun parts(kind: ItemPartKind): List<ItemPart> =
        itemDefinitions.filter { it.first == kind }.map { it.second.part }

    private fun components(fileName: String, expectedKind: String, expectTier: Boolean): List<ItemComponentDefinition> =
        document(fileName, expectedKind).getValue("entries").jsonArray.map { element ->
            val entry = element.jsonObject
            val effect = entry.getValue("effect").jsonObject
            ItemComponentDefinition(
                part = entry.toItemPart(
                    group = if (expectTier) "Gema" else "Modificação",
                    description = effect.string("description"),
                ),
                compatibleItemTypes = entry.stringSet("compatibleItemTypes"),
                compatibleBaseGroups = entry.stringSet("compatibleBaseGroups"),
                compatibleBaseIds = entry.stringSet("compatibleBaseIds"),
                effect = ItemEffect(
                    id = entry.string("id"),
                    type = ItemEffectType.valueOf(effect.string("type")),
                    value = effect.int("value"),
                    target = effect.string("target"),
                    condition = ItemEffectCondition.valueOf(effect.string("condition")),
                    description = effect.string("description"),
                ),
                tier = entry.optionalString("tier").takeIf(String::isNotBlank)?.let(GemTier::valueOf),
            )
        }

    private fun document(fileName: String, expectedKind: String? = null): JsonObject {
        val stream = checkNotNull(CanonicalItemCatalog::class.java.classLoader?.getResourceAsStream(fileName)) {
            "Canonical catalog resource not found: $fileName"
        }
        return stream.bufferedReader().use { reader ->
            Json.parseToJsonElement(reader.readText()).jsonObject.also { document ->
                check(document.int("schemaVersion") == 1) { "$fileName uses an unsupported schema version" }
                check(document.int("catalogVersion") >= 1) { "$fileName has an invalid catalog version" }
                expectedKind?.let { check(document.string("kind") == it) { "$fileName has an unexpected catalog kind" } }
            }
        }
    }
}

private fun JsonObject.toItemPart(group: String? = null, description: String? = null) = ItemPart(
    id = string("id"),
    name = string("name"),
    group = group ?: string("group"),
    creationCost = nullableInt("creationCost"),
    price = optionalInt("price"),
    load = optionalInt("load"),
    durability = optionalInt("durability"),
    region = optionalString("region"),
    effect = description ?: optionalString("effect"),
    pg = optionalInt("pg"),
    pl = optionalInt("pl"),
    agilityLimit = nullableInt("agilityLimit"),
)

private fun JsonObject.string(name: String): String = getValue(name).jsonPrimitive.content
private fun JsonObject.optionalString(name: String): String = get(name)?.jsonPrimitive?.content.orEmpty()
private fun JsonObject.int(name: String): Int = getValue(name).jsonPrimitive.int
private fun JsonObject.optionalInt(name: String): Int = get(name)?.jsonPrimitive?.int ?: 0
private fun JsonObject.nullableInt(name: String): Int? = get(name)?.takeUnless { it is JsonNull }?.jsonPrimitive?.int
private fun JsonObject.stringSet(name: String): Set<String> =
    get(name)?.jsonArray?.map { it.jsonPrimitive.content }?.toSet().orEmpty()
