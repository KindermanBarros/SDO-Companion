package com.kinderman.sdo.domain.model

import java.util.Base64

private const val PENDING_PREFIX = "@pending/"
private const val DELETED_PREFIX = "@deleted/"
private const val SERVER_REVISION = "@revision"
private const val BASE_REVISION = "@baseRevision"
private const val PROTOCOL_VERSION = "@protocol"

private fun elementKey(id: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(id.toByteArray(Charsets.UTF_8))
fun Character.pendingSyncKeys(): Set<String> = fieldUpdatedAt.keys.filter { it.startsWith(PENDING_PREFIX) }
    .mapTo(mutableSetOf()) { it.removePrefix(PENDING_PREFIX) }
fun Character.serverSyncRevision(): Long = fieldUpdatedAt[SERVER_REVISION] ?: 0L
fun needsLegacySyncReview(local: Character, remote: Character): Boolean = local.dirty &&
    local.pendingSyncKeys().isEmpty() && local.fieldUpdatedAt[PROTOCOL_VERSION] != 1L &&
    remote.fieldUpdatedAt[PROTOCOL_VERSION] == 1L && characterConflictFields(local, remote).isNotEmpty()
fun Character.withoutPendingSyncKeys(): Character = copy(
    fieldUpdatedAt = fieldUpdatedAt.filterKeys { !it.startsWith(PENDING_PREFIX) && it != BASE_REVISION },
)

private fun <T> elementChanges(group: String, before: List<T>, after: List<T>, id: (T) -> String, fields: (T, T) -> Set<String>): Set<String> {
    if (before == after) return emptySet()
    require(before.map(id).distinct().size == before.size && after.map(id).distinct().size == after.size) {
        "A ficha contém identificadores repetidos em uma lista. Revise os itens antes de salvar."
    }
    val old = before.associateBy(id)
    val new = after.associateBy(id)
    return buildSet {
        (old.keys + new.keys).forEach { key ->
            val path = "$group/${elementKey(key)}"
            val previous = old[key]
            val next = new[key]
            if (previous == null || next == null) add(path)
            else fields(previous, next).forEach { add("$path/$it") }
        }
    }
}

/** Actual edit paths, rather than differences between unrelated complete snapshots. */
fun characterEditKeys(before: Character, after: Character): Set<String> = buildSet {
    val collections = setOf("inventory", "itemStates", "conditionInstances", "abilities", "personalNotes", "attributes")
    addAll(characterConflictFields(before, after).map { it.id }.filterNot { it in collections })
    addAll(elementChanges("inventory", before.inventory, after.inventory, { it.id }, ::changedInventoryItemFields))
    addAll(elementChanges("itemStates", before.itemStates, after.itemStates, { it.id.value }, ::changedItemStateFields))
    addAll(elementChanges("conditionInstances", before.conditionInstances, after.conditionInstances, { it.instanceId.value }, ::changedConditionInstanceFields))
    addAll(elementChanges("conditions", before.conditions, after.conditions, { it.id }, ::changedConditionEffectFields))
    addAll(elementChanges("abilities", before.abilities, after.abilities, { it.id }, ::changedAbilityFields))
    addAll(elementChanges("powers", before.powers, after.powers, { it.id }, ::changedPowerFields))
    addAll(elementChanges("mysticAbilities", before.mysticAbilities, after.mysticAbilities, { it.id }, ::changedMysticAbilityFields))
    addAll(elementChanges("personalNotes", before.personalNotes, after.personalNotes, { it.id }, ::changedPersonalNoteFields))
    addAll(elementChanges("attributes", before.attributes, after.attributes, { it.acronym.ifBlank { it.name } }, ::changedAttributeValueFields))
}

private fun <T> patchElements(group: String, target: List<T>, source: List<T>, keys: Set<String>, metadata: Map<String, Long>, id: (T) -> String, copy: (T, T, Set<String>) -> T): List<T> {
    if (group in keys) return source
    if (keys.none { it.startsWith("$group/") }) return target
    val result = target.associateByTo(linkedMapOf(), id)
    val incoming = source.associateBy(id)
    val editedIds = keys.filter { it.startsWith("$group/") }.map { it.removePrefix("$group/").substringBefore('/') }.toSet()
    (result.keys + incoming.keys).forEach { key ->
        val encoded = elementKey(key)
        if (encoded !in editedIds) return@forEach
        val path = "$group/$encoded"
        val value = incoming[key]
        if (path in keys) {
            if (value == null) result.remove(key)
            else if (result[key] == null && metadata[DELETED_PREFIX + path] == null) result[key] = value
            else if (result[key] != null) {
                val properties = keys.filter { it.startsWith("$path/") }.mapTo(mutableSetOf()) { it.removePrefix("$path/") }
                if (properties.isNotEmpty()) result[key] = copy(result.getValue(key), value, properties)
            }
        } else if (value != null) {
            val existing = result[key]
            // A stale edit of a deleted element cannot resurrect it; explicit additions use the root path.
            if (existing != null) {
                val properties = keys.filter { it.startsWith("$path/") }.mapTo(mutableSetOf()) { it.removePrefix("$path/") }
                result[key] = copy(existing, value, properties)
            } else if (metadata[DELETED_PREFIX + path] == null) result[key] = value
        }
    }
    return result.values.toList()
}

/** Apply an intent patch to the latest snapshot, preserving unrelated fields/elements. */
fun applyCharacterEditKeys(target: Character, source: Character, keys: Set<String>): Character {
    val scalarKeys = keys.filterTo(mutableSetOf()) { '/' !in it }
    val base = mergeCharacterConflict(target, source, scalarKeys)
    return base.copy(
        inventory = patchElements("inventory", target.inventory, source.inventory, keys, target.fieldUpdatedAt, { it.id }, ::patchInventoryItem),
        itemStates = patchElements("itemStates", target.itemStates, source.itemStates, keys, target.fieldUpdatedAt, { it.id.value }, ::patchItemState),
        conditionInstances = patchElements("conditionInstances", target.conditionInstances, source.conditionInstances, keys, target.fieldUpdatedAt, { it.instanceId.value }, ::patchConditionInstance),
        conditions = patchElements("conditions", target.conditions, source.conditions, keys, target.fieldUpdatedAt, { it.id }, ::patchConditionEffect),
        abilities = patchElements("abilities", target.abilities, source.abilities, keys, target.fieldUpdatedAt, { it.id }, ::patchAbility),
        powers = patchElements("powers", target.powers, source.powers, keys, target.fieldUpdatedAt, { it.id }, ::patchPower),
        mysticAbilities = patchElements("mysticAbilities", target.mysticAbilities, source.mysticAbilities, keys, target.fieldUpdatedAt, { it.id }, ::patchMysticAbility),
        personalNotes = patchElements("personalNotes", target.personalNotes, source.personalNotes, keys, target.fieldUpdatedAt, { it.id }, ::patchPersonalNote),
        attributes = patchElements("attributes", target.attributes, source.attributes, keys, target.fieldUpdatedAt, { it.acronym.ifBlank { it.name } }) { a, b, fields ->
            val prefix = "@deleted/attributes/${elementKey(a.acronym.ifBlank { a.name })}/"
            val nestedMetadata = target.fieldUpdatedAt.filterKeys { it.startsWith(prefix) }
                .mapKeys { "@deleted/" + it.key.removePrefix(prefix) }
            patchAttributeValue(a, b, fields, nestedMetadata)
        },
        fieldUpdatedAt = target.fieldUpdatedAt,
        appliedDeliveryIds = (target.appliedDeliveryIds + source.appliedDeliveryIds).distinct(),
    )
}

/** A transaction assigns this revision; client wall clocks never choose the committed winner. */
fun commitCharacterEditKeys(remote: Character, local: Character, keys: Set<String> = local.pendingSyncKeys()): Character {
    check(remote.serverSyncRevision() in 0 until Long.MAX_VALUE) { "A revisão da ficha atingiu um valor inválido." }
    val revision = remote.serverSyncRevision() + 1
    val merged = applyCharacterEditKeys(remote, local, keys)
    val metadata = remote.withoutPendingSyncKeys().fieldUpdatedAt.toMutableMap()
    metadata[SERVER_REVISION] = revision
    metadata[PROTOCOL_VERSION] = 1L
    keys.forEach { path ->
        metadata["@server/$path"] = revision
        if (path.count { it == '/' } == 1 || path.startsWith("attributes/") && path.split('/').size == 4 && path.split('/')[2] == "skills") {
            if (merged.containsSyncElement(path)) metadata.remove(DELETED_PREFIX + path)
            else metadata[DELETED_PREFIX + path] = revision
        }
    }
    val now = System.currentTimeMillis()
    check(remote.updatedAt < Long.MAX_VALUE) { "O horário registrado na ficha é inválido." }
    val committedAt = maxOf(now, remote.updatedAt + 1)
    keys.forEach { path ->
        val group = when (val g = path.substringBefore('/')) {
            "powers", "mysticAbilities" -> "abilities"
            "conditions" -> "conditionInstances"
            else -> g
        }
        metadata[group] = maxOf(committedAt, metadata[group] ?: 0L)
    }
    return merged.copy(fieldUpdatedAt = metadata, updatedAt = committedAt, dirty = false,
        lastEditedBy = local.lastEditedBy, lastSyncedAt = committedAt)
}

private fun Character.containsSyncElement(path: String): Boolean {
    val parts = path.split('/')
    if (parts.size == 4 && parts[0] == "attributes" && parts[2] == "skills") {
        return attributes.firstOrNull { elementKey(it.acronym.ifBlank { it.name }) == parts[1] }
            ?.skills?.any { elementKey(it.name) == parts[3] } == true
    }
    val group = path.substringBefore('/')
    val encoded = path.substringAfter('/')
    return when (group) {
        "inventory" -> inventory.any { elementKey(it.id) == encoded }
        "itemStates" -> itemStates.any { elementKey(it.id.value) == encoded }
        "conditionInstances" -> conditionInstances.any { elementKey(it.instanceId.value) == encoded }
        "conditions" -> conditions.any { elementKey(it.id) == encoded }
        "abilities" -> abilities.any { elementKey(it.id) == encoded }
        "powers" -> powers.any { elementKey(it.id) == encoded }
        "mysticAbilities" -> mysticAbilities.any { elementKey(it.id) == encoded }
        "personalNotes" -> personalNotes.any { elementKey(it.id) == encoded }
        "attributes" -> attributes.any { elementKey(it.acronym.ifBlank { it.name }) == encoded }
        else -> true
    }
}

/** Preserve edits made while a committed response was in flight. */
fun preservePendingCharacterEdits(remote: Character, current: Character): Character {
    val pending = current.pendingSyncKeys()
    if (pending.isEmpty()) return remote
    val baseRevision = current.fieldUpdatedAt[BASE_REVISION] ?: current.serverSyncRevision()
    return applyCharacterEditKeys(remote, current, pending).copy(
        fieldUpdatedAt = remote.fieldUpdatedAt +
            current.fieldUpdatedAt.filterKeys { it.startsWith(PENDING_PREFIX) } +
            (BASE_REVISION to baseRevision),
        updatedAt = maxOf(remote.updatedAt, current.updatedAt), lastSyncedAt = remote.updatedAt,
        lastEditedBy = current.lastEditedBy, dirty = true,
    )
}

fun Character.withPendingEditKeys(keys: Set<String>, at: Long = 1L): Character = copy(
    fieldUpdatedAt = fieldUpdatedAt + keys.associate { PENDING_PREFIX + it to maxOf(1L, at) },
)

/** Acknowledging an older upload must remove only the intent that it actually included. */
fun preserveEditsAfterUpload(remote: Character, current: Character, uploaded: Character): Character {
    val pending = current.pendingSyncKeys()
    val changedSinceUpload = characterEditKeys(uploaded, current)
    val remaining = changedSinceUpload.filterTo(mutableSetOf()) { change ->
        pending.any { it == change || it.startsWith("$change/") || change.startsWith("$it/") }
    }
    return preservePendingCharacterEdits(remote, current.withoutPendingSyncKeys().withPendingEditKeys(remaining))
}

private fun changedInventoryItemFields(a: InventoryItem, b: InventoryItem): Set<String> = buildSet {
    if (a.id != b.id) add("id")
    if (a.state != b.state) add("state")
    if (a.name != b.name) add("name")
    if (a.load != b.load) add("load")
    if (a.backpackCapacity != b.backpackCapacity) add("backpackCapacity")
    if (a.durabilityCurrent != b.durabilityCurrent) add("durabilityCurrent")
    if (a.durabilityMax != b.durabilityMax) add("durabilityMax")
    if (a.itemCondition != b.itemCondition) add("itemCondition")
    if (a.region != b.region) add("region")
    if (a.effect != b.effect) add("effect")
    if (a.pg != b.pg) add("pg")
    if (a.pl != b.pl) add("pl")
    if (a.category != b.category) add("category")
    if (a.agilityLimit != b.agilityLimit) add("agilityLimit")
    if (a.quality != b.quality) add("quality")
    if (a.quantity != b.quantity) add("quantity")
    if (a.linkedAshId != b.linkedAshId) add("linkedAshId")
    if (a.ashPurity != b.ashPurity) add("ashPurity")
    if (a.acquisitionSource != b.acquisitionSource) add("acquisitionSource")
    if (a.heritageCost != b.heritageCost) add("heritageCost")
    if (a.purchasePrice != b.purchasePrice) add("purchasePrice")
    if (a.catalogEntryId != b.catalogEntryId) add("catalogEntryId")
    if (a.catalogVersion != b.catalogVersion) add("catalogVersion")
    if (a.acquiredAt != b.acquiredAt) add("acquiredAt")
    if (a.canonical != b.canonical) add("canonical")
    if (a.baseId != b.baseId) add("baseId")
    if (a.materialId != b.materialId) add("materialId")
    if (a.secondaryMaterialId != b.secondaryMaterialId) add("secondaryMaterialId")
    if (a.modificationIds != b.modificationIds) add("modificationIds")
    if (a.gemIds != b.gemIds) add("gemIds")
    if (a.technologyIds != b.technologyIds) add("technologyIds")
    if (a.gemSlots != b.gemSlots) add("gemSlots")
    if (a.technologySlots != b.technologySlots) add("technologySlots")
    if (a.installedEnhancements != b.installedEnhancements) add("installedEnhancements")
    if (a.enhancementSlots != b.enhancementSlots) add("enhancementSlots")
    if (a.enhancementChargesCurrent != b.enhancementChargesCurrent) add("enhancementChargesCurrent")
    if (a.enhancementChargesMax != b.enhancementChargesMax) add("enhancementChargesMax")
    if (a.mechanicalEffects != b.mechanicalEffects) add("mechanicalEffects")
    if (a.dataVersion != b.dataVersion) add("dataVersion")
}

private fun patchInventoryItem(target: InventoryItem, source: InventoryItem, fields: Set<String>): InventoryItem = target.copy(
    id = if ("id" in fields) source.id else target.id,
    state = if ("state" in fields) source.state else target.state,
    name = if ("name" in fields) source.name else target.name,
    load = if ("load" in fields) source.load else target.load,
    backpackCapacity = if ("backpackCapacity" in fields) source.backpackCapacity else target.backpackCapacity,
    durabilityCurrent = if ("durabilityCurrent" in fields) source.durabilityCurrent else target.durabilityCurrent,
    durabilityMax = if ("durabilityMax" in fields) source.durabilityMax else target.durabilityMax,
    itemCondition = if ("itemCondition" in fields) source.itemCondition else target.itemCondition,
    region = if ("region" in fields) source.region else target.region,
    effect = if ("effect" in fields) source.effect else target.effect,
    pg = if ("pg" in fields) source.pg else target.pg,
    pl = if ("pl" in fields) source.pl else target.pl,
    category = if ("category" in fields) source.category else target.category,
    agilityLimit = if ("agilityLimit" in fields) source.agilityLimit else target.agilityLimit,
    quality = if ("quality" in fields) source.quality else target.quality,
    quantity = if ("quantity" in fields) source.quantity else target.quantity,
    linkedAshId = if ("linkedAshId" in fields) source.linkedAshId else target.linkedAshId,
    ashPurity = if ("ashPurity" in fields) source.ashPurity else target.ashPurity,
    acquisitionSource = if ("acquisitionSource" in fields) source.acquisitionSource else target.acquisitionSource,
    heritageCost = if ("heritageCost" in fields) source.heritageCost else target.heritageCost,
    purchasePrice = if ("purchasePrice" in fields) source.purchasePrice else target.purchasePrice,
    catalogEntryId = if ("catalogEntryId" in fields) source.catalogEntryId else target.catalogEntryId,
    catalogVersion = if ("catalogVersion" in fields) source.catalogVersion else target.catalogVersion,
    acquiredAt = if ("acquiredAt" in fields) source.acquiredAt else target.acquiredAt,
    canonical = if ("canonical" in fields) source.canonical else target.canonical,
    baseId = if ("baseId" in fields) source.baseId else target.baseId,
    materialId = if ("materialId" in fields) source.materialId else target.materialId,
    secondaryMaterialId = if ("secondaryMaterialId" in fields) source.secondaryMaterialId else target.secondaryMaterialId,
    modificationIds = if ("modificationIds" in fields) source.modificationIds else target.modificationIds,
    gemIds = if ("gemIds" in fields) source.gemIds else target.gemIds,
    technologyIds = if ("technologyIds" in fields) source.technologyIds else target.technologyIds,
    gemSlots = if ("gemSlots" in fields) source.gemSlots else target.gemSlots,
    technologySlots = if ("technologySlots" in fields) source.technologySlots else target.technologySlots,
    installedEnhancements = if ("installedEnhancements" in fields) source.installedEnhancements else target.installedEnhancements,
    enhancementSlots = if ("enhancementSlots" in fields) source.enhancementSlots else target.enhancementSlots,
    enhancementChargesCurrent = if ("enhancementChargesCurrent" in fields) source.enhancementChargesCurrent else target.enhancementChargesCurrent,
    enhancementChargesMax = if ("enhancementChargesMax" in fields) source.enhancementChargesMax else target.enhancementChargesMax,
    mechanicalEffects = if ("mechanicalEffects" in fields) source.mechanicalEffects else target.mechanicalEffects,
    dataVersion = if ("dataVersion" in fields) source.dataVersion else target.dataVersion,
)

private fun changedItemStateFields(a: ItemState, b: ItemState): Set<String> = buildSet {
    if (a.id != b.id) add("id")
    if (a.definition != b.definition) add("definition")
    if (a.scope != b.scope) add("scope")
    if (a.material != b.material) add("material")
    if (a.installations != b.installations) add("installations")
    if (a.stack != b.stack) add("stack")
    if (a.consumable != b.consumable) add("consumable")
    if (a.ammo != b.ammo) add("ammo")
    if (a.reload != b.reload) add("reload")
    if (a.container != b.container) add("container")
}

private fun patchItemState(target: ItemState, source: ItemState, fields: Set<String>): ItemState = target.copy(
    id = if ("id" in fields) source.id else target.id,
    definition = if ("definition" in fields) source.definition else target.definition,
    scope = if ("scope" in fields) source.scope else target.scope,
    material = if ("material" in fields) source.material else target.material,
    installations = if ("installations" in fields) source.installations else target.installations,
    stack = if ("stack" in fields) source.stack else target.stack,
    consumable = if ("consumable" in fields) source.consumable else target.consumable,
    ammo = if ("ammo" in fields) source.ammo else target.ammo,
    reload = if ("reload" in fields) source.reload else target.reload,
    container = if ("container" in fields) source.container else target.container,
)

private fun changedConditionInstanceFields(a: ConditionInstance, b: ConditionInstance): Set<String> = buildSet {
    if (a.instanceId != b.instanceId) add("instanceId")
    if (a.payload != b.payload) add("payload")
    if (a.source != b.source) add("source")
    if (a.appliedAt != b.appliedAt) add("appliedAt")
}

private fun patchConditionInstance(target: ConditionInstance, source: ConditionInstance, fields: Set<String>): ConditionInstance = target.copy(
    instanceId = if ("instanceId" in fields) source.instanceId else target.instanceId,
    payload = if ("payload" in fields) source.payload else target.payload,
    source = if ("source" in fields) source.source else target.source,
    appliedAt = if ("appliedAt" in fields) source.appliedAt else target.appliedAt,
)

private fun changedConditionEffectFields(a: ConditionEffect, b: ConditionEffect): Set<String> = buildSet {
    if (a.id != b.id) add("id")
    if (a.name != b.name) add("name")
    if (a.intensity != b.intensity) add("intensity")
    if (a.duration != b.duration) add("duration")
    if (a.origin != b.origin) add("origin")
    if (a.summary != b.summary) add("summary")
}

private fun patchConditionEffect(target: ConditionEffect, source: ConditionEffect, fields: Set<String>): ConditionEffect = target.copy(
    id = if ("id" in fields) source.id else target.id,
    name = if ("name" in fields) source.name else target.name,
    intensity = if ("intensity" in fields) source.intensity else target.intensity,
    duration = if ("duration" in fields) source.duration else target.duration,
    origin = if ("origin" in fields) source.origin else target.origin,
    summary = if ("summary" in fields) source.summary else target.summary,
)

private fun changedAbilityFields(a: Ability, b: Ability): Set<String> = buildSet {
    if (a.id != b.id) add("id")
    if (a.name != b.name) add("name")
    if (a.kind != b.kind) add("kind")
    if (a.source != b.source) add("source")
    if (a.execution != b.execution) add("execution")
    if (a.range != b.range) add("range")
    if (a.duration != b.duration) add("duration")
    if (a.resistance != b.resistance) add("resistance")
    if (a.cost != b.cost) add("cost")
    if (a.effect != b.effect) add("effect")
    if (a.mechanicalEffect != b.mechanicalEffect) add("mechanicalEffect")
    if (a.definition != b.definition) add("definition")
    if (a.spellData != b.spellData) add("spellData")
    if (a.runeData != b.runeData) add("runeData")
    if (a.ashData != b.ashData) add("ashData")
    if (a.powerData != b.powerData) add("powerData")
    if (a.favorite != b.favorite) add("favorite")
    if (a.available != b.available) add("available")
    if (a.revision != b.revision) add("revision")
}

private fun patchAbility(target: Ability, source: Ability, fields: Set<String>): Ability = target.copy(
    id = if ("id" in fields) source.id else target.id,
    name = if ("name" in fields) source.name else target.name,
    kind = if ("kind" in fields) source.kind else target.kind,
    source = if ("source" in fields) source.source else target.source,
    execution = if ("execution" in fields) source.execution else target.execution,
    range = if ("range" in fields) source.range else target.range,
    duration = if ("duration" in fields) source.duration else target.duration,
    resistance = if ("resistance" in fields) source.resistance else target.resistance,
    cost = if ("cost" in fields) source.cost else target.cost,
    effect = if ("effect" in fields) source.effect else target.effect,
    mechanicalEffect = if ("mechanicalEffect" in fields) source.mechanicalEffect else target.mechanicalEffect,
    definition = if ("definition" in fields) source.definition else target.definition,
    spellData = if ("spellData" in fields) source.spellData else target.spellData,
    runeData = if ("runeData" in fields) source.runeData else target.runeData,
    ashData = if ("ashData" in fields) source.ashData else target.ashData,
    powerData = if ("powerData" in fields) source.powerData else target.powerData,
    favorite = if ("favorite" in fields) source.favorite else target.favorite,
    available = if ("available" in fields) source.available else target.available,
    revision = if ("revision" in fields) source.revision else target.revision,
)

private fun changedPowerFields(a: Power, b: Power): Set<String> = buildSet {
    if (a.id != b.id) add("id")
    if (a.name != b.name) add("name")
    if (a.origin != b.origin) add("origin")
    if (a.cost != b.cost) add("cost")
    if (a.action != b.action) add("action")
    if (a.range != b.range) add("range")
    if (a.duration != b.duration) add("duration")
    if (a.limit != b.limit) add("limit")
    if (a.effect != b.effect) add("effect")
    if (a.category != b.category) add("category")
    if (a.prerequisites != b.prerequisites) add("prerequisites")
    if (a.activationCondition != b.activationCondition) add("activationCondition")
    if (a.enhancements != b.enhancements) add("enhancements")
    if (a.deactivationCondition != b.deactivationCondition) add("deactivationCondition")
    if (a.ruleReference != b.ruleReference) add("ruleReference")
    if (a.sourceType != b.sourceType) add("sourceType")
    if (a.sourceId != b.sourceId) add("sourceId")
    if (a.catalogEntryId != b.catalogEntryId) add("catalogEntryId")
    if (a.catalogVersion != b.catalogVersion) add("catalogVersion")
    if (a.favorite != b.favorite) add("favorite")
    if (a.available != b.available) add("available")
    if (a.canonicalSource != b.canonicalSource) add("canonicalSource")
    if (a.knowledgeId != b.knowledgeId) add("knowledgeId")
    if (a.knowledgeLevel != b.knowledgeLevel) add("knowledgeLevel")
    if (a.costType != b.costType) add("costType")
    if (a.costValue != b.costValue) add("costValue")
    if (a.destinyCostEligible != b.destinyCostEligible) add("destinyCostEligible")
    if (a.executionType != b.executionType) add("executionType")
    if (a.rangeType != b.rangeType) add("rangeType")
    if (a.targetArea != b.targetArea) add("targetArea")
    if (a.durationType != b.durationType) add("durationType")
    if (a.durationValue != b.durationValue) add("durationValue")
    if (a.durationUnit != b.durationUnit) add("durationUnit")
    if (a.resistance != b.resistance) add("resistance")
    if (a.timeValue != b.timeValue) add("timeValue")
    if (a.timeUnit != b.timeUnit) add("timeUnit")
    if (a.grantsPermanentBonus != b.grantsPermanentBonus) add("grantsPermanentBonus")
    if (a.modifiers != b.modifiers) add("modifiers")
    if (a.active != b.active) add("active")
    if (a.linkedItemId != b.linkedItemId) add("linkedItemId")
    if (a.revision != b.revision) add("revision")
}

private fun patchPower(target: Power, source: Power, fields: Set<String>): Power = target.copy(
    id = if ("id" in fields) source.id else target.id,
    name = if ("name" in fields) source.name else target.name,
    origin = if ("origin" in fields) source.origin else target.origin,
    cost = if ("cost" in fields) source.cost else target.cost,
    action = if ("action" in fields) source.action else target.action,
    range = if ("range" in fields) source.range else target.range,
    duration = if ("duration" in fields) source.duration else target.duration,
    limit = if ("limit" in fields) source.limit else target.limit,
    effect = if ("effect" in fields) source.effect else target.effect,
    category = if ("category" in fields) source.category else target.category,
    prerequisites = if ("prerequisites" in fields) source.prerequisites else target.prerequisites,
    activationCondition = if ("activationCondition" in fields) source.activationCondition else target.activationCondition,
    enhancements = if ("enhancements" in fields) source.enhancements else target.enhancements,
    deactivationCondition = if ("deactivationCondition" in fields) source.deactivationCondition else target.deactivationCondition,
    ruleReference = if ("ruleReference" in fields) source.ruleReference else target.ruleReference,
    sourceType = if ("sourceType" in fields) source.sourceType else target.sourceType,
    sourceId = if ("sourceId" in fields) source.sourceId else target.sourceId,
    catalogEntryId = if ("catalogEntryId" in fields) source.catalogEntryId else target.catalogEntryId,
    catalogVersion = if ("catalogVersion" in fields) source.catalogVersion else target.catalogVersion,
    favorite = if ("favorite" in fields) source.favorite else target.favorite,
    available = if ("available" in fields) source.available else target.available,
    canonicalSource = if ("canonicalSource" in fields) source.canonicalSource else target.canonicalSource,
    knowledgeId = if ("knowledgeId" in fields) source.knowledgeId else target.knowledgeId,
    knowledgeLevel = if ("knowledgeLevel" in fields) source.knowledgeLevel else target.knowledgeLevel,
    costType = if ("costType" in fields) source.costType else target.costType,
    costValue = if ("costValue" in fields) source.costValue else target.costValue,
    destinyCostEligible = if ("destinyCostEligible" in fields) source.destinyCostEligible else target.destinyCostEligible,
    executionType = if ("executionType" in fields) source.executionType else target.executionType,
    rangeType = if ("rangeType" in fields) source.rangeType else target.rangeType,
    targetArea = if ("targetArea" in fields) source.targetArea else target.targetArea,
    durationType = if ("durationType" in fields) source.durationType else target.durationType,
    durationValue = if ("durationValue" in fields) source.durationValue else target.durationValue,
    durationUnit = if ("durationUnit" in fields) source.durationUnit else target.durationUnit,
    resistance = if ("resistance" in fields) source.resistance else target.resistance,
    timeValue = if ("timeValue" in fields) source.timeValue else target.timeValue,
    timeUnit = if ("timeUnit" in fields) source.timeUnit else target.timeUnit,
    grantsPermanentBonus = if ("grantsPermanentBonus" in fields) source.grantsPermanentBonus else target.grantsPermanentBonus,
    modifiers = if ("modifiers" in fields) source.modifiers else target.modifiers,
    active = if ("active" in fields) source.active else target.active,
    linkedItemId = if ("linkedItemId" in fields) source.linkedItemId else target.linkedItemId,
    revision = if ("revision" in fields) source.revision else target.revision,
)

private fun changedMysticAbilityFields(a: MysticAbility, b: MysticAbility): Set<String> = buildSet {
    if (a.id != b.id) add("id")
    if (a.type != b.type) add("type")
    if (a.name != b.name) add("name")
    if (a.cost != b.cost) add("cost")
    if (a.action != b.action) add("action")
    if (a.range != b.range) add("range")
    if (a.duration != b.duration) add("duration")
    if (a.effect != b.effect) add("effect")
    if (a.favorite != b.favorite) add("favorite")
    if (a.available != b.available) add("available")
    if (a.category != b.category) add("category")
    if (a.source != b.source) add("source")
    if (a.ruleReference != b.ruleReference) add("ruleReference")
    if (a.catalogEntryId != b.catalogEntryId) add("catalogEntryId")
    if (a.catalogVersion != b.catalogVersion) add("catalogVersion")
    if (a.canonicalSource != b.canonicalSource) add("canonicalSource")
    if (a.knowledgeId != b.knowledgeId) add("knowledgeId")
    if (a.knowledgeLevel != b.knowledgeLevel) add("knowledgeLevel")
    if (a.costType != b.costType) add("costType")
    if (a.costValue != b.costValue) add("costValue")
    if (a.executionType != b.executionType) add("executionType")
    if (a.rangeType != b.rangeType) add("rangeType")
    if (a.targetArea != b.targetArea) add("targetArea")
    if (a.durationType != b.durationType) add("durationType")
    if (a.durationValue != b.durationValue) add("durationValue")
    if (a.durationUnit != b.durationUnit) add("durationUnit")
    if (a.resistance != b.resistance) add("resistance")
    if (a.timeValue != b.timeValue) add("timeValue")
    if (a.timeUnit != b.timeUnit) add("timeUnit")
    if (a.ashSource != b.ashSource) add("ashSource")
    if (a.ashPurity != b.ashPurity) add("ashPurity")
    if (a.linkedInventoryItemId != b.linkedInventoryItemId) add("linkedInventoryItemId")
    if (a.inscriberId != b.inscriberId) add("inscriberId")
    if (a.inscriberPower != b.inscriberPower) add("inscriberPower")
    if (a.inscriberRunicKnowledge != b.inscriberRunicKnowledge) add("inscriberRunicKnowledge")
    if (a.revision != b.revision) add("revision")
}

private fun patchMysticAbility(target: MysticAbility, source: MysticAbility, fields: Set<String>): MysticAbility = target.copy(
    id = if ("id" in fields) source.id else target.id,
    type = if ("type" in fields) source.type else target.type,
    name = if ("name" in fields) source.name else target.name,
    cost = if ("cost" in fields) source.cost else target.cost,
    action = if ("action" in fields) source.action else target.action,
    range = if ("range" in fields) source.range else target.range,
    duration = if ("duration" in fields) source.duration else target.duration,
    effect = if ("effect" in fields) source.effect else target.effect,
    favorite = if ("favorite" in fields) source.favorite else target.favorite,
    available = if ("available" in fields) source.available else target.available,
    category = if ("category" in fields) source.category else target.category,
    source = if ("source" in fields) source.source else target.source,
    ruleReference = if ("ruleReference" in fields) source.ruleReference else target.ruleReference,
    catalogEntryId = if ("catalogEntryId" in fields) source.catalogEntryId else target.catalogEntryId,
    catalogVersion = if ("catalogVersion" in fields) source.catalogVersion else target.catalogVersion,
    canonicalSource = if ("canonicalSource" in fields) source.canonicalSource else target.canonicalSource,
    knowledgeId = if ("knowledgeId" in fields) source.knowledgeId else target.knowledgeId,
    knowledgeLevel = if ("knowledgeLevel" in fields) source.knowledgeLevel else target.knowledgeLevel,
    costType = if ("costType" in fields) source.costType else target.costType,
    costValue = if ("costValue" in fields) source.costValue else target.costValue,
    executionType = if ("executionType" in fields) source.executionType else target.executionType,
    rangeType = if ("rangeType" in fields) source.rangeType else target.rangeType,
    targetArea = if ("targetArea" in fields) source.targetArea else target.targetArea,
    durationType = if ("durationType" in fields) source.durationType else target.durationType,
    durationValue = if ("durationValue" in fields) source.durationValue else target.durationValue,
    durationUnit = if ("durationUnit" in fields) source.durationUnit else target.durationUnit,
    resistance = if ("resistance" in fields) source.resistance else target.resistance,
    timeValue = if ("timeValue" in fields) source.timeValue else target.timeValue,
    timeUnit = if ("timeUnit" in fields) source.timeUnit else target.timeUnit,
    ashSource = if ("ashSource" in fields) source.ashSource else target.ashSource,
    ashPurity = if ("ashPurity" in fields) source.ashPurity else target.ashPurity,
    linkedInventoryItemId = if ("linkedInventoryItemId" in fields) source.linkedInventoryItemId else target.linkedInventoryItemId,
    inscriberId = if ("inscriberId" in fields) source.inscriberId else target.inscriberId,
    inscriberPower = if ("inscriberPower" in fields) source.inscriberPower else target.inscriberPower,
    inscriberRunicKnowledge = if ("inscriberRunicKnowledge" in fields) source.inscriberRunicKnowledge else target.inscriberRunicKnowledge,
    revision = if ("revision" in fields) source.revision else target.revision,
)

private fun changedPersonalNoteFields(a: PersonalNote, b: PersonalNote): Set<String> = buildSet {
    if (a.id != b.id) add("id")
    if (a.title != b.title) add("title")
    if (a.text != b.text) add("text")
}

private fun patchPersonalNote(target: PersonalNote, source: PersonalNote, fields: Set<String>): PersonalNote = target.copy(
    id = if ("id" in fields) source.id else target.id,
    title = if ("title" in fields) source.title else target.title,
    text = if ("text" in fields) source.text else target.text,
)

private fun changedAttributeValueFields(a: AttributeValue, b: AttributeValue): Set<String> = buildSet {
    if (a.name != b.name) add("name")
    if (a.acronym != b.acronym) add("acronym")
    if (a.value != b.value) add("value")
    if (a.modifier != b.modifier) add("modifier")
    addAll(elementChanges("skills", a.skills, b.skills, { it.name }, ::changedSkillFields))
}

private fun patchAttributeValue(target: AttributeValue, source: AttributeValue, fields: Set<String>, metadata: Map<String, Long>): AttributeValue = target.copy(
    name = if ("name" in fields) source.name else target.name,
    acronym = if ("acronym" in fields) source.acronym else target.acronym,
    value = if ("value" in fields) source.value else target.value,
    modifier = if ("modifier" in fields) source.modifier else target.modifier,
    skills = patchElements("skills", target.skills, source.skills, fields, metadata, { it.name }, ::patchSkill),
)

/** Legacy dirty records have no intent markers; prefer only their previously selected groups. */
fun pendingCharacterEditKeys(local: Character, remote: Character): Set<String> {
    val pending = local.pendingSyncKeys()
    if (pending.isNotEmpty() || !local.dirty) return pending
    if (needsLegacySyncReview(local, remote)) {
        throw DomainError.RevisionConflict("Há alterações locais de uma versão anterior. Sincronize e revise a ficha antes de executar esta ação.")
    }
    val conflict = CharacterSyncConflict(local, remote, remote.updatedAt)
    val localGroups = conflict.fields.mapTo(mutableSetOf()) { it.id } - automaticRemoteFieldIds(conflict)
    return characterEditKeys(remote, local).filterTo(mutableSetOf()) {
        val group = it.substringBefore('/')
        val selected = group in localGroups || group in setOf("powers", "mysticAbilities") && "abilities" in localGroups ||
            group == "conditions" && "conditionInstances" in localGroups
        selected && (it.count { c -> c == '/' } != 1 || local.containsSyncElement(it) || "@deleted/$it" in local.fieldUpdatedAt)
    }
}

private fun changedSkillFields(a: SkillValue, b: SkillValue): Set<String> = buildSet {
    if (a.value != b.value) add("value")
    if (a.modifier != b.modifier) add("modifier")
}

private fun patchSkill(target: SkillValue, source: SkillValue, fields: Set<String>): SkillValue = target.copy(
    value = if ("value" in fields) source.value else target.value,
    modifier = if ("modifier" in fields) source.modifier else target.modifier,
)

/**
 * Resolve pending edit keys against a committed remote sheet.
 * Independent fields and independent properties of the same list element are always preserved.
 * Same-field collisions use latest-timestamp LWW with ties favoring the committed remote value,
 * and remote element deletions (tombstones) are never resurrected by stale property edits.
 */
fun resolveConcurrentEditKeys(
    local: Character,
    remote: Character,
    keys: Set<String>,
): Set<String> {
    if (keys.isEmpty()) return emptySet()
    val baseRevision = local.fieldUpdatedAt[BASE_REVISION] ?: local.serverSyncRevision()
    val remoteRevision = remote.serverSyncRevision()
    val hasNewerRemoteRevision = remoteRevision > baseRevision
    val hasConcurrentLegacyEdit = !hasNewerRemoteRevision &&
        remote.updatedAt > local.lastSyncedAt &&
        remote.lastEditedBy.isNotBlank() &&
        remote.lastEditedBy != local.lastEditedBy

    if (!hasNewerRemoteRevision && !hasConcurrentLegacyEdit) return keys

    if (remote.fieldUpdatedAt[PROTOCOL_VERSION] != 1L || !hasNewerRemoteRevision) {
        val conflict = CharacterSyncConflict(local, remote, remote.updatedAt)
        val remoteWins = automaticRemoteFieldIds(conflict)
        return keys.filterNotTo(mutableSetOf()) { key ->
            val group = key.substringBefore('/')
            group in remoteWins ||
                (group in setOf("powers", "mysticAbilities") && "abilities" in remoteWins) ||
                (group == "conditions" && "conditionInstances" in remoteWins)
        }
    }

    fun groupFor(key: String): String = when (val group = key.substringBefore('/')) {
        "powers", "mysticAbilities" -> "abilities"
        "conditions" -> "conditionInstances"
        else -> group
    }

    fun ancestorPaths(key: String): List<String> = buildList {
        var idx = key.indexOf('/')
        while (idx >= 0) {
            add(key.substring(0, idx))
            idx = key.indexOf('/', idx + 1)
        }
    }

    fun isDeletedByRemote(key: String): Boolean {
        if ((remote.fieldUpdatedAt["$DELETED_PREFIX$key"] ?: 0L) > baseRevision) return true
        return ancestorPaths(key).any { (remote.fieldUpdatedAt["$DELETED_PREFIX$it"] ?: 0L) > baseRevision }
    }

    fun isTouchedByRemote(key: String): Boolean {
        if ((remote.fieldUpdatedAt["@server/$key"] ?: 0L) > baseRevision) return true
        if ((remote.fieldUpdatedAt["$DELETED_PREFIX$key"] ?: 0L) > baseRevision) return true
        if (ancestorPaths(key).any {
                (remote.fieldUpdatedAt["@server/$it"] ?: 0L) > baseRevision ||
                    (remote.fieldUpdatedAt["$DELETED_PREFIX$it"] ?: 0L) > baseRevision
            }
        ) return true
        val childServerPrefix = "@server/$key/"
        val childDeletedPrefix = "$DELETED_PREFIX$key/"
        return remote.fieldUpdatedAt.any { (metaKey, rev) ->
            rev > baseRevision && (metaKey.startsWith(childServerPrefix) || metaKey.startsWith(childDeletedPrefix))
        }
    }

    fun fieldTime(character: Character, group: String, fallback: Long): Long {
        val lookup = if (group == "inventory" || group == "itemStates") listOf("inventory", "itemStates") else listOf(group)
        val hasGroupTimestamps = character.fieldUpdatedAt.keys.any { !it.startsWith("@") }
        return lookup.maxOf { id ->
            character.fieldUpdatedAt[id] ?: if (!hasGroupTimestamps) fallback else 0L
        }
    }

    return keys.filterTo(mutableSetOf()) { key ->
        if (!isTouchedByRemote(key)) return@filterTo true
        if (isDeletedByRemote(key) && '/' in key) return@filterTo false
        val group = groupFor(key)
        val localPendingAt = local.fieldUpdatedAt["$PENDING_PREFIX$key"]?.takeIf { it > 1L }
        val localTime = localPendingAt ?: fieldTime(local, group, local.updatedAt)
        val remoteTime = fieldTime(remote, group, remote.updatedAt)
        localTime > remoteTime
    }
}

/**
 * Splits targets into bounded batches matching Spark's Rules access budget
 * (at most 5 targets when any ITEM_FIELD is present, otherwise at most 7).
 */
fun chunkSparkTargets(
    targets: List<SparkTargetSpec>,
    maxTargets: Int = MAX_SPARK_TARGETS_PER_OPERATION,
): List<List<SparkTargetSpec>> {
    require(maxTargets in 1..MAX_SPARK_TARGETS_PER_OPERATION) {
        "Spark protocol allows 1..$MAX_SPARK_TARGETS_PER_OPERATION targets per operation."
    }
    if (targets.isEmpty()) return emptyList()
    val result = mutableListOf<List<SparkTargetSpec>>()
    var current = mutableListOf<SparkTargetSpec>()
    for (target in targets) {
        val candidate = current + target
        if (candidate.size > maxTargets || !isWithinSparkTransactionBudget(candidate)) {
            if (current.isNotEmpty()) {
                result += current.toList()
                current = mutableListOf()
            }
        }
        current += target
    }
    if (current.isNotEmpty()) result += current.toList()
    return result
}

/**
 * Transitions a recoverable [SheetIntentStatus.ACCESS_DENIED] or [SheetIntentStatus.DRAFT] intent
 * back to [SheetIntentStatus.PENDING] without silently altering its original [SheetIntent.baseRevision].
 */
fun SheetIntent.retryAsPending(now: Long = updatedAt): SheetIntent {
    require(status == SheetIntentStatus.ACCESS_DENIED || status == SheetIntentStatus.DRAFT) {
        "Somente rascunhos ou intenções sem acesso podem ser reenviados diretamente."
    }
    return copy(status = SheetIntentStatus.PENDING, error = "", updatedAt = now)
}
