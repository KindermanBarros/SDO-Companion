package com.kinderman.sdo.domain.model

import java.util.UUID

enum class UserRole { USER, ADMIN, PLAYER, MASTER }

enum class CharacterLock { NONE, PLAYER, HISTORIAN }

enum class CharacterCreationStatus { DRAFT, COMPLETED }

enum class PowerSourceType {
    PATH,
    RACE,
    ITEM,
    KNOWLEDGE,
    NARRATIVE,
    MANUAL,
    CATALOG,
}

data class UserSession(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: UserRole,
) {
    val isAdmin: Boolean get() = role == UserRole.ADMIN
}

data class ResourceValue(
    val current: Int = 0,
    val maximum: Int = 0,
    val adjustment: Int = 0,
)

data class SkillValue(
    val name: String = "",
    val value: Int = 0,
    val modifier: Int = 0,
)

data class AttributeValue(
    val name: String = "",
    val acronym: String = "",
    val value: Int = 0,
    val modifier: Int = 0,
    val skills: List<SkillValue> = emptyList(),
)

data class SpecialKnowledge(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val attribute: String = "",
    val value: Int = 0,
    val catalogEntryId: String = "",
    val catalogVersion: Int = 0,
    val category: String = "",
    val description: String = "",
    val prerequisites: List<String> = emptyList(),
    val mechanicalEffect: String = "",
    val source: String = "",
    val ruleReference: String = "",
    val keywords: List<String> = emptyList(),
    val repeatable: Boolean = false,
    val adjustment: Int = 0,
    val milestoneLevels: List<Int> = emptyList(),
    val milestoneRewards: List<KnowledgeMilestoneReward> = emptyList(),
    val specializationParentId: String = "",
    val pendingMilestoneLevels: List<Int> = emptyList(),
    val pendingTargetLevel: Int? = null,
) {
    val isCatalogEntry: Boolean get() = catalogEntryId.isNotBlank()
}

enum class KnowledgeMilestoneRewardType { POWER, MYSTIC_ABILITY, SPECIALIZATION }

data class KnowledgeMilestoneReward(
    val level: Int = 3,
    val type: KnowledgeMilestoneRewardType = KnowledgeMilestoneRewardType.POWER,
    val rewardCatalogId: String = "",
    val grantedEntityId: String = "",
)

data class Power(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val origin: String = "",
    val cost: String = "",
    val action: String = "",
    val range: String = "",
    val duration: String = "",
    val limit: String = "",
    val effect: String = "",
    val category: String = "",
    val prerequisites: List<String> = emptyList(),
    val activationCondition: String = "",
    val enhancements: String = "",
    val deactivationCondition: String = "",
    val ruleReference: String = "",
    val sourceType: PowerSourceType = PowerSourceType.MANUAL,
    val sourceId: String = "",
    val catalogEntryId: String = "",
    val catalogVersion: Int = 0,
    val favorite: Boolean = false,
    val available: Boolean = true,
    val canonicalSource: AbilitySource = AbilitySource.NARRATIVE,
    val knowledgeId: String = "",
    val knowledgeLevel: Int? = null,
    val costType: AbilityCostType = AbilityCostType.NONE,
    val costValue: Int = 0,
    val destinyCostEligible: Boolean = false,
    val executionType: AbilityExecution = AbilityExecution.ACTION,
    val rangeType: AbilityRange = AbilityRange.PERSONAL,
    val targetArea: String = "",
    val durationType: AbilityDuration = AbilityDuration.INSTANT,
    val durationValue: Int = 0,
    val durationUnit: AbilityTimeUnit = AbilityTimeUnit.HOURS,
    val resistance: AbilityResistance = AbilityResistance.NONE,
    val timeValue: Int = 0,
    val timeUnit: AbilityTimeUnit = AbilityTimeUnit.MINUTES,
    val grantsPermanentBonus: Boolean = false,
    val modifiers: List<AbilityModifier> = emptyList(),
    val active: Boolean = false,
    val linkedItemId: String = "",
    val revision: Int = 1,
)

data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val state: String = "M",
    val name: String = "",
    val load: Int = 0,
    val backpackCapacity: Int = 0,
    val durabilityCurrent: Int = 0,
    val durabilityMax: Int = 0,
    val itemCondition: ItemCondition = ItemCondition.NORMAL,
    val region: String = "",
    val effect: String = "",
    val pg: Int = 0,
    val pl: Int = 0,
    val category: String = "",
    val agilityLimit: Int? = null,
    val quality: ItemQuality = ItemQuality.COMMON,
    val quantity: Int = 0,
    val linkedAshId: String = "",
    val ashPurity: AshPurity = AshPurity.RAW,
    val acquisitionSource: ItemAcquisitionSource = ItemAcquisitionSource.NARRATIVE,
    val heritageCost: Int? = null,
    val purchasePrice: Int? = null,
    val catalogEntryId: String = "",
    val catalogVersion: Int = 0,
    val acquiredAt: Long = System.currentTimeMillis(),
    val canonical: Boolean = false,
    val baseId: String = "",
    val materialId: String = "",
    val modificationIds: List<String> = emptyList(),
    val gemIds: List<String> = emptyList(),
    val mechanicalEffects: List<ItemEffect> = emptyList(),
    val dataVersion: Int = CURRENT_ITEM_DATA_VERSION,
)

enum class ItemCondition { NORMAL, SCRAP, BROKEN }

val InventoryItem.isScrap: Boolean get() = itemCondition == ItemCondition.SCRAP || durabilityMax > 0 && durabilityCurrent == 0
val InventoryItem.isBroken: Boolean get() = itemCondition == ItemCondition.BROKEN
val InventoryItem.durabilityLabel: String get() = "$durabilityCurrent/$durabilityMax"

enum class InventoryState(val storageCode: String, val label: String) {
    BACKPACK("M", "Mochila"),
    EQUIPPED("E", "Equipado"),
    WIELDED("W", "Empunhado"),
    QUICK_ACCESS("R", "Acesso Rápido"),
    STORED("G", "Guardado");

    companion object {
        fun fromStorage(value: String): InventoryState {
            if (value.equals("CONTAINER", true)) return EQUIPPED
            return entries.firstOrNull {
            it.storageCode.equals(value, true) || it.name.equals(value, true)
            } ?: BACKPACK
        }
    }
}

val InventoryItem.inventoryState: InventoryState get() = InventoryState.fromStorage(state)

fun InventoryItem.withInventoryState(value: InventoryState): InventoryItem = copy(state = value.storageCode)

enum class ItemAcquisitionSource { HERITAGE, PURCHASE, REWARD, NARRATIVE }

data class BodyRegion(
    val roll: Int = 0,
    val name: String = "",
    val failures: Int = 0,
    val damage: String = "",
    val implants: String = "",
    val equipment: String = "",
    val localProtection: Int = 0,
    val generalProtection: Int = 0,
    val equippedItemIds: List<String> = emptyList(),
)

data class OrganStatus(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val failures: Int = 0,
    val implant: String = "",
    val effect: String = "",
)

data class MysticAbility(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "",
    val name: String = "",
    val cost: String = "",
    val action: String = "",
    val range: String = "",
    val duration: String = "",
    val effect: String = "",
    val favorite: Boolean = false,
    val available: Boolean = true,
    val category: String = "",
    val source: String = "",
    val ruleReference: String = "",
    val catalogEntryId: String = "",
    val catalogVersion: Int = 0,
    val canonicalSource: AbilitySource = AbilitySource.NARRATIVE,
    val knowledgeId: String = "",
    val knowledgeLevel: Int? = null,
    val costType: AbilityCostType = AbilityCostType.NONE,
    val costValue: Int = 0,
    val executionType: AbilityExecution = AbilityExecution.ACTION,
    val rangeType: AbilityRange = AbilityRange.PERSONAL,
    val targetArea: String = "",
    val durationType: AbilityDuration = AbilityDuration.INSTANT,
    val durationValue: Int = 0,
    val durationUnit: AbilityTimeUnit = AbilityTimeUnit.HOURS,
    val resistance: AbilityResistance = AbilityResistance.NONE,
    val timeValue: Int = 0,
    val timeUnit: AbilityTimeUnit = AbilityTimeUnit.MINUTES,
    val ashSource: AshSource = AshSource.FIRE,
    val ashPurity: AshPurity = AshPurity.RAW,
    val linkedInventoryItemId: String = "",
    val inscriberId: String = "",
    val inscriberPower: Int = 0,
    val inscriberRunicKnowledge: Int = 0,
    val revision: Int = 1,
)

data class ConditionEffect(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val intensity: String = "",
    val duration: String = "",
    val origin: String = "",
    val summary: String = "",
)

data class PersonalNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val text: String = "",
)

enum class ProgressionRewardType { RESOURCE, ATTRIBUTE, KNOWLEDGE, NEW_KNOWLEDGE, PATH_POWER }

data class ProgressionReward(
    val level: Int = 1,
    val type: ProgressionRewardType = ProgressionRewardType.RESOURCE,
    val targetId: String = "",
    val catalogEntryId: String = "",
    val canonical: Boolean = true,
)

data class ProgressionRecord(
    val id: String = UUID.randomUUID().toString(),
    val previousLevel: Int = 1,
    val newLevel: Int = 1,
    val rewards: List<ProgressionReward> = emptyList(),
    val appliedAt: Long = System.currentTimeMillis(),
)

enum class ModifierSourceType { BASE, ADJUSTMENT, ITEM, TRAIT, RACE, CONDITION, OTHER }

data class ValueModifier(
    val sourceType: ModifierSourceType,
    val sourceId: String = "",
    val label: String,
    val value: Int,
)

data class CalculatedValue(
    val base: Int,
    val adjustment: Int = 0,
    val modifiers: List<ValueModifier> = emptyList(),
) {
    val total: Int get() = base + adjustment + modifiers.sumOf { it.value }
}

data class Character(
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String = "",
    val campaignId: String = "default",
    val name: String = "Novo personagem",
    val race: String = "",
    val subRace: String = "",
    val raceAttribute: String = "",
    val occupation: String = "",
    val height: String = "",
    val age: String = "",
    val sex: String = "",
    val size: String = "",
    val level: Int = 1,
    val creationStatus: CharacterCreationStatus = CharacterCreationStatus.DRAFT,
    val creationStep: Int = 1,
    val creationCompletedAt: Long? = null,
    val creationRulesVersion: Int = 1,
    val itemSchemaVersion: Int = CURRENT_ITEM_DATA_VERSION,
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
    val destiny: ResourceValue = ResourceValue(1, 5),
    val exhaustion: ResourceValue = ResourceValue(0, 10),
    val corruption: ResourceValue = ResourceValue(0, 100),
    val attributes: List<AttributeValue> = defaultAttributes(),
    val protections: Map<String, Int> = defaultProtections(),
    val protectionAdjustments: Map<String, Int> = defaultProtectionAdjustments(),
    val positiveTraits: List<String> = listOf(""),
    val negativeTraits: List<String> = listOf(""),
    val learnedKnowledges: List<SpecialKnowledge> = emptyList(),
    val arcaneKnowledges: List<SpecialKnowledge> = emptyList(),
    val battleTechniques: List<SpecialKnowledge> = emptyList(),
    val pathName: String = "",
    val pathMotto: String = "",
    val pathKeywords: List<String> = listOf("", "", ""),
    val pathPillars: List<String> = listOf("", "", ""),
    val powers: List<Power> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
    val itemCreationDraft: ItemCreationDraft? = null,
    val bodyRegions: List<BodyRegion> = defaultBodyRegions(),
    val agilityLimit: String = "",
    val organs: List<OrganStatus> = emptyList(),
    val mysticAbilities: List<MysticAbility> = emptyList(),
    val conditions: List<ConditionEffect> = emptyList(),
    val story: String = "",
    val notes: String = "",
    val personalNotes: List<PersonalNote> = emptyList(),
    val lockType: CharacterLock = CharacterLock.NONE,
    val lockedBy: String = "",
    val lockedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = true,
    val lastSyncedAt: Long = 0,
    val deleted: Boolean = false,
    val appliedDeliveryIds: List<String> = emptyList(),
) {
    val isInCreation: Boolean get() = creationStatus == CharacterCreationStatus.DRAFT
    val isLocked: Boolean get() = lockType != CharacterLock.NONE
    val currentLoad: Int get() = inventory.filterNot { it.inventoryState == InventoryState.STORED }.sumOf { it.effectiveLoad() }
    val backpackCapacity: Int get() = inventory
        .filter { it.inventoryState == InventoryState.EQUIPPED }
        .filter { it.catalogEntryId.isNotBlank() && it.category.equals("Recipiente de Carga", true) }
        .maxOfOrNull(InventoryItem::backpackCapacity) ?: 0
    val maximumLoad: Int get() = 2 + attributeValue("FOR") + backpackCapacity

    val lifeBase: Int get() = 10 + skillValue("VIG", "Vitalidade")
    val sanityBase: Int get() = 10 + skillValue("INT", "Sanidade")
    val arcaneBase: Int get() = attributeValue("POD") + skillValue("POD", "Arcano")
    val energyBase: Int get() = attributeValue("VIG") + skillValue("VIG", "Energia")

    val lifeMaximum: Int get() = (lifeBase + life.adjustment + progressionLifeBonus + powerModifier(AbilityModifierTarget.RESOURCE_MAXIMUM, "LIFE")).coerceAtLeast(0)
    val sanityMaximum: Int get() = (sanityBase + sanity.adjustment + progressionSanityBonus + powerModifier(AbilityModifierTarget.RESOURCE_MAXIMUM, "SANITY")).coerceAtLeast(0)
    val arcaneMaximum: Int get() = (arcaneBase + arcane.adjustment + progressionArcaneBonus + powerModifier(AbilityModifierTarget.RESOURCE_MAXIMUM, "ARCANE")).coerceAtLeast(0)
    val energyMaximum: Int get() = (energyBase + energy.adjustment + progressionEnergyBonus + powerModifier(AbilityModifierTarget.RESOURCE_MAXIMUM, "ENERGY")).coerceAtLeast(0)
    val destinyMaximum: Int get() = (destiny.maximum + powerModifier(AbilityModifierTarget.RESOURCE_MAXIMUM, "DESTINY")).coerceAtLeast(0)

    fun lifeCalculation() = CalculatedValue(lifeBase, life.adjustment, powerValueModifiers(AbilityModifierTarget.RESOURCE_MAXIMUM, "LIFE"))
    fun sanityCalculation() = CalculatedValue(sanityBase, sanity.adjustment, powerValueModifiers(AbilityModifierTarget.RESOURCE_MAXIMUM, "SANITY"))
    fun arcaneCalculation() = CalculatedValue(arcaneBase, arcane.adjustment, powerValueModifiers(AbilityModifierTarget.RESOURCE_MAXIMUM, "ARCANE"))
    fun energyCalculation() = CalculatedValue(energyBase, energy.adjustment, powerValueModifiers(AbilityModifierTarget.RESOURCE_MAXIMUM, "ENERGY"))

    fun protectionBase(name: String): Int = when (name) {
        "Geral" -> 10 + equippedGeneralProtection
        "Esquiva" -> protectionTotal("Geral") + attributeValue("AGI") + skillValue("AGI", "Reflexos")
        "Postura" -> 10 + attributeValue("CAR") + skillValue("CAR", "Lábia")
        "Mental" -> 10 + attributeValue("INT") + skillValue("INT", "Sanidade")
        "Arcana" -> 10 + attributeValue("POD") + skillValue("POD", "Arcano")
        else -> 0
    }

    fun protectionTotal(name: String): Int =
        (protectionBase(name) + (protectionAdjustments[name] ?: 0) + powerModifier(AbilityModifierTarget.PROTECTION, name)).coerceAtLeast(0)

    fun protectionCalculation(name: String) = CalculatedValue(
        base = protectionBase(name),
        adjustment = protectionAdjustments[name] ?: 0,
    )

    fun calculatedProtections(): Map<String, Int> = defaultProtectionNames.associateWith(::protectionTotal)

    val equippedGeneralProtection: Int
        get() {
            val typed = EquipmentEffectEngine.resolve(this).entries.filter { it.type == ItemEffectType.PG }
            return if (typed.isNotEmpty()) typed.sumOf { it.value }
            else equippedItems().filterNot { it.category.equals("Escudo", true) && it.inventoryState != InventoryState.WIELDED }.sumOf { it.pg }
        }

    val equippedAgilityLimit: Int?
        get() = EquipmentEffectEngine.resolve(this).agilityLimit
            ?: equippedItems().mapNotNull { it.agilityLimit }.minOrNull()

    val equipmentAttackBonus: Int get() = EquipmentEffectEngine.resolve(this).attackBonus
    val equipmentPhysicalDamageBonus: Int get() = EquipmentEffectEngine.resolve(this).physicalDamageBonus
    val equipmentMagicDamageBonus: Int get() = EquipmentEffectEngine.resolve(this).magicDamageBonus
    fun equipmentDurabilityBonus(itemId: String): Int = EquipmentEffectEngine.resolve(this).durabilityBonus(itemId)
    val activeEquipmentRules: List<EquipmentEffectAudit> get() = EquipmentEffectEngine.resolve(this).activeRules

    fun localProtection(region: BodyRegion): Int {
        val typed = EquipmentEffectEngine.resolve(this).entries
            .filter { it.type == ItemEffectType.PL && (it.targetId.isBlank() || it.targetId.equals(region.name, true)) }
        return region.localProtection + if (typed.isNotEmpty()) typed.sumOf { it.value } else equippedItems(region).sumOf { it.pl }
    }

    fun equippedItems(region: BodyRegion): List<InventoryItem> =
        inventory.filter { it.id in region.equippedItemIds }

    fun equipItems(regionIndex: Int, itemIds: Set<String>): Character {
        if (regionIndex !in bodyRegions.indices) return this
        val regionName = bodyRegions[regionIndex].name
        val candidates = inventory.filter { it.id in itemIds && it.matchesRegion(regionName) }
        val armor = candidates.filter { it.category.equals("Armadura", true) }.takeLast(1)
        val accessories = candidates.filter { it.category.equals("Acessório", true) }.takeLast(1)
        val other = candidates.filterNot {
            it.category.equals("Armadura", true) || it.category.equals("Acessório", true)
        }
        val selectedIds = (other + armor + accessories).mapTo(linkedSetOf()) { it.id }
        val updatedRegions = bodyRegions.mapIndexed { index, region ->
            if (index == regionIndex) region.copy(equippedItemIds = selectedIds.toList()) else region
        }
        val allEquippedIds = updatedRegions.flatMap { it.equippedItemIds }.toSet()
        val previouslyAssignedIds = bodyRegions.flatMap { it.equippedItemIds }.toSet()
        val affectedIds = previouslyAssignedIds + selectedIds
        val updatedInventory = inventory.map { item ->
            when {
                item.id in allEquippedIds -> item.withInventoryState(
                    if (item.inventoryState == InventoryState.WIELDED) InventoryState.WIELDED else InventoryState.EQUIPPED,
                )
                item.id in affectedIds && item.inventoryState in setOf(InventoryState.EQUIPPED, InventoryState.WIELDED) ->
                    item.withInventoryState(InventoryState.BACKPACK)
                else -> item
            }
        }
        return copy(bodyRegions = updatedRegions, inventory = updatedInventory).synchronizeItemPowers()
    }

    fun removeInventoryItem(itemId: String): Character = copy(
        inventory = inventory.filterNot { it.id == itemId },
        bodyRegions = bodyRegions.map { region ->
            region.copy(equippedItemIds = region.equippedItemIds.filterNot { it == itemId })
        },
    ).synchronizeItemPowers()

    private fun equippedItems(): List<InventoryItem> {
        val equippedIds = bodyRegions.flatMap { it.equippedItemIds }.toSet()
        return inventory.filter { it.id in equippedIds }
    }

    fun acquiredKnowledgeValue(name: String): Int {
        return acquiredKnowledgeCalculation(name).total.coerceAtLeast(0)
    }

    fun acquiredKnowledgeCalculation(name: String): CalculatedValue {
        val matching = (learnedKnowledges + arcaneKnowledges + battleTechniques)
            .filter { it.name.equals(name, true) }
        return CalculatedValue(
            base = matching.sumOf { knowledge ->
                val permanentAttribute = attributes.firstOrNull { it.acronym.equals(knowledge.attribute, true) }
                    ?.let { permanentAttributeValue(it.acronym) }
                    ?: knowledge.value // Legacy entries without an attribute remain readable until explicitly migrated.
                minOf(knowledge.value.coerceIn(0, 5), permanentAttribute.coerceAtLeast(0))
            },
            adjustment = matching.sumOf { it.adjustment },
            modifiers = equippedKnowledgeModifiers(matching.firstOrNull()?.id ?: name, name) + powerValueModifiers(AbilityModifierTarget.KNOWLEDGE, matching.firstOrNull()?.id ?: name),
        )
    }

    fun attributeTotal(acronym: String): Int = attributeCalculation(acronym).total

    fun attributeCalculation(acronym: String): CalculatedValue {
        val attribute = attributes.firstOrNull { it.acronym.equals(acronym, true) }
        return CalculatedValue(
            base = attribute?.value ?: 0,
            adjustment = attribute?.modifier ?: 0,
            modifiers = racialAttributeModifiers(acronym) + equippedAttributeModifiers(acronym) + powerValueModifiers(AbilityModifierTarget.ATTRIBUTE, acronym),
        )
    }

    fun permanentAttributeValue(acronym: String): Int {
        val attribute = attributes.firstOrNull { it.acronym.equals(acronym, true) }
        return ((attribute?.value ?: 0) + racialAttributeModifiers(acronym).sumOf { it.value })
            .coerceAtLeast(0)
    }

    private fun racialAttributeModifiers(acronym: String): List<ValueModifier> =
        if (raceAttribute.equals(acronym, true) && raceAttribute.isNotBlank()) {
            listOf(ValueModifier(ModifierSourceType.RACE, race, race.ifBlank { "Bônus racial" }, 1))
        } else emptyList()

    fun basicKnowledgeTotal(attributeAcronym: String, skillName: String): Int =
        basicKnowledgeCalculation(attributeAcronym, skillName).total

    fun basicKnowledgeCalculation(attributeAcronym: String, skillName: String): CalculatedValue {
        val skill = attributes
            .firstOrNull { it.acronym.equals(attributeAcronym, true) }
            ?.skills
            ?.firstOrNull { it.name.equals(skillName, true) }
        return CalculatedValue(
            base = skill?.value ?: 0,
            adjustment = skill?.modifier ?: 0,
            modifiers = equippedKnowledgeModifiers(basicKnowledgeId(attributeAcronym, skillName), skillName),
        )
    }

    private fun attributeValue(acronym: String): Int = attributeTotal(acronym)

    private fun skillValue(attributeAcronym: String, skillName: String): Int = basicKnowledgeTotal(attributeAcronym, skillName)

    private fun powerModifier(type: AbilityModifierTarget, target: String): Int =
        activePowerModifiers().filter { (_, modifier) -> modifier.targetType == type && modifier.targetId.equals(target, true) }.sumOf { it.second.value }

    internal fun powerValueModifiers(type: AbilityModifierTarget, target: String): List<ValueModifier> =
        activePowerModifiers().filter { (_, modifier) -> modifier.targetType == type && modifier.targetId.equals(target, true) }.map { (power, modifier) ->
            ValueModifier(ModifierSourceType.TRAIT, power.id, power.name.ifBlank { "Poder passivo" }, modifier.value)
        }

    private fun equippedAttributeModifiers(targetId: String): List<ValueModifier> =
        equipmentModifiers(ItemEffectType.ATTRIBUTE, targetId)

    private fun equippedKnowledgeModifiers(targetId: String, displayName: String): List<ValueModifier> =
        EquipmentEffectEngine.resolve(this).entries.filter { active ->
            active.type == ItemEffectType.KNOWLEDGE &&
                (active.targetId.equals(targetId, true) || active.targetId.equals(displayName, true))
        }.map { active ->
            ValueModifier(ModifierSourceType.ITEM, active.itemId, active.itemName.ifBlank { "Item sem nome" }, active.value)
        }

    private fun equipmentModifiers(type: ItemEffectType, targetId: String): List<ValueModifier> =
        EquipmentEffectEngine.resolve(this).entries.filter { it.type == type && it.targetId.equals(targetId, true) }.map { active ->
            ValueModifier(ModifierSourceType.ITEM, active.itemId, active.itemName.ifBlank { "Item sem nome" }, active.value)
        }
}

fun basicKnowledgeId(attributeAcronym: String, skillName: String): String =
    "basic:${attributeAcronym.uppercase()}:${skillName.lowercase()}"

private fun Int?.orZero() = this ?: 0

fun InventoryItem.matchesRegion(bodyRegionName: String): Boolean {
    if (region.isBlank()) return false
    val itemRegion = region.normalizedEquipmentRegion()
    val bodyRegion = bodyRegionName.normalizedEquipmentRegion()
    return when {
        bodyRegion.startsWith("pe ") || bodyRegion == "pe" -> "pe" in itemRegion
        bodyRegion.startsWith("mao ") || bodyRegion == "mao" -> "mao" in itemRegion
        bodyRegion.startsWith("braco ") || bodyRegion == "braco" -> "braco" in itemRegion
        bodyRegion.startsWith("perna ") || bodyRegion == "perna" -> "perna" in itemRegion
        else -> bodyRegion in itemRegion || itemRegion in bodyRegion
    }
}

private fun String.normalizedEquipmentRegion(): String = lowercase()
    .replace('á', 'a').replace('à', 'a').replace('â', 'a').replace('ã', 'a')
    .replace('é', 'e').replace('ê', 'e').replace('í', 'i')
    .replace('ó', 'o').replace('ô', 'o').replace('õ', 'o').replace('ú', 'u').replace('ç', 'c')
    .replace(Regex("\\b(pes|maos|bracos|pernas)\\b")) { it.value.dropLast(1) }

fun nextPersonalNoteTitle(notes: List<PersonalNote>): String {
    val prefix = "Registro Pessoal "
    val highestNumber = notes.maxOfOrNull { note ->
        note.title.takeIf { it.startsWith(prefix) }
            ?.removePrefix(prefix)
            ?.toIntOrNull()
            ?: 0
    } ?: 0
    return "$prefix${maxOf(notes.size, highestNumber) + 1}"
}

fun defaultAttributes() = listOf(
    AttributeValue("Força", "FOR", skills = listOf("Atletismo", "Brutalidade", "Luta", "Arremesso").map(::SkillValue)),
    AttributeValue("Vigor", "VIG", skills = listOf("Energia", "Vitalidade", "Tolerância", "Regeneração").map(::SkillValue)),
    AttributeValue("Agilidade", "AGI", skills = listOf("Furtividade", "Reflexos", "Movimento", "Pontaria").map(::SkillValue)),
    AttributeValue("Poder", "POD", skills = listOf("Arcano", "Sentidos", "Controle", "Recuperação").map(::SkillValue)),
    AttributeValue("Intelecto", "INT", skills = listOf("Sanidade", "Intuição", "Religião", "Raciocínio").map(::SkillValue)),
    AttributeValue("Carisma", "CAR", skills = listOf("Política", "Lábia", "Enganação", "Intimidação").map(::SkillValue)),
)

fun defaultProtections() = linkedMapOf("Geral" to 10, "Esquiva" to 10, "Postura" to 10, "Mental" to 10, "Arcana" to 10)

val defaultProtectionNames = listOf("Geral", "Esquiva", "Postura", "Mental", "Arcana")

fun defaultProtectionAdjustments() = defaultProtectionNames.associateWith { 0 }

fun defaultBodyRegions() = listOf(
    "Cabeça", "Torso", "Braço direito", "Braço esquerdo", "Mão direita",
    "Mão esquerda", "Perna direita", "Perna esquerda", "Pé direito", "Pé esquerdo",
).mapIndexed { index, name -> BodyRegion(roll = index + 1, name = name) }

fun normalizeBodyRegions(regions: List<BodyRegion>): List<BodyRegion> {
    if (regions.isEmpty()) return defaultBodyRegions()
    return defaultBodyRegions().map { canonical ->
        regions.firstOrNull { it.name.equals(canonical.name, true) }
            ?.copy(roll = canonical.roll, name = canonical.name)
            ?: canonical
    }
}
