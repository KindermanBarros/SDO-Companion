package com.kinderman.sdo.data.local

import androidx.room.TypeConverter
import com.kinderman.sdo.domain.model.AttributeValue
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
        row.split("\u001c").let { fields -> AttributeValue(fields[0], fields[1], fields.getOrNull(2)?.toIntOrNull() ?: 0, fields.getOrNull(3)?.toIntOrNull() ?: 0, fields.getOrNull(4).orEmpty().split(ROW).filter(String::isNotEmpty).map { skill -> skill.parts().let { SkillValue(it[0], it.getOrNull(1)?.toIntOrNull() ?: 0, it.getOrNull(2)?.toIntOrNull() ?: 0) } }) }
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
