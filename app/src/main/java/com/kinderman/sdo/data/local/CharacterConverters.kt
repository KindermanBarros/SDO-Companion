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
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemBonus
import com.kinderman.sdo.domain.model.ItemBonusType
import com.kinderman.sdo.domain.model.MysticAbility
import com.kinderman.sdo.domain.model.OrganStatus
import com.kinderman.sdo.domain.model.PersonalNote
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.PowerSourceType
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SkillValue
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.defaultAttributes

private const val ROW = "\u001e"
private const val FIELD = "\u001f"
private const val BONUS_ROW = "\u001b"
private const val BONUS_FIELD = "\u001a"
private const val NESTED = "\u0019"
private const val MODIFIER_ROW = "\u0018"
private const val MODIFIER_FIELD = "\u0017"
private fun String.parts() = split(FIELD)
private fun List<String>.row() = joinToString(FIELD)
private fun List<String>.nested() = joinToString(NESTED)
private fun String.toNestedList() = if (isBlank()) emptyList() else split(NESTED)

class CharacterConverters {
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
            item.id, item.state, item.name, item.load.toString(), item.durability, item.region,
            item.effect, item.pg.toString(), item.pl.toString(), item.category,
            item.agilityLimit?.toString().orEmpty(), item.quality,
            item.bonuses.joinToString(BONUS_ROW) { bonus ->
                listOf(bonus.type.name, bonus.target, bonus.value.toString()).joinToString(BONUS_FIELD)
            },
            "canonical-v1", item.quantity.toString(), item.linkedAshId, item.ashPurity.name,
        ).row()
    }

    @TypeConverter fun stringToInventory(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { row ->
        row.parts().let { p ->
            InventoryItem(
                id = p[0], state = p.getOrElse(1) { "M" }, name = p.getOrElse(2) { "" },
                load = p.getOrNull(3)?.toIntOrNull() ?: 0, durability = p.getOrElse(4) { "" },
                region = p.getOrElse(5) { "" }, effect = p.getOrElse(6) { "" },
                pg = p.getOrNull(7)?.toIntOrNull() ?: 0, pl = p.getOrNull(8)?.toIntOrNull() ?: 0,
                category = p.getOrElse(9) { "" }, agilityLimit = p.getOrNull(10)?.toIntOrNull(),
                quality = p.getOrElse(11) { "Comum" },
                bonuses = p.getOrElse(12) { "" }.split(BONUS_ROW).filter(String::isNotBlank).map { encoded ->
                    val fields = encoded.split(BONUS_FIELD)
                    ItemBonus(
                        type = runCatching { ItemBonusType.valueOf(fields[0]) }.getOrDefault(ItemBonusType.ATTRIBUTE),
                        target = fields.getOrElse(1) { "" },
                        value = fields.getOrNull(2)?.toIntOrNull() ?: 0,
                    )
                },
                quantity = p.getOrNull(14)?.toIntOrNull().takeIf { p.getOrNull(13) == "canonical-v1" } ?: 0,
                linkedAshId = p.getOrElse(15) { "" }.takeIf { p.getOrNull(13) == "canonical-v1" }.orEmpty(),
                ashPurity = p.enumAt(16, AshPurity.RAW),
            )
        }
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
            "canonical-v1", it.milestoneLevels.joinToString(","), it.specializationParentId,
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
                milestoneLevels = p.getOrElse(16) { "" }.takeIf { p.getOrNull(15) == "canonical-v1" }.orEmpty().split(',').mapNotNull(String::toIntOrNull),
                specializationParentId = p.getOrElse(17) { "" }.takeIf { p.getOrNull(15) == "canonical-v1" }.orEmpty(),
            )
        }
    }

    @TypeConverter fun bodyToString(value: List<BodyRegion>) = value.joinToString(ROW) { listOf(it.roll.toString(), it.name, it.failures.toString(), it.damage, it.implants, it.equipment, it.localProtection.toString(), it.generalProtection.toString(), it.equippedItemIds.joinToString(",")).row() }
    @TypeConverter fun stringToBody(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { it.parts().let { p -> BodyRegion(p[0].toIntOrNull() ?: 0, p.getOrElse(1) { "" }, p.getOrNull(2)?.toIntOrNull() ?: 0, p.getOrElse(3) { "" }, p.getOrElse(4) { "" }, p.getOrElse(5) { "" }, p.getOrNull(6)?.toIntOrNull() ?: 0, p.getOrNull(7)?.toIntOrNull() ?: 0, p.getOrElse(8) { "" }.split(',').filter(String::isNotBlank)) } }

    @TypeConverter fun organsToString(value: List<OrganStatus>) = value.joinToString(ROW) { listOf(it.id, it.name, it.failures.toString(), it.implant, it.effect).row() }
    @TypeConverter fun stringToOrgans(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { it.parts().let { p -> OrganStatus(p[0], p.getOrElse(1) { "" }, p.getOrNull(2)?.toIntOrNull() ?: 0, p.getOrElse(3) { "" }, p.getOrElse(4) { "" }) } }

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
