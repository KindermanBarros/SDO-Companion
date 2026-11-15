package com.kinderman.sdo.data.sync

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.kinderman.sdo.data.local.CharacterRecord

/** Preserves the existing record contract while keeping slash-containing sync keys out of RTDB paths. */
object SheetWireCodec {
    private val gson = GsonBuilder().serializeNulls().create()
    private val localFields = setOf("dirty", "lastSyncedAt")

    fun encode(record: CharacterRecord): String = gson.toJson(record)
    fun decode(json: String): CharacterRecord {
        val defaults = gson.toJsonTree(CharacterRecord()).asJsonObject
        val incoming = JsonParser.parseString(json).asJsonObject
        incoming.entrySet().forEach { (key, value) ->
            if (key == "fieldUpdatedAt" && value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                defaults.add(key, JsonParser.parseString(value.asString))
            } else if (!value.isJsonNull || defaults.get(key)?.isJsonNull != false) {
                defaults.add(key, value)
            }
        }
        return gson.fromJson(defaults, CharacterRecord::class.java)
    }

    fun state(record: CharacterRecord): Map<String, Any?> {
        val value = gson.toJsonTree(record).asJsonObject
        localFields.forEach(value::remove)
        value.addProperty("fieldUpdatedAt", gson.toJson(record.fieldUpdatedAt))
        return objectMap(value)
    }

    fun changes(before: CharacterRecord, after: CharacterRecord): Map<String, Any?> {
        val old = state(before)
        return state(after).filter { (key, value) -> key !in protectedFields && old[key] != value }
    }

    fun json(value: Any?): String = gson.toJson(value)
    fun session(value: Any?): com.kinderman.sdo.domain.model.SessionCommand =
        gson.fromJson(gson.toJson(value), com.kinderman.sdo.domain.model.SessionCommand::class.java)
    fun map(json: String): Map<String, Any?> = objectMap(JsonParser.parseString(json).asJsonObject)
    fun map(value: Any): Map<String, Any?> = objectMap(gson.toJsonTree(value).asJsonObject)
    private fun objectMap(value: JsonObject): Map<String, Any?> = value.entrySet().associate { it.key to plain(it.value) }
    private fun plain(value: JsonElement): Any? = when {
        value.isJsonNull -> null
        value.isJsonObject -> objectMap(value.asJsonObject)
        value.isJsonArray -> value.asJsonArray.map(::plain)
        value.asJsonPrimitive.isBoolean -> value.asBoolean
        value.asJsonPrimitive.isNumber -> value.asString.toLongOrNull() ?: value.asDouble
        else -> value.asString
    }
    private val protectedFields = setOf("id", "ownerId", "campaignId", "fieldUpdatedAt", "updatedAt", "lastEditedBy", "deleted", "lockType", "isLocked", "lockedBy", "lockedAt", "appliedDeliveryIds")
}
