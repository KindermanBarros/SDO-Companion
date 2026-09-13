package com.kinderman.sdo.data.local

import androidx.room.TypeConverter
import com.kinderman.sdo.domain.model.AttributeValue
import com.kinderman.sdo.domain.model.AbilityCostType
import com.kinderman.sdo.domain.model.AbilityDuration
import com.kinderman.sdo.domain.model.AbilityExecution
import com.kinderman.sdo.domain.model.AbilityModifier
import com.kinderman.sdo.domain.model.AbilityModifierTarget
import com.kinderman.sdo.domain.model.AbilityRange
import com.kinderman.sdo.domain.model.AbilityResistance
import com.kinderman.sdo.domain.model.AbilitySource
import com.kinderman.sdo.domain.model.AbilityTimeUnit
import com.kinderman.sdo.domain.model.AshPurity
import com.kinderman.sdo.domain.model.AshSource
import com.kinderman.sdo.domain.model.BodyRegion
import com.kinderman.sdo.domain.model.BodyIntegrity
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemAcquisitionSource
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectCondition
import com.kinderman.sdo.domain.model.ItemEffectType
import com.kinderman.sdo.domain.model.ItemCreationDraft
import com.kinderman.sdo.domain.model.ItemCondition
import com.kinderman.sdo.domain.model.ItemQuality
import com.kinderman.sdo.domain.model.KnowledgeMilestoneReward
import com.kinderman.sdo.domain.model.KnowledgeMilestoneRewardType
import com.kinderman.sdo.domain.model.MysticAbility
import com.kinderman.sdo.domain.model.OrganStatus
import com.kinderman.sdo.domain.model.OrganSlot
import com.kinderman.sdo.domain.model.PersonalNote
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.ProgressionRecord
import com.kinderman.sdo.domain.model.ProgressionReward
import com.kinderman.sdo.domain.model.ProgressionRewardType
import com.kinderman.sdo.domain.model.PowerSourceType
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SkillValue
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.defaultAttributes
import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.ActiveModifier
import com.kinderman.sdo.domain.model.BodyState
import com.kinderman.sdo.domain.model.ConditionInstance
import com.kinderman.sdo.domain.model.ItemState
import com.kinderman.sdo.domain.model.ScopedItemDefinition
import com.kinderman.sdo.domain.model.AbilityCatalogId
import com.kinderman.sdo.domain.model.AuditableChoice
import com.kinderman.sdo.domain.model.CatalogEntryId
import com.kinderman.sdo.domain.model.CatalogReference
import com.kinderman.sdo.domain.model.CharacterProgression
import com.kinderman.sdo.domain.model.ChoiceKind
import com.kinderman.sdo.domain.model.EntityId
import com.kinderman.sdo.domain.model.ItemCatalogId
import com.kinderman.sdo.domain.model.SourceRef
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val ROW = "\u001e"
private const val FIELD = "\u001f"
private const val BONUS_ROW = "\u001b"
private const val BONUS_FIELD = "\u001a"
private const val NESTED = "\u0019"
private const val MODIFIER_ROW = "\u0018"
private const val MODIFIER_FIELD = "\u0017"
private const val MILESTONE_ROW = "\u0016"
private const val MILESTONE_FIELD = "\u0015"
private fun String.parts() = split(FIELD)
private fun List<String>.row() = joinToString(FIELD)
private fun List<String>.nested() = joinToString(NESTED)
private fun String.toNestedList() = if (isBlank()) emptyList() else split(NESTED)

class CharacterConverters {
    private val canonicalJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @TypeConverter fun canonicalAbilitiesToString(value: List<Ability>): String =
        canonicalJson.encodeToString(ListSerializer(Ability.serializer()), value)
    @TypeConverter fun stringToCanonicalAbilities(value: String): List<Ability> = value.takeIf(String::isNotBlank)
        ?.let { runCatching { canonicalJson.decodeFromString(ListSerializer(Ability.serializer()), it) }.getOrDefault(emptyList<Ability>()) }.orEmpty()
    fun isCanonicalAbilitiesPayloadValid(value: String) = value.isBlank() ||
        runCatching { canonicalJson.decodeFromString(ListSerializer(Ability.serializer()), value) }.isSuccess

    @TypeConverter fun canonicalBodyToString(value: BodyState?): String? =
        value?.let { canonicalJson.encodeToString(BodyState.serializer(), it) }
    @TypeConverter fun stringToCanonicalBody(value: String?): BodyState? = value?.takeIf(String::isNotBlank)
        ?.let { runCatching { canonicalJson.decodeFromString(BodyState.serializer(), it) }.getOrNull() }
    fun isCanonicalBodyPayloadValid(value: String) = value.isBlank() ||
        runCatching { canonicalJson.decodeFromString(BodyState.serializer(), value) }.isSuccess

    @TypeConverter fun canonicalConditionsToString(value: List<ConditionInstance>): String =
        canonicalJson.encodeToString(ListSerializer(ConditionInstance.serializer()), value)
    @TypeConverter fun stringToCanonicalConditions(value: String): List<ConditionInstance> = value.takeIf(String::isNotBlank)
        ?.let { runCatching { canonicalJson.decodeFromString(ListSerializer(ConditionInstance.serializer()), it) }.getOrDefault(emptyList<ConditionInstance>()) }.orEmpty()
    fun isCanonicalConditionsPayloadValid(value: String) = value.isBlank() ||
        runCatching { canonicalJson.decodeFromString(ListSerializer(ConditionInstance.serializer()), value) }.isSuccess

    @TypeConverter fun activeModifiersToString(value: List<ActiveModifier>): String =
        canonicalJson.encodeToString(ListSerializer(ActiveModifier.serializer()), value)
    @TypeConverter fun stringToActiveModifiers(value: String): List<ActiveModifier> = value.takeIf(String::isNotBlank)
        ?.let { runCatching { canonicalJson.decodeFromString(ListSerializer(ActiveModifier.serializer()), it) }.getOrDefault(emptyList<ActiveModifier>()) }.orEmpty()
    fun isActiveModifiersPayloadValid(value: String) = value.isBlank() ||
        runCatching { canonicalJson.decodeFromString(ListSerializer(ActiveModifier.serializer()), value) }.isSuccess

    fun canonicalItemsToString(value: List<ItemState>): String =
        canonicalJson.encodeToString(ListSerializer(ItemState.serializer()), value)
    fun stringToCanonicalItems(value: String): List<ItemState> = value.takeIf(String::isNotBlank)
        ?.let { runCatching { canonicalJson.decodeFromString(ListSerializer(ItemState.serializer()), it) }.getOrDefault(emptyList()) }.orEmpty()
    fun isCanonicalItemsPayloadValid(value: String): Boolean = value.isBlank() ||
        runCatching { canonicalJson.decodeFromString(ListSerializer(ItemState.serializer()), value) }.isSuccess

    fun scopedItemCatalogToString(value: List<ScopedItemDefinition>): String =
        canonicalJson.encodeToString(ListSerializer(ScopedItemDefinition.serializer()), value)
    fun stringToScopedItemCatalog(value: String): List<ScopedItemDefinition> = value.takeIf(String::isNotBlank)
        ?.let { runCatching { canonicalJson.decodeFromString(ListSerializer(ScopedItemDefinition.serializer()), it) }.getOrDefault(emptyList()) }.orEmpty()
    fun isScopedItemCatalogPayloadValid(value: String): Boolean = value.isBlank() ||
        runCatching { canonicalJson.decodeFromString(ListSerializer(ScopedItemDefinition.serializer()), value) }.isSuccess

    @Serializable
    private data class ChoicePayload(
        val id: String,
        val kind: String,
        val optionType: String,
        val optionId: String,
        val optionRevision: Int,
        val source: SourceRef,
        val grantedEntityIds: List<String>,
        val chosenAt: Long,
    )

    fun progressionToString(value: CharacterProgression): String = canonicalJson.encodeToString(
        ListSerializer(ChoicePayload.serializer()),
        value.choices.map { choice ->
            val (type, id) = when (val optionId = choice.option.id) {
                is AbilityCatalogId -> "ability" to optionId.value
                is ItemCatalogId -> "item" to optionId.value
                is CatalogEntryId -> "catalog" to optionId.value
                is EntityId -> "entity" to optionId.value
                else -> "entity" to optionId.toString()
            }
            ChoicePayload(choice.id.value, choice.kind.name, type, id, choice.option.revision, choice.source,
                choice.grantedEntityIds.map(EntityId::value), choice.chosenAt)
        },
    )

    fun stringToCanonicalProgression(value: String): CharacterProgression = value.takeIf(String::isNotBlank)?.let { payload ->
        runCatching {
            CharacterProgression(canonicalJson.decodeFromString(ListSerializer(ChoicePayload.serializer()), payload).map { choice ->
                val option = when (choice.optionType) {
                    "ability" -> CatalogReference(AbilityCatalogId(choice.optionId), choice.optionRevision)
                    "item" -> CatalogReference(ItemCatalogId(choice.optionId), choice.optionRevision)
                    "catalog" -> CatalogReference(CatalogEntryId(choice.optionId), choice.optionRevision)
                    else -> CatalogReference(EntityId(choice.optionId), choice.optionRevision)
                }
                AuditableChoice(EntityId(choice.id), ChoiceKind.valueOf(choice.kind), option, choice.source,
                    choice.grantedEntityIds.map(::EntityId), choice.chosenAt)
            })
        }.getOrDefault(CharacterProgression())
    } ?: CharacterProgression()

    fun isProgressionPayloadValid(value: String): Boolean = value.isBlank() || runCatching {
        canonicalJson.decodeFromString(ListSerializer(ChoicePayload.serializer()), value)
    }.isSuccess

    @TypeConverter fun itemCreationDraftToString(value: ItemCreationDraft?): String? = value?.let {
        listOf(
            it.step.toString(), it.category, it.baseId, it.materialId, it.quality.name,
            it.modificationIds.nested(), it.gemIds.nested(), it.gemSlots.toString(),
            it.technologySlots.toString(), it.customName, it.manualPrice, it.commonName,
            it.commonEffect, it.commonLoad.toString(), it.commonQuantity.toString(),
            it.commonCategory, it.commonRegion, it.commonDurability.toString(), it.commonPg.toString(),
            it.commonPl.toString(), it.commonAgilityLimit?.toString().orEmpty(), it.commonAttack.toString(),
            it.commonDamage.toString(), it.commonRange.toString(), it.technologyIds.nested(),
        ).row()
    }

    @TypeConverter fun stringToItemCreationDraft(value: String?): ItemCreationDraft? =
        value?.takeIf(String::isNotBlank)?.parts()?.let { fields ->
            ItemCreationDraft(
                step = fields.getOrNull(0)?.toIntOrNull()?.coerceIn(1, 7) ?: 1,
                category = fields.getOrElse(1) { "Arma" },
                baseId = fields.getOrElse(2) { "" },
                materialId = fields.getOrElse(3) { "" },
                quality = runCatching { ItemQuality.valueOf(fields.getOrElse(4) { "" }) }.getOrDefault(ItemQuality.COMMON),
                modificationIds = fields.getOrElse(5) { "" }.toNestedList(),
                gemIds = fields.getOrElse(6) { "" }.toNestedList(),
                gemSlots = fields.getOrNull(7)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                technologySlots = fields.getOrNull(8)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                customName = fields.getOrElse(9) { "" },
                manualPrice = fields.getOrElse(10) { "" },
                commonName = fields.getOrElse(11) { "" },
                commonEffect = fields.getOrElse(12) { "" },
                commonLoad = fields.getOrNull(13)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                commonQuantity = fields.getOrNull(14)?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                commonCategory = fields.getOrElse(15) { "Item" },
                commonRegion = fields.getOrElse(16) { "" },
                commonDurability = fields.getOrNull(17)?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                commonPg = fields.getOrNull(18)?.toIntOrNull() ?: 0,
                commonPl = fields.getOrNull(19)?.toIntOrNull() ?: 0,
                commonAgilityLimit = fields.getOrNull(20)?.toIntOrNull(),
                commonAttack = fields.getOrNull(21)?.toIntOrNull() ?: 0,
                commonDamage = fields.getOrNull(22)?.toIntOrNull() ?: 0,
                commonRange = fields.getOrNull(23)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                technologyIds = fields.getOrElse(24) { "" }.toNestedList(),
            )
        }

    @TypeConverter fun progressionToString(value: List<ProgressionRecord>) = value.joinToString(ROW) { record ->
        listOf(record.id, record.previousLevel.toString(), record.newLevel.toString(), record.appliedAt.toString(),
            record.rewards.joinToString(BONUS_ROW) { reward ->
                listOf(reward.level.toString(), reward.type.name, reward.targetId, reward.catalogEntryId, reward.canonical.toString()).joinToString(BONUS_FIELD)
            }).row()
    }

    @TypeConverter fun stringToProgression(value: String): List<ProgressionRecord> = if (value.isEmpty()) emptyList() else value.split(ROW).map { encoded ->
        val fields = encoded.parts()
        ProgressionRecord(
            id = fields.getOrElse(0) { "" }, previousLevel = fields.getOrNull(1)?.toIntOrNull() ?: 1,
            newLevel = fields.getOrNull(2)?.toIntOrNull() ?: 1, appliedAt = fields.getOrNull(3)?.toLongOrNull() ?: 0,
            rewards = fields.getOrElse(4) { "" }.split(BONUS_ROW).filter(String::isNotBlank).map { reward ->
                val parts = reward.split(BONUS_FIELD)
                ProgressionReward(parts.getOrNull(0)?.toIntOrNull() ?: 1,
                    runCatching { ProgressionRewardType.valueOf(parts.getOrElse(1) { "" }) }.getOrDefault(ProgressionRewardType.RESOURCE),
                    parts.getOrElse(2) { "" }, parts.getOrElse(3) { "" }, parts.getOrNull(4)?.toBooleanStrictOrNull() ?: true)
            },
        )
    }
    @TypeConverter fun resourceToString(value: ResourceValue) =
        "${value.current}|${value.maximum}|${value.adjustment}"

    @TypeConverter fun stringToResource(value: String) = value.split('|').let {
        ResourceValue(
            current = it.getOrNull(0)?.toIntOrNull() ?: 0,
            maximum = it.getOrNull(1)?.toIntOrNull() ?: 0,
            adjustment = it.getOrNull(2)?.toIntOrNull() ?: 0,
        )
    }

    @TypeConverter fun stringsToString(value: List<String>) = value.joinToString(FIELD)
    @TypeConverter fun stringToStrings(value: String) = if (value.isEmpty()) emptyList() else value.split(FIELD)

    @TypeConverter fun mapToString(value: Map<String, Int>) = value.entries.joinToString(ROW) { listOf(it.key, it.value.toString()).row() }
    @TypeConverter fun stringToMap(value: String) = if (value.isEmpty()) emptyMap() else value.split(ROW).associate { item -> item.parts().let { it[0] to (it.getOrNull(1)?.toIntOrNull() ?: 0) } }

    @TypeConverter fun attributesToString(value: List<AttributeValue>) = value.joinToString("\u001d") { attribute ->
        listOf(attribute.name, attribute.acronym, attribute.value.toString(), attribute.modifier.toString(), attribute.skills.joinToString(ROW) { listOf(it.name, it.value.toString(), it.modifier.toString()).row() }).joinToString("\u001c")
    }
    @TypeConverter fun stringToAttributes(value: String) = if (value.isEmpty()) defaultAttributes() else value.split("\u001d").map { row ->
        row.split("\u001c").let { fields -> AttributeValue(fields[0], fields.getOrElse(1) { "" }, fields.getOrNull(2)?.toIntOrNull() ?: 0, fields.getOrNull(3)?.toIntOrNull() ?: 0, fields.getOrNull(4).orEmpty().split(ROW).filter(String::isNotEmpty).map { skill -> skill.parts().let { SkillValue(it[0], it.getOrNull(1)?.toIntOrNull() ?: 0, it.getOrNull(2)?.toIntOrNull() ?: 0) } }) }
    }

    @TypeConverter fun powersToString(value: List<Power>) = value.joinToString(ROW) {
        listOf(
            it.id,
            it.name,
            it.origin,
            it.cost,
            it.action,
            it.range,
            it.duration,
            it.limit,
            it.effect,
            it.category,
            it.prerequisites.nested(),
            it.activationCondition,
            it.enhancements,
            it.deactivationCondition,
            it.ruleReference,
            it.sourceType.name,
            it.sourceId,
            it.catalogEntryId,
            it.catalogVersion.toString(),
            it.favorite.toString(),
            it.available.toString(),
            "canonical-v2",
            it.canonicalSource.name,
            it.knowledgeId,
            it.knowledgeLevel?.toString().orEmpty(),
            it.costType.name,
            it.costValue.toString(),
            it.executionType.name,
            it.rangeType.name,
            it.targetArea,
            it.durationType.name,
            it.resistance.name,
            it.timeValue.toString(),
            it.timeUnit.name,
            it.grantsPermanentBonus.toString(),
            it.modifiers.joinToString(MODIFIER_ROW) { modifier ->
                listOf(modifier.id, modifier.targetType.name, modifier.targetId, modifier.value.toString()).joinToString(MODIFIER_FIELD)
            },
            it.active.toString(),
            it.linkedItemId,
            it.revision.toString(),
            it.durationValue.toString(),
            it.durationUnit.name,
        ).row()
    }

    @TypeConverter fun stringToPowers(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { row ->
        row.parts().let { p ->
            if (p.size >= 9) {
                Power(
                    id = p[0],
                    name = p[1],
                    origin = p[2],
                    cost = p[3],
                    action = p[4],
                    range = p[5],
                    duration = p[6],
                    limit = p[7],
                    effect = p[8],
                    category = p.getOrElse(9) { "" },
                    prerequisites = p.getOrElse(10) { "" }.toNestedList(),
                    activationCondition = p.getOrElse(11) { "" },
                    enhancements = p.getOrElse(12) { "" },
                    deactivationCondition = p.getOrElse(13) { "" },
                    ruleReference = p.getOrElse(14) { "" },
                    sourceType = runCatching { PowerSourceType.valueOf(p.getOrElse(15) { PowerSourceType.MANUAL.name }) }
                        .getOrDefault(inferLegacyPowerSource(p.getOrElse(2) { "" })),
                    sourceId = p.getOrElse(16) { "" },
                    catalogEntryId = p.getOrElse(17) { "" },
                    catalogVersion = p.getOrNull(18)?.toIntOrNull() ?: 0,
                    favorite = p.getOrNull(19)?.toBooleanStrictOrNull() ?: false,
                    available = p.getOrNull(20)?.toBooleanStrictOrNull() ?: true,
                    canonicalSource = p.enumAt(22, AbilitySource.NARRATIVE),
                    knowledgeId = p.getOrElse(23) { "" }.takeIf { p.getOrNull(21)?.startsWith("canonical-") == true }.orEmpty(),
                    knowledgeLevel = p.getOrNull(24)?.toIntOrNull().takeIf { p.getOrNull(21)?.startsWith("canonical-") == true },
                    costType = p.enumAt(25, legacyCostType(p.getOrElse(3) { "" })),
                    costValue = p.getOrNull(26)?.toIntOrNull().takeIf { p.getOrNull(21)?.startsWith("canonical-") == true } ?: legacyCostValue(p.getOrElse(3) { "" }),
                    executionType = p.enumAt(27, legacyExecution(p.getOrElse(4) { "" })),
                    rangeType = p.enumAt(28, legacyRange(p.getOrElse(5) { "" })),
                    targetArea = p.getOrElse(29) { "" }.takeIf { p.getOrNull(21)?.startsWith("canonical-") == true }.orEmpty(),
                    durationType = p.enumAt(30, legacyDuration(p.getOrElse(6) { "" })),
                    durationValue = p.getOrNull(39)?.toIntOrNull().takeIf { p.getOrNull(21) == "canonical-v2" } ?: 0,
                    durationUnit = p.enumAt(40, AbilityTimeUnit.HOURS),
                    resistance = p.enumAt(31, AbilityResistance.NONE),
                    timeValue = p.getOrNull(32)?.toIntOrNull().takeIf { p.getOrNull(21)?.startsWith("canonical-") == true } ?: 0,
                    timeUnit = p.enumAt(33, AbilityTimeUnit.MINUTES),
                    grantsPermanentBonus = p.getOrNull(34)?.toBooleanStrictOrNull().takeIf { p.getOrNull(21)?.startsWith("canonical-") == true } ?: false,
                    modifiers = p.getOrElse(35) { "" }.takeIf { p.getOrNull(21)?.startsWith("canonical-") == true }.toModifiers(),
                    active = p.getOrNull(36)?.toBooleanStrictOrNull().takeIf { p.getOrNull(21)?.startsWith("canonical-") == true } ?: false,
                    linkedItemId = p.getOrElse(37) { "" }.takeIf { p.getOrNull(21)?.startsWith("canonical-") == true }.orEmpty(),
                    revision = p.getOrNull(38)?.toIntOrNull().takeIf { p.getOrNull(21)?.startsWith("canonical-") == true } ?: 1,
                )
            } else {
                Power(
                    name = p.getOrElse(0) { "" },
                    origin = p.getOrElse(1) { "" },
                    cost = p.getOrElse(2) { "" },
                    action = p.getOrElse(3) { "" },
                    range = p.getOrElse(4) { "" },
                    duration = p.getOrElse(5) { "" },
                    limit = p.getOrElse(6) { "" },
                    effect = p.getOrElse(7) { "" },
                    sourceType = inferLegacyPowerSource(p.getOrElse(1) { "" }),
                )
            }
        }
    }

    @TypeConverter fun inventoryToString(value: List<InventoryItem>) = value.joinToString(ROW) { item ->
        listOf(
            item.id, item.state, item.name, item.load.toString(), item.durabilityCurrent.toString(), item.region,
            item.effect, item.pg.toString(), item.pl.toString(), item.category,
            item.agilityLimit?.toString().orEmpty(), item.quality.name,
            "", // reserved legacy slot; ItemBonus is no longer part of the domain model
            "canonical-v6", item.quantity.toString(), item.linkedAshId, item.ashPurity.name,
            item.acquisitionSource.name, item.heritageCost?.toString().orEmpty(), item.purchasePrice?.toString().orEmpty(),
            item.catalogEntryId, item.catalogVersion.toString(), item.acquiredAt.toString(), item.canonical.toString(),
            item.baseId, item.materialId, item.modificationIds.nested(), item.gemIds.nested(),
            item.mechanicalEffects.joinToString(MODIFIER_ROW) { effect ->
                listOf(effect.id, effect.type.name, effect.value.toString(), effect.target, effect.condition.name, effect.description, effect.resolvedTargetId).joinToString(MODIFIER_FIELD)
            },
            item.dataVersion.toString(), item.durabilityMax.toString(), item.itemCondition.name,
            item.backpackCapacity.toString(), item.technologyIds.nested(), item.gemSlots.toString(), item.technologySlots.toString(),
        ).row()
    }

    @TypeConverter fun stringToInventory(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { row ->
        row.parts().let { p ->
            InventoryItem(
                id = p[0], state = p.getOrElse(1) { "M" }, name = p.getOrElse(2) { "" },
                load = p.getOrNull(3)?.toIntOrNull() ?: 0,
                durabilityCurrent = if (p.getOrNull(13)?.startsWith("canonical-") == true) p.getOrNull(4)?.toIntOrNull() ?: 0 else legacyDurability(p.getOrElse(4) { "" }).first,
                durabilityMax = if (p.getOrNull(13)?.startsWith("canonical-") == true) p.getOrNull(30)?.toIntOrNull() ?: 0 else legacyDurability(p.getOrElse(4) { "" }).second,
                region = p.getOrElse(5) { "" }, effect = p.getOrElse(6) { "" },
                pg = p.getOrNull(7)?.toIntOrNull() ?: 0, pl = p.getOrNull(8)?.toIntOrNull() ?: 0,
                category = p.getOrElse(9) { "" }, agilityLimit = p.getOrNull(10)?.toIntOrNull(),
                quality = runCatching { ItemQuality.valueOf(p.getOrElse(11) { "COMMON" }) }
                    .getOrElse { ItemQuality.entries.firstOrNull { it.label.equals(p.getOrElse(11) { "" }, true) } ?: ItemQuality.COMMON },
                quantity = p.getOrNull(14)?.toIntOrNull().takeIf { p.getOrNull(13)?.startsWith("canonical-") == true } ?: 0,
                linkedAshId = p.getOrElse(15) { "" }.takeIf { p.getOrNull(13)?.startsWith("canonical-") == true }.orEmpty(),
                ashPurity = p.enumAt(16, AshPurity.RAW),
                acquisitionSource = p.enumAt(17, ItemAcquisitionSource.NARRATIVE),
                heritageCost = p.getOrNull(18)?.toIntOrNull().takeIf { p.getOrNull(13)?.startsWith("canonical-") == true },
                purchasePrice = p.getOrNull(19)?.toIntOrNull().takeIf { p.getOrNull(13)?.startsWith("canonical-") == true },
                catalogEntryId = p.getOrElse(20) { "" }.takeIf { p.getOrNull(13)?.startsWith("canonical-") == true }.orEmpty(),
                catalogVersion = p.getOrNull(21)?.toIntOrNull().takeIf { p.getOrNull(13)?.startsWith("canonical-") == true } ?: 0,
                acquiredAt = p.getOrNull(22)?.toLongOrNull().takeIf { p.getOrNull(13)?.startsWith("canonical-") == true } ?: 0,
                canonical = p.getOrNull(23)?.toBooleanStrictOrNull().takeIf { p.getOrNull(13)?.startsWith("canonical-") == true } ?: false,
                baseId = p.getOrElse(24) { "" }.takeIf { p.getOrNull(13)?.startsWith("canonical-") == true }.orEmpty(),
                materialId = p.getOrElse(25) { "" }.takeIf { p.getOrNull(13)?.startsWith("canonical-") == true }.orEmpty(),
                modificationIds = p.getOrElse(26) { "" }.takeIf { p.getOrNull(13)?.startsWith("canonical-") == true }?.toNestedList().orEmpty(),
                gemIds = p.getOrElse(27) { "" }.takeIf { p.getOrNull(13)?.startsWith("canonical-") == true }?.toNestedList().orEmpty(),
                mechanicalEffects = p.getOrElse(28) { "" }.takeIf { p.getOrNull(13)?.startsWith("canonical-") == true }
                    ?.split(MODIFIER_ROW)?.filter(String::isNotBlank)?.map { encoded ->
                        val fields = encoded.split(MODIFIER_FIELD)
                        ItemEffect(
                            id = fields.getOrElse(0) { "" },
                            type = runCatching { ItemEffectType.valueOf(fields.getOrElse(1) { "" }) }.getOrDefault(ItemEffectType.DURABILITY),
                            value = fields.getOrNull(2)?.toIntOrNull() ?: 0,
                            target = fields.getOrElse(3) { "" },
                            condition = runCatching { ItemEffectCondition.valueOf(fields.getOrElse(4) { "" }) }.getOrDefault(ItemEffectCondition.WIELDED),
                            description = fields.getOrElse(5) { "" },
                            resolvedTargetId = fields.getOrElse(6) { "" },
                        )
                    }.orEmpty(),
                technologyIds = p.getOrElse(33) { "" }.takeIf { p.getOrNull(13) == "canonical-v6" }?.toNestedList().orEmpty(),
                gemSlots = p.getOrNull(34)?.toIntOrNull().takeIf { p.getOrNull(13) == "canonical-v6" } ?: 0,
                technologySlots = p.getOrNull(35)?.toIntOrNull().takeIf { p.getOrNull(13) == "canonical-v6" } ?: 0,
                dataVersion = p.getOrNull(29)?.toIntOrNull().takeIf { p.getOrNull(13) in setOf("canonical-v4", "canonical-v5", "canonical-v6") } ?: 0,
                itemCondition = p.enumAt(31, if (legacyDurability(p.getOrElse(4) { "" }).second == 0) ItemCondition.SCRAP else ItemCondition.NORMAL),
                backpackCapacity = p.getOrNull(32)?.toIntOrNull().takeIf { p.getOrNull(13) in setOf("canonical-v5", "canonical-v6") } ?: 0,
            )
        }
    }

    private fun legacyDurability(value: String): Pair<Int, Int> {
        val parts = value.split('/')
        val maximum = parts.getOrNull(1)?.toIntOrNull() ?: parts.firstOrNull()?.toIntOrNull() ?: 0
        val current = parts.firstOrNull()?.toIntOrNull()?.coerceIn(0, maximum) ?: maximum
        return current to maximum
    }

    @TypeConverter fun knowledgesToString(value: List<SpecialKnowledge>) = value.joinToString(ROW) {
        listOf(
            it.id,
            it.name,
            it.attribute,
            it.value.toString(),
            it.catalogEntryId,
            it.catalogVersion.toString(),
            it.category,
            it.description,
            it.prerequisites.nested(),
            it.mechanicalEffect,
            it.source,
            it.ruleReference,
            it.keywords.nested(),
            it.repeatable.toString(),
            it.adjustment.toString(),
            "canonical-v3", it.milestoneLevels.joinToString(","), it.specializationParentId,
            it.milestoneRewards.joinToString(MILESTONE_ROW) { reward ->
                listOf(reward.level.toString(), reward.type.name, reward.rewardCatalogId, reward.grantedEntityId).joinToString(MILESTONE_FIELD)
            }, it.pendingMilestoneLevels.joinToString(","), it.pendingTargetLevel?.toString().orEmpty(),
        ).row()
    }

    @TypeConverter fun stringToKnowledges(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { row ->
        row.parts().let { p ->
            SpecialKnowledge(
                id = p.getOrElse(0) { "" },
                name = p.getOrElse(1) { "" },
                attribute = p.getOrElse(2) { "" },
                value = p.getOrNull(3)?.toIntOrNull() ?: 0,
                catalogEntryId = p.getOrElse(4) { "" },
                catalogVersion = p.getOrNull(5)?.toIntOrNull() ?: 0,
                category = p.getOrElse(6) { "" },
                description = p.getOrElse(7) { "" },
                prerequisites = p.getOrElse(8) { "" }.toNestedList(),
                mechanicalEffect = p.getOrElse(9) { "" },
                source = p.getOrElse(10) { "" },
                ruleReference = p.getOrElse(11) { "" },
                keywords = p.getOrElse(12) { "" }.toNestedList(),
                repeatable = p.getOrNull(13)?.toBooleanStrictOrNull() ?: false,
                adjustment = p.getOrNull(14)?.toIntOrNull() ?: 0,
                milestoneLevels = p.getOrElse(16) { "" }.takeIf { p.getOrNull(15)?.startsWith("canonical-") == true }.orEmpty().split(',').mapNotNull(String::toIntOrNull),
                specializationParentId = p.getOrElse(17) { "" }.takeIf { p.getOrNull(15)?.startsWith("canonical-") == true }.orEmpty(),
                milestoneRewards = p.getOrElse(18) { "" }.takeIf { p.getOrNull(15) in setOf("canonical-v2", "canonical-v3") }
                    .orEmpty().split(MILESTONE_ROW).filter(String::isNotBlank).map { encoded ->
                        val fields = encoded.split(MILESTONE_FIELD)
                        KnowledgeMilestoneReward(
                            level = fields.getOrNull(0)?.toIntOrNull() ?: 3,
                            type = runCatching { KnowledgeMilestoneRewardType.valueOf(fields.getOrElse(1) { "" }) }
                                .getOrDefault(KnowledgeMilestoneRewardType.POWER),
                            rewardCatalogId = fields.getOrElse(2) { "" },
                            grantedEntityId = fields.getOrElse(3) { "" },
                        )
                    },
                pendingMilestoneLevels = p.getOrElse(19) { "" }.takeIf { p.getOrNull(15) == "canonical-v3" }
                    .orEmpty().split(',').mapNotNull(String::toIntOrNull),
                pendingTargetLevel = p.getOrNull(20)?.takeIf { p.getOrNull(15) == "canonical-v3" }?.toIntOrNull(),
            )
        }
    }

    @TypeConverter fun bodyToString(value: List<BodyRegion>) = value.joinToString(ROW) { listOf(it.roll.toString(), it.name, it.failures.toString(), it.damage, it.implants, it.equipment, it.localProtection.toString(), it.generalProtection.toString(), it.equippedItemIds.joinToString(","), it.state.name, it.implantInstanceIds.joinToString(","), it.prosthesisInstanceId).row() }
    @TypeConverter fun stringToBody(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { it.parts().let { p -> BodyRegion(p[0].toIntOrNull() ?: 0, p.getOrElse(1) { "" }, p.getOrNull(2)?.toIntOrNull() ?: 0, p.getOrElse(3) { "" }, p.getOrElse(4) { "" }, p.getOrElse(5) { "" }, p.getOrNull(6)?.toIntOrNull() ?: 0, p.getOrNull(7)?.toIntOrNull() ?: 0, p.getOrElse(8) { "" }.split(',').filter(String::isNotBlank), runCatching { BodyIntegrity.valueOf(p.getOrElse(9) { "Intact" }) }.getOrDefault(BodyIntegrity.Intact), p.getOrElse(10) { "" }.split(',').filter(String::isNotBlank), p.getOrElse(11) { "" }) } }

    @TypeConverter fun organsToString(value: List<OrganStatus>) = value.joinToString(ROW) { listOf(it.id, it.name, it.failures.toString(), it.implant, it.effect, it.slot.name, it.state.name, it.implantInstanceId).row() }
    @TypeConverter fun stringToOrgans(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { it.parts().let { p -> OrganStatus(p[0], p.getOrElse(1) { "" }, p.getOrNull(2)?.toIntOrNull() ?: 0, p.getOrElse(3) { "" }, p.getOrElse(4) { "" }, runCatching { OrganSlot.valueOf(p.getOrElse(5) { "Other" }) }.getOrDefault(OrganSlot.Other), runCatching { BodyIntegrity.valueOf(p.getOrElse(6) { "Intact" }) }.getOrDefault(BodyIntegrity.Intact), p.getOrElse(7) { "" }) } }

    @TypeConverter fun abilitiesToString(value: List<MysticAbility>) = value.joinToString(ROW) {
        listOf(
            it.id, it.type, it.name, it.cost, it.action, it.range, it.duration, it.effect,
            it.favorite.toString(), it.available.toString(), "catalog-v2", it.category, it.source,
            it.ruleReference, it.catalogEntryId, it.catalogVersion.toString(),
            "canonical-v2", it.canonicalSource.name, it.knowledgeId, it.knowledgeLevel?.toString().orEmpty(),
            it.costType.name, it.costValue.toString(), it.executionType.name, it.rangeType.name,
            it.targetArea, it.durationType.name, it.resistance.name, it.timeValue.toString(), it.timeUnit.name,
            it.ashSource.name, it.ashPurity.name, it.linkedInventoryItemId, it.inscriberId, it.revision.toString(),
            it.inscriberPower.toString(), it.inscriberRunicKnowledge.toString(),
            it.durationValue.toString(), it.durationUnit.name,
        ).row()
    }
    @TypeConverter fun stringToAbilities(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map {
        it.parts().let { p ->
            MysticAbility(
                id = p[0], type = p.getOrElse(1) { "" }, name = p.getOrElse(2) { "" },
                cost = p.getOrElse(3) { "" }, action = p.getOrElse(4) { "" }, range = p.getOrElse(5) { "" },
                duration = p.getOrElse(6) { "" }, effect = p.getOrElse(7) { "" },
                favorite = p.getOrNull(8)?.toBooleanStrictOrNull() ?: false,
                available = p.getOrNull(9)?.toBooleanStrictOrNull() ?: true,
                category = p.getOrElse(11) { "" }.takeIf { p.getOrNull(10) == "catalog-v2" }.orEmpty(),
                source = p.getOrElse(12) { "" }.takeIf { p.getOrNull(10) == "catalog-v2" }.orEmpty(),
                ruleReference = p.getOrElse(13) { "" }.takeIf { p.getOrNull(10) == "catalog-v2" }.orEmpty(),
                catalogEntryId = p.getOrElse(14) { "" }.takeIf { p.getOrNull(10) == "catalog-v2" }.orEmpty(),
                catalogVersion = p.getOrNull(15)?.toIntOrNull().takeIf { p.getOrNull(10) == "catalog-v2" } ?: 0,
                canonicalSource = p.enumAt(17, AbilitySource.NARRATIVE),
                knowledgeId = p.getOrElse(18) { "" }.takeIf { p.getOrNull(16)?.startsWith("canonical-") == true }.orEmpty(),
                knowledgeLevel = p.getOrNull(19)?.toIntOrNull().takeIf { p.getOrNull(16)?.startsWith("canonical-") == true },
                costType = p.enumAt(20, legacyCostType(p.getOrElse(3) { "" }, p.getOrElse(1) { "" })),
                costValue = p.getOrNull(21)?.toIntOrNull().takeIf { p.getOrNull(16)?.startsWith("canonical-") == true } ?: legacyCostValue(p.getOrElse(3) { "" }),
                executionType = p.enumAt(22, legacyExecution(p.getOrElse(4) { "" })),
                rangeType = p.enumAt(23, legacyRange(p.getOrElse(5) { "" })),
                targetArea = p.getOrElse(24) { "" }.takeIf { p.getOrNull(16)?.startsWith("canonical-") == true }.orEmpty(),
                durationType = p.enumAt(25, legacyDuration(p.getOrElse(6) { "" })),
                durationValue = p.getOrNull(36)?.toIntOrNull().takeIf { p.getOrNull(16) == "canonical-v2" } ?: 0,
                durationUnit = p.enumAt(37, AbilityTimeUnit.HOURS),
                resistance = p.enumAt(26, AbilityResistance.NONE),
                timeValue = p.getOrNull(27)?.toIntOrNull().takeIf { p.getOrNull(16)?.startsWith("canonical-") == true } ?: 0,
                timeUnit = p.enumAt(28, AbilityTimeUnit.MINUTES),
                ashSource = p.enumAt(29, AshSource.FIRE),
                ashPurity = p.enumAt(30, AshPurity.RAW),
                linkedInventoryItemId = p.getOrElse(31) { "" }.takeIf { p.getOrNull(16)?.startsWith("canonical-") == true }.orEmpty(),
                inscriberId = p.getOrElse(32) { "" }.takeIf { p.getOrNull(16)?.startsWith("canonical-") == true }.orEmpty(),
                revision = p.getOrNull(33)?.toIntOrNull().takeIf { p.getOrNull(16)?.startsWith("canonical-") == true } ?: 1,
                inscriberPower = p.getOrNull(34)?.toIntOrNull().takeIf { p.getOrNull(16)?.startsWith("canonical-") == true } ?: 0,
                inscriberRunicKnowledge = p.getOrNull(35)?.toIntOrNull().takeIf { p.getOrNull(16)?.startsWith("canonical-") == true } ?: 0,
            )
        }
    }

    @TypeConverter fun conditionsToString(value: List<ConditionEffect>) = value.joinToString(ROW) { listOf(it.id, it.name, it.intensity, it.duration, it.origin, it.summary).row() }
    @TypeConverter fun stringToConditions(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { it.parts().let { p -> ConditionEffect(p[0], p.getOrElse(1) { "" }, p.getOrElse(2) { "" }, p.getOrElse(3) { "" }, p.getOrElse(4) { "" }, p.getOrElse(5) { "" }) } }

    @TypeConverter fun personalNotesToString(value: List<PersonalNote>) =
        value.joinToString(ROW) { listOf(it.id, it.title, it.text).row() }

    @TypeConverter fun stringToPersonalNotes(value: String) =
        if (value.isEmpty()) emptyList() else value.split(ROW).map { row ->
            row.parts().let { fields ->
                PersonalNote(
                    id = fields.getOrElse(0) { "" },
                    title = fields.getOrElse(1) { "" },
                    text = fields.getOrElse(2) { "" },
                )
            }
        }
}

private fun inferLegacyPowerSource(origin: String): PowerSourceType = when {
    origin.startsWith("Caminho", ignoreCase = true) -> PowerSourceType.PATH
    origin.startsWith("Raça", ignoreCase = true) || origin.startsWith("Sub-raça", ignoreCase = true) -> PowerSourceType.RACE
    else -> PowerSourceType.MANUAL
}

private inline fun <reified T : Enum<T>> List<String>.enumAt(index: Int, fallback: T): T =
    runCatching { enumValueOf<T>(getOrElse(index) { "" }) }.getOrDefault(fallback)

private fun String?.toModifiers(): List<AbilityModifier> = this.orEmpty().split(MODIFIER_ROW).filter(String::isNotBlank).map { encoded ->
    val values = encoded.split(MODIFIER_FIELD)
    AbilityModifier(
        id = values.getOrElse(0) { "" },
        targetType = runCatching { AbilityModifierTarget.valueOf(values.getOrElse(1) { "" }) }.getOrDefault(AbilityModifierTarget.ATTRIBUTE),
        targetId = values.getOrElse(2) { "" },
        value = values.getOrNull(3)?.toIntOrNull() ?: 0,
    )
}

private fun legacyCostType(cost: String, type: String = ""): AbilityCostType {
    if (type.equals("Cinza", true)) return AbilityCostType.DOSE
    return when {
        cost.contains("arcano", true) || cost.contains("PM", true) -> AbilityCostType.ARCANE
        cost.contains("energia", true) || cost.contains("PE", true) -> AbilityCostType.ENERGY
        cost.contains("destino", true) -> AbilityCostType.DESTINY
        cost.contains("sanidade", true) || cost.contains("PS", true) -> AbilityCostType.SANITY
        cost.contains("vida", true) || cost.contains("PV", true) || cost.contains("HP", true) -> AbilityCostType.LIFE
        else -> AbilityCostType.NONE
    }
}

private fun legacyCostValue(cost: String): Int = Regex("\\d+").find(cost)?.value?.toIntOrNull() ?: 0

private fun legacyExecution(action: String): AbilityExecution = when {
    action.contains("reação", true) -> AbilityExecution.REACTION
    action.contains("turno", true) -> AbilityExecution.TURN
    action.contains("livre", true) -> AbilityExecution.FREE
    action.contains("passiv", true) -> AbilityExecution.PASSIVE
    action.contains("minuto", true) || action.contains("hora", true) || action.contains("dia", true) -> AbilityExecution.TIME
    else -> AbilityExecution.ACTION
}

private fun legacyRange(range: String): AbilityRange = when {
    range.contains("indefin", true) -> AbilityRange.INDEFINITE
    range.contains("pessoal", true) || range.contains("toque", true) -> AbilityRange.PERSONAL
    Regex("(?:9|[1-8])\\s*m", RegexOption.IGNORE_CASE).containsMatchIn(range) -> AbilityRange.SHORT
    Regex("(?:[12]\\d|30)\\s*m", RegexOption.IGNORE_CASE).containsMatchIn(range) -> AbilityRange.MEDIUM
    else -> AbilityRange.LONG
}

private fun legacyDuration(duration: String): AbilityDuration = when {
    duration.contains("turno", true) -> AbilityDuration.TURNS
    duration.contains("cena", true) -> AbilityDuration.SCENE
    duration.contains("sessão", true) -> AbilityDuration.SESSION
    duration.contains("instant", true) -> AbilityDuration.INSTANT
    else -> AbilityDuration.TIME
}
