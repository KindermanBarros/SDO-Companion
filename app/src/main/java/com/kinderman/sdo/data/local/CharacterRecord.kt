package com.kinderman.sdo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.kinderman.sdo.domain.model.AttributeValue
import com.kinderman.sdo.domain.model.BodyRegion
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterLock
import com.kinderman.sdo.domain.model.CharacterCreationStatus
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.CURRENT_CHARACTER_SCHEMA_VERSION
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemCreationDraft
import com.kinderman.sdo.domain.model.MysticAbility
import com.kinderman.sdo.domain.model.OrganStatus
import com.kinderman.sdo.domain.model.PersonalNote
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.ProgressionRecord
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.NeedsReview
import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.ActiveModifier
import com.kinderman.sdo.domain.model.BodyState
import com.kinderman.sdo.domain.model.ConditionInstance
import com.kinderman.sdo.domain.model.defaultAttributes
import com.kinderman.sdo.domain.model.defaultBodyRegions
import com.kinderman.sdo.domain.model.defaultProtectionAdjustments
import com.kinderman.sdo.domain.model.defaultProtections
import com.kinderman.sdo.domain.model.canonicalized
import com.kinderman.sdo.domain.model.allCanonicalAbilitiesSafely
import com.kinderman.sdo.domain.model.canonicalBodyState
import com.kinderman.sdo.domain.model.toCanonicalAbility
import com.kinderman.sdo.domain.model.toCanonicalInstance
import com.kinderman.sdo.domain.model.toCanonicalState
import com.kinderman.sdo.domain.model.toLegacyRegion
import com.kinderman.sdo.domain.model.toLegacyStatus
import com.kinderman.sdo.domain.model.toLegacyEffect
import com.kinderman.sdo.domain.model.normalizeBodyRegions
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.domain.model.synchronizeItemPowers
import com.kinderman.sdo.domain.catalog.withRefreshedPresetPowers
import com.kinderman.sdo.domain.catalog.withMigratedCreationRules
import com.kinderman.sdo.domain.catalog.withNormalizedInventory

private val canonicalConverters = CharacterConverters()

private fun Character.abilitiesForPersistence(): List<Ability> {
    val projected = allCanonicalAbilitiesSafely()
    return (projected + abilities).groupBy(Ability::id).values.map { versions ->
        versions.maxBy(Ability::revision)
    }
}

private fun BodyRegion.canonicalSignature(): List<String> = listOf(
    roll.toString(), name, failures.toString(), localProtection.toString(), generalProtection.toString(),
    equippedItemIds.joinToString(","), state.name, implantInstanceIds.joinToString(","), prosthesisInstanceId,
)

private fun OrganStatus.canonicalSignature(): List<String> = listOf(
    id, name, failures.toString(), slot.name, state.name, implantInstanceId,
)

private fun Character.bodyStateForPersistence(): BodyState? {
    val saved = bodyState
    val untouchedLegacyBody = normalizeBodyRegions(bodyRegions).map { it.canonicalSignature() } ==
        normalizeBodyRegions(defaultBodyRegions()).map { it.canonicalSignature() } && organs.isEmpty()
    if (saved != null && untouchedLegacyBody) return saved
    if (saved != null && saved.regions.map { it.toLegacyRegion().canonicalSignature() } == normalizeBodyRegions(bodyRegions).map { it.canonicalSignature() } &&
        saved.organs.map { it.toLegacyStatus().canonicalSignature() } == organs.map { it.canonicalSignature() }) return saved
    return runCatching {
        val savedRegions = saved?.regions.orEmpty().associateBy { it.region }
        val savedOrgans = saved?.organs.orEmpty().associateBy { it.organ }
        BodyState(
            regions = normalizeBodyRegions(bodyRegions).map { legacy ->
                val typed = legacy.toCanonicalState()
                typed.copy(injuries = savedRegions[typed.region]?.injuries.orEmpty())
            },
            organs = organs.map { legacy ->
                val typed = legacy.toCanonicalState()
                typed.copy(
                    injuries = savedOrgans[typed.organ]?.injuries.orEmpty(),
                    effectNotes = savedOrgans[typed.organ]?.effectNotes.orEmpty(),
                )
            },
        )
    }.getOrNull() ?: saved
}

private fun Character.conditionsForPersistence(): List<ConditionInstance> {
    val projected = conditions.mapNotNull { runCatching { it.toCanonicalInstance() }.getOrNull() }
    if (conditions.isEmpty()) return conditionInstances
    val unchanged = conditionInstances.isNotEmpty() && conditionInstances.map { instance ->
        listOf(instance.instanceId.value, instance.name, instance.intensity?.toString().orEmpty(), instance.duration?.kind?.name.orEmpty())
    } == projected.map { instance ->
        listOf(instance.instanceId.value, instance.name, instance.intensity?.toString().orEmpty(), instance.duration?.kind?.name.orEmpty())
    }
    return if (unchanged) conditionInstances else projected
}

@IgnoreExtraProperties
@Entity(tableName = "characters")
data class CharacterRecord(
    @PrimaryKey val id: String = "",
    val ownerId: String = "",
    val campaignId: String = "",
    val name: String = "Novo personagem",
    val race: String = "",
    val subRace: String = "",
    val raceAttribute: String = "",
    val occupation: String = "",
    val age: String = "",
    val level: Int = 1,
    val creationStatus: String = CharacterCreationStatus.COMPLETED.name,
    val creationStep: Int = 1,
    val creationCompletedAt: Long? = null,
    val creationRulesVersion: Int = 1,
    val itemSchemaVersion: Int = 0,
    val canonicalSchemaVersion: Int = 0,
    val migrationReviewPayload: String = "",
    val canonicalAbilitiesPayload: String = "",
    val canonicalBodyPayload: String = "",
    val canonicalConditionsPayload: String = "",
    val activeModifiersPayload: String = "",
    val canonicalItemsPayload: String = "",
    val scopedItemCatalogPayload: String = "",
    val canonicalProgressionPayload: String = "",
    val progressionLifeBonus: Int = 0,
    val progressionSanityBonus: Int = 0,
    val progressionArcaneBonus: Int = 0,
    val progressionEnergyBonus: Int = 0,
    val progressionHistory: List<ProgressionRecord> = emptyList(),
    val money: Int = 0,
    val life: ResourceValue = ResourceValue(),
    val sanity: ResourceValue = ResourceValue(),
    val arcane: ResourceValue = ResourceValue(),
    val energy: ResourceValue = ResourceValue(),
    val destiny: ResourceValue = ResourceValue(5, 5),
    val exhaustion: ResourceValue = ResourceValue(0, 10),
    val corruption: ResourceValue = ResourceValue(0, 100),
    val attributes: List<AttributeValue> = defaultAttributes(),
    val protections: Map<String, Int> = defaultProtections(),
    val protectionAdjustments: Map<String, Int> = emptyMap(),
    val positiveTraits: List<String> = emptyList(),
    val negativeTraits: List<String> = emptyList(),
    val pathName: String = "",
    val pathMotto: String = "",
    val powers: List<Power> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
    val itemCreationDraft: ItemCreationDraft? = null,
    val story: String = "",
    val notes: String = "",
    @ColumnInfo(defaultValue = "''")
    val transports: List<com.kinderman.sdo.domain.model.CharacterTransport> = emptyList(),
    @ColumnInfo(defaultValue = "''")
    val familiars: List<com.kinderman.sdo.domain.model.CharacterFamiliar> = emptyList(),
    val personalNotes: List<PersonalNote> = emptyList(),
    val updatedAt: Long = 0,
    val lastEditedBy: String = "",
    @ColumnInfo(defaultValue = "'{}'")
    val fieldUpdatedAt: Map<String, Long> = emptyMap(),
    @get:Exclude
    @field:Exclude
    val dirty: Boolean = false,
    @get:Exclude
    @field:Exclude
    val lastSyncedAt: Long = 0,
    val height: String = "",
    val sex: String = "",
    val size: String = "",
    @ColumnInfo(defaultValue = "''")
    val photoUri: String = "",
    val learnedKnowledges: List<SpecialKnowledge> = emptyList(),
    val arcaneKnowledges: List<SpecialKnowledge> = emptyList(),
    val battleTechniques: List<SpecialKnowledge> = emptyList(),
    val pathKeywords: List<String> = emptyList(),
    val pathPillars: List<String> = emptyList(),
    val containerCapacity: Int = 0,
    val bodyRegions: List<BodyRegion> = emptyList(),
    val agilityLimit: String = "",
    val organs: List<OrganStatus> = emptyList(),
    val mysticAbilities: List<MysticAbility> = emptyList(),
    val conditions: List<ConditionEffect> = emptyList(),
    @get:PropertyName("isLocked")
    @field:PropertyName("isLocked")
    val isLocked: Boolean = false,
    val lockType: String = "NONE",
    val lockedBy: String = "",
    val lockedAt: Long? = null,
    val deleted: Boolean = false,
    val appliedDeliveryIds: List<String> = emptyList(),
)

fun CharacterRecord.toDomain(): Character {
    val decodedAbilities = canonicalAbilitiesPayload
        .takeIf { it.isNotBlank() && canonicalConverters.isCanonicalAbilitiesPayloadValid(it) }
        ?.let(canonicalConverters::stringToCanonicalAbilities)
    val decodedBody = canonicalBodyPayload
        .takeIf { it.isNotBlank() && canonicalConverters.isCanonicalBodyPayloadValid(it) }
        ?.let(canonicalConverters::stringToCanonicalBody)
    val decodedConditions = canonicalConditionsPayload
        .takeIf { it.isNotBlank() && canonicalConverters.isCanonicalConditionsPayloadValid(it) }
        ?.let(canonicalConverters::stringToCanonicalConditions)
    val decodedModifiers = activeModifiersPayload
        .takeIf(canonicalConverters::isActiveModifiersPayloadValid)
        ?.let(canonicalConverters::stringToActiveModifiers)
        .orEmpty()
    val decodedItems = canonicalItemsPayload
        .takeIf(canonicalConverters::isCanonicalItemsPayloadValid)
        ?.let(canonicalConverters::stringToCanonicalItems)
        .orEmpty()
    val decodedItemCatalog = scopedItemCatalogPayload
        .takeIf(canonicalConverters::isScopedItemCatalogPayloadValid)
        ?.let(canonicalConverters::stringToScopedItemCatalog)
        .orEmpty()
    val decodedProgression = canonicalProgressionPayload
        .takeIf(canonicalConverters::isProgressionPayloadValid)
        ?.let(canonicalConverters::stringToCanonicalProgression)
        ?: com.kinderman.sdo.domain.model.CharacterProgression()

    return Character(
    id = id,
    ownerId = ownerId,
    campaignId = normalizeCampaignId(campaignId),
    name = name,
    race = race,
    subRace = subRace,
    raceAttribute = raceAttribute,
    occupation = occupation,
    height = height,
    age = age,
    sex = sex,
    size = size,
    photoUri = photoUri,
    level = level.coerceIn(1, com.kinderman.sdo.domain.progression.LevelProgression.MAX_LEVEL),
    creationStatus = runCatching { CharacterCreationStatus.valueOf(creationStatus) }.getOrDefault(CharacterCreationStatus.COMPLETED),
    creationStep = creationStep.coerceIn(1, com.kinderman.sdo.domain.creation.CharacterCreation.STEP_COUNT),
    creationCompletedAt = creationCompletedAt,
    creationRulesVersion = creationRulesVersion,
    itemSchemaVersion = com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION,
    canonicalSchemaVersion = canonicalSchemaVersion,
    abilities = decodedAbilities
        ?: (powers.mapNotNull { runCatching { it.toCanonicalAbility() }.getOrNull() } + mysticAbilities.mapNotNull { runCatching { it.toCanonicalAbility() }.getOrNull() }),
    bodyState = decodedBody
        ?: runCatching { Character(bodyRegions = normalizeBodyRegions(bodyRegions), organs = organs).canonicalBodyState() }.getOrNull(),
    conditionInstances = decodedConditions
        ?: conditions.mapNotNull { runCatching { it.toCanonicalInstance() }.getOrNull() },
    activeModifiers = decodedModifiers,
    itemStates = decodedItems,
    customItemCatalog = decodedItemCatalog,
    progression = decodedProgression,
    migrationReviews = runCatching { MigrationReviewCodec.decode(migrationReviewPayload) }.getOrDefault(emptyList()),
    progressionLifeBonus = progressionLifeBonus,
    progressionSanityBonus = progressionSanityBonus,
    progressionArcaneBonus = progressionArcaneBonus,
    progressionEnergyBonus = progressionEnergyBonus,
    progressionHistory = progressionHistory,
    money = money,
    life = life,
    sanity = sanity,
    arcane = arcane,
    energy = energy,
    destiny = destiny,
    exhaustion = exhaustion,
    corruption = corruption.copy(
        current = corruption.current.coerceIn(0, 100),
        maximum = 100,
    ),
    attributes = attributes.ifEmpty { defaultAttributes() },
    protections = protections.ifEmpty { defaultProtections() },
    protectionAdjustments = resolveProtectionAdjustments(
        attributes = attributes.ifEmpty { defaultAttributes() },
        storedProtections = protections.ifEmpty { defaultProtections() },
        storedAdjustments = protectionAdjustments,
    ),
    positiveTraits = positiveTraits.ifEmpty { listOf("") },
    negativeTraits = negativeTraits.ifEmpty { listOf("") },
    learnedKnowledges = learnedKnowledges,
    arcaneKnowledges = arcaneKnowledges,
    battleTechniques = battleTechniques,
    pathName = pathName,
    pathMotto = pathMotto,
    pathKeywords = pathKeywords.ifEmpty { listOf("", "", "") },
    pathPillars = pathPillars.ifEmpty { listOf("", "", "") },
    powers = powers.map(Power::canonicalized),
    inventory = inventory.map { item ->
        if (itemSchemaVersion < com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION) {
            item.copy(dataVersion = minOf(item.dataVersion, itemSchemaVersion))
        } else item
    },
    itemCreationDraft = itemCreationDraft,
    bodyRegions = (decodedBody?.regions?.map { it.toLegacyRegion() } ?: normalizeBodyRegions(bodyRegions)).map { region ->
        if (canonicalSchemaVersion >= 2) region else region.copy(state = when {
            region.failures >= 4 -> com.kinderman.sdo.domain.model.BodyIntegrity.Destroyed
            region.failures > 0 -> com.kinderman.sdo.domain.model.BodyIntegrity.Damaged
            else -> com.kinderman.sdo.domain.model.BodyIntegrity.Intact
        })
    },
    agilityLimit = agilityLimit,
    organs = (decodedBody?.organs?.map { it.toLegacyStatus() } ?: organs).map { organ ->
        if (canonicalSchemaVersion >= 2) organ else organ.copy(state = when {
            organ.failures >= 3 -> com.kinderman.sdo.domain.model.BodyIntegrity.Destroyed
            organ.failures > 0 -> com.kinderman.sdo.domain.model.BodyIntegrity.Damaged
            else -> com.kinderman.sdo.domain.model.BodyIntegrity.Intact
        })
    },
    mysticAbilities = mysticAbilities.map(MysticAbility::canonicalized),
    conditions = decodedConditions?.map { it.toLegacyEffect() } ?: conditions,
    story = story,
    notes = notes,
    transports = transports,
    familiars = familiars,
    personalNotes = personalNotes.ifEmpty {
        notes.takeIf(String::isNotBlank)?.let { legacyText ->
            listOf(PersonalNote(id = "legacy-$id", title = "Registro Pessoal 1", text = legacyText))
        }.orEmpty()
    },
    lockType = if (lockType == "NONE" && isLocked) {
        CharacterLock.HISTORIAN
    } else {
        runCatching { CharacterLock.valueOf(lockType) }.getOrDefault(CharacterLock.NONE)
    },
    lockedBy = lockedBy,
    lockedAt = lockedAt,
    updatedAt = updatedAt,
    lastEditedBy = lastEditedBy,
    fieldUpdatedAt = fieldUpdatedAt,
    dirty = dirty,
    lastSyncedAt = lastSyncedAt,
    deleted = deleted,
    appliedDeliveryIds = appliedDeliveryIds,
    ).withMigratedCreationRules().withRefreshedPresetPowers().withNormalizedInventory().synchronizeItemPowers()
}

fun Character.toRecord(): CharacterRecord {
    if (canonicalSchemaVersion != com.kinderman.sdo.domain.model.CURRENT_CHARACTER_SCHEMA_VERSION) {
        throw com.kinderman.sdo.domain.model.DomainError.LegacyWriteRejected()
    }
    val persistedBody = bodyStateForPersistence()
    val persistedConditions = conditionsForPersistence()
    val normalizedRegions = normalizeBodyRegions(bodyRegions)
    val defaultRegions = normalizeBodyRegions(defaultBodyRegions())
    val canStripBodyRegions = persistedBody != null && (
        normalizedRegions == defaultRegions ||
            persistedBody.regions.map { it.toLegacyRegion() } == normalizedRegions
        )
    val canStripOrgans = persistedBody != null && (
        organs.isEmpty() ||
            persistedBody.organs.map { it.toLegacyStatus() } == organs
        )
    val canStripConditions = conditions.isEmpty() ||
        persistedConditions.map { it.toLegacyEffect() } == conditions
    return CharacterRecord(
    id = id,
    ownerId = ownerId,
    campaignId = normalizeCampaignId(campaignId),
    name = name,
    race = race,
    subRace = subRace,
    raceAttribute = raceAttribute,
    occupation = occupation,
    age = age,
    level = level,
    creationStatus = creationStatus.name,
    creationStep = creationStep,
    creationCompletedAt = creationCompletedAt,
    creationRulesVersion = creationRulesVersion,
    itemSchemaVersion = itemSchemaVersion,
    canonicalSchemaVersion = canonicalSchemaVersion,
    migrationReviewPayload = MigrationReviewCodec.encode(migrationReviews),
    canonicalAbilitiesPayload = canonicalConverters.canonicalAbilitiesToString(abilitiesForPersistence()),
    canonicalBodyPayload = canonicalConverters.canonicalBodyToString(persistedBody).orEmpty(),
    canonicalConditionsPayload = canonicalConverters.canonicalConditionsToString(persistedConditions),
    activeModifiersPayload = canonicalConverters.activeModifiersToString(activeModifiers),
    canonicalItemsPayload = canonicalConverters.canonicalItemsToString(itemsForPersistence().first),
    scopedItemCatalogPayload = canonicalConverters.scopedItemCatalogToString(itemsForPersistence().second),
    canonicalProgressionPayload = canonicalConverters.progressionToString(progression),
    progressionLifeBonus = progressionLifeBonus,
    progressionSanityBonus = progressionSanityBonus,
    progressionArcaneBonus = progressionArcaneBonus,
    progressionEnergyBonus = progressionEnergyBonus,
    progressionHistory = progressionHistory,
    money = money,
    // Maximums are projections of the canonical formulas, never persisted authority.
    life = life.copy(maximum = 0),
    sanity = sanity.copy(maximum = 0),
    arcane = arcane.copy(maximum = 0),
    energy = energy.copy(maximum = 0),
    destiny = destiny.copy(maximum = 0),
    exhaustion = exhaustion,
    corruption = corruption.copy(
        current = corruption.current.coerceIn(0, 100),
        maximum = 100,
    ),
    attributes = attributes,
    // Protection totals are derived from attributes, equipment and adjustments on read.
    protections = emptyMap(),
    protectionAdjustments = protectionAdjustments,
    positiveTraits = positiveTraits,
    negativeTraits = negativeTraits,
    pathName = pathName,
    pathMotto = pathMotto,
    powers = powers.map(Power::canonicalized),
    inventory = withNormalizedInventory().inventory,
    itemCreationDraft = itemCreationDraft,
    story = story,
    notes = "",
    transports = transports,
    familiars = familiars,
    personalNotes = personalNotes,
    updatedAt = updatedAt,
    lastEditedBy = lastEditedBy,
    fieldUpdatedAt = fieldUpdatedAt,
    dirty = dirty,
    lastSyncedAt = lastSyncedAt,
    height = height,
    sex = sex,
    size = size,
    photoUri = photoUri,
    learnedKnowledges = learnedKnowledges,
    arcaneKnowledges = arcaneKnowledges,
    battleTechniques = battleTechniques,
    pathKeywords = pathKeywords,
    pathPillars = pathPillars,
    bodyRegions = if (canStripBodyRegions) emptyList() else bodyRegions,
    agilityLimit = agilityLimit,
    organs = if (canStripOrgans) emptyList() else organs,
    mysticAbilities = mysticAbilities.map(MysticAbility::canonicalized),
    conditions = if (canStripConditions) emptyList() else conditions,
    isLocked = isLocked,
    lockType = lockType.name,
    lockedBy = lockedBy,
    lockedAt = lockedAt,
    deleted = deleted,
    appliedDeliveryIds = appliedDeliveryIds,
    )
}

fun CharacterRecord.hasRedundantLegacyProjections(): Boolean {
    if (protections.isNotEmpty() || notes.isNotBlank()) return true
    val decodedBody = canonicalBodyPayload
        .takeIf { it.isNotBlank() && canonicalConverters.isCanonicalBodyPayloadValid(it) }
        ?.let(canonicalConverters::stringToCanonicalBody)
    if (decodedBody != null) {
        val normalizedRegions = normalizeBodyRegions(bodyRegions)
        val defaultRegions = normalizeBodyRegions(defaultBodyRegions())
        if (bodyRegions.isNotEmpty() && (
                normalizedRegions == defaultRegions ||
                    decodedBody.regions.map { it.toLegacyRegion() } == normalizedRegions
                )
        ) {
            return true
        }
        if (organs.isNotEmpty() && decodedBody.organs.map { it.toLegacyStatus() } == organs) {
            return true
        }
    }
    val decodedConditions = canonicalConditionsPayload
        .takeIf { it.isNotBlank() && canonicalConverters.isCanonicalConditionsPayloadValid(it) }
        ?.let(canonicalConverters::stringToCanonicalConditions)
    if (decodedConditions != null && conditions.isNotEmpty() && decodedConditions.map { it.toLegacyEffect() } == conditions) {
        return true
    }
    return false
}

fun CharacterRecord.optimizedForFirestore(
    migrationManager: CharacterMigrationManager = CharacterMigrationManager(),
): CharacterRecord {
    val migrated = if (migrationManager.requiresMigration(this)) {
        migrationManager.migrate(this, markDirty = false)
    } else {
        this
    }
    return migrated.copy(dirty = false, lastSyncedAt = 0)
}

/**
 * Build a minimal top-level Firestore update map containing only fields that changed
 * between [before] and [after] (plus optional deletions of stripped legacy projections).
 */
fun firestoreCharacterPatch(
    before: CharacterRecord,
    after: CharacterRecord,
    existingDocumentKeys: Set<String> = emptySet(),
    deleteValue: Any? = null,
): Map<String, Any?> = buildMap {
    fun <T> diff(key: String, oldVal: T, newVal: T) {
        if (oldVal != newVal) put(key, newVal)
    }
    diff("ownerId", before.ownerId, after.ownerId)
    diff("campaignId", before.campaignId, after.campaignId)
    diff("name", before.name, after.name)
    diff("race", before.race, after.race)
    diff("subRace", before.subRace, after.subRace)
    diff("raceAttribute", before.raceAttribute, after.raceAttribute)
    diff("occupation", before.occupation, after.occupation)
    diff("age", before.age, after.age)
    diff("level", before.level, after.level)
    diff("creationStatus", before.creationStatus, after.creationStatus)
    diff("creationStep", before.creationStep, after.creationStep)
    diff("creationCompletedAt", before.creationCompletedAt, after.creationCompletedAt)
    diff("creationRulesVersion", before.creationRulesVersion, after.creationRulesVersion)
    diff("itemSchemaVersion", before.itemSchemaVersion, after.itemSchemaVersion)
    diff("canonicalSchemaVersion", before.canonicalSchemaVersion, after.canonicalSchemaVersion)
    diff("migrationReviewPayload", before.migrationReviewPayload, after.migrationReviewPayload)
    diff("canonicalAbilitiesPayload", before.canonicalAbilitiesPayload, after.canonicalAbilitiesPayload)
    diff("canonicalBodyPayload", before.canonicalBodyPayload, after.canonicalBodyPayload)
    diff("canonicalConditionsPayload", before.canonicalConditionsPayload, after.canonicalConditionsPayload)
    diff("activeModifiersPayload", before.activeModifiersPayload, after.activeModifiersPayload)
    diff("canonicalItemsPayload", before.canonicalItemsPayload, after.canonicalItemsPayload)
    diff("scopedItemCatalogPayload", before.scopedItemCatalogPayload, after.scopedItemCatalogPayload)
    diff("canonicalProgressionPayload", before.canonicalProgressionPayload, after.canonicalProgressionPayload)
    diff("progressionLifeBonus", before.progressionLifeBonus, after.progressionLifeBonus)
    diff("progressionSanityBonus", before.progressionSanityBonus, after.progressionSanityBonus)
    diff("progressionArcaneBonus", before.progressionArcaneBonus, after.progressionArcaneBonus)
    diff("progressionEnergyBonus", before.progressionEnergyBonus, after.progressionEnergyBonus)
    diff("progressionHistory", before.progressionHistory, after.progressionHistory)
    diff("money", before.money, after.money)
    diff("life", before.life, after.life)
    diff("sanity", before.sanity, after.sanity)
    diff("arcane", before.arcane, after.arcane)
    diff("energy", before.energy, after.energy)
    diff("destiny", before.destiny, after.destiny)
    diff("exhaustion", before.exhaustion, after.exhaustion)
    diff("corruption", before.corruption, after.corruption)
    diff("attributes", before.attributes, after.attributes)
    diff("protectionAdjustments", before.protectionAdjustments, after.protectionAdjustments)
    diff("positiveTraits", before.positiveTraits, after.positiveTraits)
    diff("negativeTraits", before.negativeTraits, after.negativeTraits)
    diff("pathName", before.pathName, after.pathName)
    diff("pathMotto", before.pathMotto, after.pathMotto)
    diff("powers", before.powers, after.powers)
    diff("inventory", before.inventory, after.inventory)
    diff("itemCreationDraft", before.itemCreationDraft, after.itemCreationDraft)
    diff("story", before.story, after.story)
    diff("transports", before.transports, after.transports)
    diff("familiars", before.familiars, after.familiars)
    diff("personalNotes", before.personalNotes, after.personalNotes)
    diff("updatedAt", before.updatedAt, after.updatedAt)
    diff("lastEditedBy", before.lastEditedBy, after.lastEditedBy)
    diff("fieldUpdatedAt", before.fieldUpdatedAt, after.fieldUpdatedAt)
    diff("height", before.height, after.height)
    diff("sex", before.sex, after.sex)
    diff("size", before.size, after.size)
    diff("photoUri", before.photoUri, after.photoUri)
    diff("learnedKnowledges", before.learnedKnowledges, after.learnedKnowledges)
    diff("arcaneKnowledges", before.arcaneKnowledges, after.arcaneKnowledges)
    diff("battleTechniques", before.battleTechniques, after.battleTechniques)
    diff("pathKeywords", before.pathKeywords, after.pathKeywords)
    diff("pathPillars", before.pathPillars, after.pathPillars)
    diff("containerCapacity", before.containerCapacity, after.containerCapacity)
    diff("agilityLimit", before.agilityLimit, after.agilityLimit)
    diff("mysticAbilities", before.mysticAbilities, after.mysticAbilities)
    diff("isLocked", before.isLocked, after.isLocked)
    diff("lockType", before.lockType, after.lockType)
    diff("lockedBy", before.lockedBy, after.lockedBy)
    diff("lockedAt", before.lockedAt, after.lockedAt)
    diff("deleted", before.deleted, after.deleted)
    diff("appliedDeliveryIds", before.appliedDeliveryIds, after.appliedDeliveryIds)

    fun diffLegacy(key: String, isEmpty: Boolean, oldVal: Any?, newVal: Any?) {
        if (isEmpty && key in existingDocumentKeys && deleteValue != null) put(key, deleteValue)
        else if (oldVal != newVal) put(key, newVal)
    }
    diffLegacy("protections", after.protections.isEmpty(), before.protections, after.protections)
    diffLegacy("notes", after.notes.isBlank(), before.notes, after.notes)
    diffLegacy("bodyRegions", after.bodyRegions.isEmpty(), before.bodyRegions, after.bodyRegions)
    diffLegacy("organs", after.organs.isEmpty(), before.organs, after.organs)
    diffLegacy("conditions", after.conditions.isEmpty(), before.conditions, after.conditions)
    if ("dirty" in existingDocumentKeys && deleteValue != null) put("dirty", deleteValue)
}

private fun Character.itemsForPersistence(): Pair<List<com.kinderman.sdo.domain.model.ItemState>, List<com.kinderman.sdo.domain.model.ScopedItemDefinition>> {
    val definitions = customItemCatalog.associateBy { it.reference }.toMutableMap()
    val states = inventory.map { item ->
        val existing = itemStates.firstOrNull { it.id.value == item.id }
        val custom = item.catalogEntryId.isBlank()
        val projectedReference = com.kinderman.sdo.domain.model.CatalogReference(
            com.kinderman.sdo.domain.model.ItemCatalogId(if (custom) "character:$id:${item.id}" else item.catalogEntryId),
            item.catalogVersion.coerceAtLeast(1),
        )
        val reference = existing?.definition?.takeIf {
            custom || (it.id.value == item.catalogEntryId && it.revision == item.catalogVersion.coerceAtLeast(1))
        } ?: projectedReference
        if (custom) definitions.putIfAbsent(reference, com.kinderman.sdo.domain.model.ScopedItemDefinition(
            reference, com.kinderman.sdo.domain.model.CatalogScope.Character, id,
            item.name.ifBlank { "Item sem nome" }, item.effect,
        ))
        (existing ?: com.kinderman.sdo.domain.model.ItemState(
            id = com.kinderman.sdo.domain.model.ItemInstanceId(item.id),
            definition = reference,
            scope = if (custom) com.kinderman.sdo.domain.model.CatalogScope.Character else com.kinderman.sdo.domain.model.CatalogScope.Canonical,
        )).copy(
            definition = reference,
            stack = com.kinderman.sdo.domain.model.StackState(item.quantity.coerceAtLeast(0), existing?.stack?.groupingKey),
            consumable = if (item.linkedAshId.isNotBlank())
                (existing?.consumable ?: com.kinderman.sdo.domain.model.ConsumableState()).copy(doses = item.quantity.coerceAtLeast(0))
            else existing?.consumable,
        )
    }
    return states to definitions.values.toList()
}

private object MigrationReviewCodec {
    private val encoder = java.util.Base64.getUrlEncoder().withoutPadding()
    private val decoder = java.util.Base64.getUrlDecoder()

    fun encode(reviews: List<NeedsReview>): String = reviews.joinToString(".") { review ->
        listOf(review.field, review.legacyValue, review.reason).joinToString("~") {
            encoder.encodeToString(it.toByteArray(Charsets.UTF_8))
        }
    }

    fun decode(payload: String): List<NeedsReview> = payload.split('.').filter(String::isNotBlank).mapNotNull { row ->
        val fields = row.split('~').mapNotNull { encoded ->
            runCatching { decoder.decode(encoded).toString(Charsets.UTF_8) }.getOrNull()
        }
        fields.takeIf { it.size == 3 }?.let { NeedsReview(it[0], it[1], it[2]) }
    }
}

private fun resolveProtectionAdjustments(
    attributes: List<AttributeValue>,
    storedProtections: Map<String, Int>,
    storedAdjustments: Map<String, Int>,
): Map<String, Int> {
    if (storedAdjustments.isNotEmpty()) return defaultProtectionAdjustments() + storedAdjustments
    if (storedProtections == defaultProtections()) return defaultProtectionAdjustments()

    fun attribute(acronym: String): Int = attributes.firstOrNull { it.acronym == acronym }?.value ?: 0
    fun skill(attributeAcronym: String, name: String): Int = attributes
        .firstOrNull { it.acronym == attributeAcronym }
        ?.skills
        ?.firstOrNull { it.name == name }
        ?.value
        ?: 0

    val generalTotal = storedProtections["Geral"] ?: 10
    val bases = linkedMapOf(
        "Geral" to 10,
        "Esquiva" to (generalTotal + attribute("AGI") + skill("AGI", "Reflexos")),
        "Postura" to (10 + attribute("CAR") + skill("CAR", "Lábia")),
        "Mental" to (10 + attribute("INT") + skill("INT", "Sanidade")),
        "Arcana" to (10 + attribute("POD") + skill("POD", "Arcano")),
    )
    return bases.mapValues { (name, base) -> (storedProtections[name] ?: base) - base }
}

/** Normalizes root lock and campaign invariants before Spark v2 wire encoding. */
fun CharacterRecord.normalizedForSparkV2(): CharacterRecord {
    val resolvedLockType = if (lockType == "NONE" && isLocked) {
        CharacterLock.HISTORIAN.name
    } else if (lockType in setOf("NONE", "PLAYER", "HISTORIAN")) {
        lockType
    } else {
        CharacterLock.NONE.name
    }
    return copy(
        campaignId = normalizeCampaignId(campaignId),
        lockType = resolvedLockType,
        isLocked = resolvedLockType != CharacterLock.NONE.name,
        lockedBy = if (resolvedLockType == CharacterLock.NONE.name) "" else lockedBy,
        lockedAt = if (resolvedLockType == CharacterLock.NONE.name) null else lockedAt,
    )
}
