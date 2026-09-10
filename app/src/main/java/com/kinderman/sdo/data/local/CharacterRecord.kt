package com.kinderman.sdo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.kinderman.sdo.domain.model.AttributeValue
import com.kinderman.sdo.domain.model.BodyRegion
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterLock
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.MysticAbility
import com.kinderman.sdo.domain.model.OrganStatus
import com.kinderman.sdo.domain.model.PersonalNote
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.defaultAttributes
import com.kinderman.sdo.domain.model.defaultBodyRegions
import com.kinderman.sdo.domain.model.defaultProtectionAdjustments
import com.kinderman.sdo.domain.model.defaultProtections
import com.kinderman.sdo.domain.model.canonicalized
import com.kinderman.sdo.domain.model.normalizeBodyRegions
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.domain.catalog.withRefreshedPresetPowers

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
    val money: Int = 0,
    val life: ResourceValue = ResourceValue(),
    val sanity: ResourceValue = ResourceValue(),
    val arcane: ResourceValue = ResourceValue(),
    val energy: ResourceValue = ResourceValue(),
    val destiny: ResourceValue = ResourceValue(1, 5),
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
    val story: String = "",
    val notes: String = "",
    val personalNotes: List<PersonalNote> = emptyList(),
    val updatedAt: Long = 0,
    val dirty: Boolean = false,
    @get:Exclude
    @field:Exclude
    val lastSyncedAt: Long = 0,
    val height: String = "",
    val sex: String = "",
    val size: String = "",
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

fun CharacterRecord.toDomain() = Character(
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
    level = level,
    money = money,
    life = life,
    sanity = sanity,
    arcane = arcane,
    energy = energy,
    destiny = destiny,
    exhaustion = exhaustion,
    corruption = corruption,
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
    inventory = inventory,
    containerCapacity = containerCapacity,
    bodyRegions = normalizeBodyRegions(bodyRegions),
    agilityLimit = agilityLimit,
    organs = organs,
    mysticAbilities = mysticAbilities.map(MysticAbility::canonicalized),
    conditions = conditions,
    story = story,
    notes = notes,
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
    dirty = dirty,
    lastSyncedAt = lastSyncedAt,
    deleted = deleted,
    appliedDeliveryIds = appliedDeliveryIds,
).withRefreshedPresetPowers()

fun Character.toRecord() = CharacterRecord(
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
    money = money,
    life = life.copy(maximum = lifeMaximum),
    sanity = sanity.copy(maximum = sanityMaximum),
    arcane = arcane.copy(maximum = arcaneMaximum),
    energy = energy.copy(maximum = energyMaximum),
    destiny = destiny,
    exhaustion = exhaustion,
    corruption = corruption,
    attributes = attributes,
    protections = calculatedProtections(),
    protectionAdjustments = protectionAdjustments,
    positiveTraits = positiveTraits,
    negativeTraits = negativeTraits,
    pathName = pathName,
    pathMotto = pathMotto,
    powers = powers.map(Power::canonicalized),
    inventory = inventory,
    story = story,
    notes = "",
    personalNotes = personalNotes,
    updatedAt = updatedAt,
    dirty = dirty,
    lastSyncedAt = lastSyncedAt,
    height = height,
    sex = sex,
    size = size,
    learnedKnowledges = learnedKnowledges,
    arcaneKnowledges = arcaneKnowledges,
    battleTechniques = battleTechniques,
    pathKeywords = pathKeywords,
    pathPillars = pathPillars,
    containerCapacity = containerCapacity,
    bodyRegions = bodyRegions,
    agilityLimit = agilityLimit,
    organs = organs,
    mysticAbilities = mysticAbilities.map(MysticAbility::canonicalized),
    conditions = conditions,
    isLocked = isLocked,
    lockType = lockType.name,
    lockedBy = lockedBy,
    lockedAt = lockedAt,
    deleted = deleted,
    appliedDeliveryIds = appliedDeliveryIds,
)

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
