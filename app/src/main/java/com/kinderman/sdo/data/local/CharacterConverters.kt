package com.kinderman.sdo.data.local

import androidx.room.TypeConverter
import com.kinderman.sdo.domain.model.AttributeValue
import com.kinderman.sdo.domain.model.BodyRegion
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.MysticAbility
import com.kinderman.sdo.domain.model.OrganStatus
import com.kinderman.sdo.domain.model.PersonalNote
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SkillValue
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.defaultAttributes

private const val ROW = "\u001e"
private const val FIELD = "\u001f"
private fun String.parts() = split(FIELD)
private fun List<String>.row() = joinToString(FIELD)

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

    @TypeConverter fun powersToString(value: List<Power>) = value.joinToString(ROW) { listOf(it.id, it.name, it.origin, it.cost, it.action, it.range, it.duration, it.limit, it.effect).row() }
    @TypeConverter fun stringToPowers(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { row -> row.parts().let { p ->
        if (p.size >= 9) Power(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8])
        else Power(name = p.getOrElse(0) { "" }, origin = p.getOrElse(1) { "" }, cost = p.getOrElse(2) { "" }, action = p.getOrElse(3) { "" }, range = p.getOrElse(4) { "" }, duration = p.getOrElse(5) { "" }, limit = p.getOrElse(6) { "" }, effect = p.getOrElse(7) { "" })
    } }

    @TypeConverter fun inventoryToString(value: List<InventoryItem>) = value.joinToString(ROW) { listOf(it.id, it.state, it.name, it.load.toString(), it.durability, it.region, it.effect).row() }
    @TypeConverter fun stringToInventory(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { it.parts().let { p -> InventoryItem(p[0], p.getOrElse(1) { "M" }, p.getOrElse(2) { "" }, p.getOrNull(3)?.toIntOrNull() ?: 0, p.getOrElse(4) { "" }, p.getOrElse(5) { "" }, p.getOrElse(6) { "" }) } }

    @TypeConverter fun knowledgesToString(value: List<SpecialKnowledge>) = value.joinToString(ROW) { listOf(it.id, it.name, it.attribute, it.value.toString()).row() }
    @TypeConverter fun stringToKnowledges(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { it.parts().let { p -> SpecialKnowledge(p[0], p.getOrElse(1) { "" }, p.getOrElse(2) { "" }, p.getOrNull(3)?.toIntOrNull() ?: 0) } }

    @TypeConverter fun bodyToString(value: List<BodyRegion>) = value.joinToString(ROW) { listOf(it.roll.toString(), it.name, it.failures.toString(), it.damage, it.implants, it.equipment, it.localProtection.toString(), it.generalProtection.toString()).row() }
    @TypeConverter fun stringToBody(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { it.parts().let { p -> BodyRegion(p[0].toIntOrNull() ?: 0, p.getOrElse(1) { "" }, p.getOrNull(2)?.toIntOrNull() ?: 0, p.getOrElse(3) { "" }, p.getOrElse(4) { "" }, p.getOrElse(5) { "" }, p.getOrNull(6)?.toIntOrNull() ?: 0, p.getOrNull(7)?.toIntOrNull() ?: 0) } }

    @TypeConverter fun organsToString(value: List<OrganStatus>) = value.joinToString(ROW) { listOf(it.id, it.name, it.failures.toString(), it.implant, it.effect).row() }
    @TypeConverter fun stringToOrgans(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { it.parts().let { p -> OrganStatus(p[0], p.getOrElse(1) { "" }, p.getOrNull(2)?.toIntOrNull() ?: 0, p.getOrElse(3) { "" }, p.getOrElse(4) { "" }) } }

    @TypeConverter fun abilitiesToString(value: List<MysticAbility>) = value.joinToString(ROW) { listOf(it.id, it.type, it.name, it.cost, it.action, it.range, it.duration, it.effect).row() }
    @TypeConverter fun stringToAbilities(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { it.parts().let { p -> MysticAbility(p[0], p.getOrElse(1) { "" }, p.getOrElse(2) { "" }, p.getOrElse(3) { "" }, p.getOrElse(4) { "" }, p.getOrElse(5) { "" }, p.getOrElse(6) { "" }, p.getOrElse(7) { "" }) } }

    @TypeConverter fun conditionsToString(value: List<ConditionEffect>) = value.joinToString(ROW) { listOf(it.id, it.name, it.intensity, it.duration, it.origin).row() }
    @TypeConverter fun stringToConditions(value: String) = if (value.isEmpty()) emptyList() else value.split(ROW).map { it.parts().let { p -> ConditionEffect(p[0], p.getOrElse(1) { "" }, p.getOrElse(2) { "" }, p.getOrElse(3) { "" }, p.getOrElse(4) { "" }) } }

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
