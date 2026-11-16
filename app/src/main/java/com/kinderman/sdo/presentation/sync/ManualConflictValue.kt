package com.kinderman.sdo.presentation.sync

import com.kinderman.sdo.domain.model.SheetConflictItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Small typed editor boundary; complex game structures remain editable on the sheet. */
internal data class ManualConflictValue(
    val kind: Kind,
    val initialValues: List<String>,
    private val fieldKey: String,
) {
    enum class Kind { TEXT, INTEGER, DECIMAL, BOOLEAN, RESOURCE }

    fun encode(values: List<String>): String? {
        if (values.size != initialValues.size) return null
        return when (kind) {
            Kind.TEXT -> JsonPrimitive(values.single()).toString()
            Kind.BOOLEAN -> values.single().toBooleanStrictOrNull()?.let { JsonPrimitive(it).toString() }
            Kind.INTEGER -> values.single().toIntOrNull()?.takeIf { number ->
                when (fieldKey) {
                    "level", "creationStep" -> number in 1..20
                    "creationRulesVersion" -> number >= 1
                    "money", "quantity", "containerCapacity" -> number >= 0
                    else -> true
                }
            }?.let { JsonPrimitive(it).toString() }
            Kind.DECIMAL -> values.single().toDoubleOrNull()?.takeIf(Double::isFinite)
                ?.let { JsonPrimitive(it).toString() }
            Kind.RESOURCE -> {
                val numbers = values.map { it.toIntOrNull() ?: return null }
                if (numbers[0] < 0 || numbers[1] < 0 ||
                    (fieldKey == "corruption" && numbers[0] > 100)) return null
                JsonObject(listOf("current", "maximum", "adjustment").zip(numbers).associate {
                    it.first to JsonPrimitive(it.second)
                }).toString()
            }
        }
    }

    companion object {
        fun from(conflict: SheetConflictItem): ManualConflictValue? {
            if (!(conflict.targetKey.startsWith("field:") || "/field:" in conflict.targetKey)) return null
            val key = conflict.targetKey.substringAfterLast("field:")
            if (key.endsWith("Payload") || key in setOf("itemCreationDraft", "definition", "meta")) return null
            val local = runCatching { Json.parseToJsonElement(conflict.localPayloadJson) }.getOrNull() ?: return null
            val remote = runCatching { Json.parseToJsonElement(conflict.remotePayloadJson) }.getOrNull() ?: return null
            if (local is JsonObject && remote is JsonObject) {
                val keys = listOf("current", "maximum", "adjustment")
                if (local.keys != keys.toSet() || remote.keys != keys.toSet() ||
                    (local.values + remote.values).any { it !is JsonPrimitive || it.isString || it.intOrNull == null }) return null
                return ManualConflictValue(Kind.RESOURCE, keys.map { local.getValue(it).jsonPrimitive.content }, key)
            }
            if (local !is JsonPrimitive || remote !is JsonPrimitive) return null
            val kind = when {
                local.isString && remote.isString -> Kind.TEXT
                local.booleanOrNull != null && remote.booleanOrNull != null -> Kind.BOOLEAN
                local.intOrNull != null && remote.intOrNull != null -> Kind.INTEGER
                local.doubleOrNull?.isFinite() == true && remote.doubleOrNull?.isFinite() == true -> Kind.DECIMAL
                else -> return null
            }
            return ManualConflictValue(kind, listOf(local.content), key)
        }
    }
}
