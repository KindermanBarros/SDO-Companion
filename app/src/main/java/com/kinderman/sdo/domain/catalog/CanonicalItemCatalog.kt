package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectCondition
import com.kinderman.sdo.domain.model.ItemEffectType
import com.kinderman.sdo.domain.model.ItemPart
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.boolean
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
    val kind: String = "MODIFICATION",
    val category: String = "",
    val origin: String = "",
    val characterCreationVisible: Boolean = true,
    val maxCharges: Int = 0,
    val recharge: String = "",
    val activation: String = "PASSIVE",
    val installationKnowledge: String = "",
    val installationDifficulty: String = "",
    val removalDifficulty: String = "",
    val failureEnhancementDamage: Int = 0,
    val failureItemDamage: Int = 0,
) {
    fun supports(base: ItemPart): Boolean =
        (compatibleBaseGroups.isEmpty() && compatibleBaseIds.isEmpty()) ||
            base.group in compatibleBaseGroups || base.id in compatibleBaseIds
}

/** Loads the shipped JSON catalogs directly, keeping them as the runtime source of truth. */
internal object CanonicalItemCatalog {
    private val itemDefinitions: List<Pair<ItemPartKind, CatalogItemDefinition>> by lazy {
        listOf("items.json", "ammunition.json").flatMap { fileName ->
            document(fileName).getValue("entries").jsonArray
        }.map { element ->
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
    val modifications by lazy { components("modifications.json", expectedKind = "MODIFICATION") }
    val gems by lazy { components("gems.json", expectedKind = "GEM") }
    val technologies by lazy { components("technologies.json", expectedKind = "TECHNOLOGY") }
    val enhancements by lazy { gems + technologies }

    private fun parts(kind: ItemPartKind): List<ItemPart> =
        itemDefinitions.filter { it.first == kind }.map { it.second.part }

    private fun components(fileName: String, expectedKind: String): List<ItemComponentDefinition> =
        document(fileName, expectedKind).getValue("entries").jsonArray.map { element ->
            val entry = element.jsonObject
            val effect = entry.getValue("effect").jsonObject
            ItemComponentDefinition(
                part = entry.toItemPart(
                    group = when (expectedKind) { "GEM" -> "Gema"; "TECHNOLOGY" -> "Tecnologia"; else -> "Modificação" },
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
                tier = entry.optionalString("rarity").takeIf(String::isNotBlank)?.let(GemTier::valueOf),
                kind = expectedKind,
                category = entry.optionalString("category"),
                origin = entry.optionalString("origin"),
                characterCreationVisible = entry.optionalBoolean("characterCreationVisible", true),
                maxCharges = entry.optionalInt("maxCharges"),
                recharge = entry.optionalString("recharge"),
                activation = entry.optionalString("activation").ifBlank { "PASSIVE" },
                installationKnowledge = entry.optionalString("installationKnowledge"),
                installationDifficulty = entry.optionalString("installationDifficulty"),
                removalDifficulty = entry.optionalString("removalDifficulty"),
                failureEnhancementDamage = entry.optionalInt("failureEnhancementDamage"),
                failureItemDamage = entry.optionalInt("failureItemDamage"),
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
    backpackCapacity = optionalInt("backpackCapacity"),
    enhancementSlots = optionalInt("enhancementSlots"),
    materialTier = optionalString("rarity"),
    characterCreationVisible = optionalBoolean("characterCreationVisible", true),
)

private fun JsonObject.string(name: String): String = getValue(name).jsonPrimitive.content
private fun JsonObject.optionalString(name: String): String = get(name)?.jsonPrimitive?.content.orEmpty()
private fun JsonObject.int(name: String): Int = getValue(name).jsonPrimitive.int
private fun JsonObject.optionalInt(name: String): Int = get(name)?.jsonPrimitive?.int ?: 0
private fun JsonObject.optionalBoolean(name: String, default: Boolean): Boolean = get(name)?.jsonPrimitive?.boolean ?: default
private fun JsonObject.nullableInt(name: String): Int? = get(name)?.takeUnless { it is JsonNull }?.jsonPrimitive?.int
private fun JsonObject.stringSet(name: String): Set<String> =
    get(name)?.jsonArray?.map { it.jsonPrimitive.content }?.toSet().orEmpty()
