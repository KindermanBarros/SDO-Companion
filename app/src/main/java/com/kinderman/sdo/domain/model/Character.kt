package com.kinderman.sdo.domain.model

import java.util.UUID

enum class UserRole { USER, ADMIN, PLAYER, MASTER }

enum class CharacterLock { NONE, PLAYER, HISTORIAN }

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
) {
    val isCatalogEntry: Boolean get() = catalogEntryId.isNotBlank()
}

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
)

data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val state: String = "M",
    val name: String = "",
    val load: Int = 0,
    val durability: String = "",
    val region: String = "",
    val effect: String = "",
    val pg: Int = 0,
    val pl: Int = 0,
    val category: String = "",
    val agilityLimit: Int? = null,
    val quality: String = "Comum",
    val bonuses: List<ItemBonus> = emptyList(),
)

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
    val containerCapacity: Int = 0,
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
) {
    val isLocked: Boolean get() = lockType != CharacterLock.NONE
    val currentLoad: Int get() = inventory.filterNot { it.state == "G" }.sumOf { it.load }
    val maximumLoad: Int get() = 2 + attributeValue("FOR") + containerCapacity

    val lifeBase: Int get() = 10 + skillValue("VIG", "Vitalidade")
    val sanityBase: Int get() = 10 + skillValue("INT", "Sanidade")
    val arcaneBase: Int get() = attributeValue("POD") + skillValue("POD", "Arcano")
    val energyBase: Int get() = attributeValue("VIG") + skillValue("VIG", "Energia")

    val lifeMaximum: Int get() = (lifeBase + life.adjustment).coerceAtLeast(0)
    val sanityMaximum: Int get() = (sanityBase + sanity.adjustment).coerceAtLeast(0)
    val arcaneMaximum: Int get() = (arcaneBase + arcane.adjustment).coerceAtLeast(0)
    val energyMaximum: Int get() = (energyBase + energy.adjustment).coerceAtLeast(0)

    fun lifeCalculation() = CalculatedValue(lifeBase, life.adjustment)
    fun sanityCalculation() = CalculatedValue(sanityBase, sanity.adjustment)
    fun arcaneCalculation() = CalculatedValue(arcaneBase, arcane.adjustment)
    fun energyCalculation() = CalculatedValue(energyBase, energy.adjustment)

    fun protectionBase(name: String): Int = when (name) {
        "Geral" -> 10 + equippedGeneralProtection
        "Esquiva" -> protectionTotal("Geral") + attributeValue("AGI") + skillValue("AGI", "Reflexos")
        "Postura" -> 10 + attributeValue("CAR") + skillValue("CAR", "Lábia")
        "Mental" -> 10 + attributeValue("INT") + skillValue("INT", "Sanidade")
        "Arcana" -> 10 + attributeValue("POD") + skillValue("POD", "Arcano")
        else -> 0
    }

    fun protectionTotal(name: String): Int =
        (protectionBase(name) + (protectionAdjustments[name] ?: 0)).coerceAtLeast(0)

    fun protectionCalculation(name: String) = CalculatedValue(
        base = protectionBase(name),
        adjustment = protectionAdjustments[name] ?: 0,
    )

    fun calculatedProtections(): Map<String, Int> = defaultProtectionNames.associateWith(::protectionTotal)

    val equippedGeneralProtection: Int
        get() = equippedItems().sumOf { it.pg }

    val equippedAgilityLimit: Int?
        get() = equippedItems().mapNotNull { it.agilityLimit }.minOrNull()

    fun localProtection(region: BodyRegion): Int =
        region.localProtection + equippedItems(region).sumOf { it.pl }

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
                item.id in allEquippedIds -> item.copy(state = "E")
                item.id in affectedIds && item.state == "E" -> item.copy(state = "M")
                else -> item
            }
        }
        return copy(bodyRegions = updatedRegions, inventory = updatedInventory)
    }

    fun removeInventoryItem(itemId: String): Character = copy(
        inventory = inventory.filterNot { it.id == itemId },
        bodyRegions = bodyRegions.map { region ->
            region.copy(equippedItemIds = region.equippedItemIds.filterNot { it == itemId })
        },
    )

    private fun equippedItems(): List<InventoryItem> {
        val equippedIds = bodyRegions.flatMap { it.equippedItemIds }.toSet()
        return inventory.filter { it.id in equippedIds }
    }

    fun acquiredKnowledgeValue(name: String): Int {
        val matching = (learnedKnowledges + arcaneKnowledges + battleTechniques)
            .filter { it.name.equals(name, true) }
        return matching.sumOf { it.value + it.adjustment } + equippedBonus(ItemBonusType.ACQUIRED_KNOWLEDGE, name)
    }

    fun acquiredKnowledgeCalculation(name: String): CalculatedValue {
        val matching = (learnedKnowledges + arcaneKnowledges + battleTechniques)
            .filter { it.name.equals(name, true) }
        return CalculatedValue(
            base = matching.sumOf { it.value },
            adjustment = matching.sumOf { it.adjustment },
            modifiers = equippedModifiers(ItemBonusType.ACQUIRED_KNOWLEDGE, name),
        )
    }

    fun attributeTotal(acronym: String): Int = attributeCalculation(acronym).total

    fun attributeCalculation(acronym: String): CalculatedValue {
        val attribute = attributes.firstOrNull { it.acronym.equals(acronym, true) }
        return CalculatedValue(
            base = attribute?.value ?: 0,
            adjustment = attribute?.modifier ?: 0,
            modifiers = equippedModifiers(ItemBonusType.ATTRIBUTE, acronym),
        )
    }

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
            modifiers = equippedModifiers(ItemBonusType.BASIC_KNOWLEDGE, ItemBonus.basicKnowledgeTarget(attributeAcronym, skillName), skillName),
        )
    }

    private fun attributeValue(acronym: String): Int = attributeTotal(acronym)

    private fun skillValue(attributeAcronym: String, skillName: String): Int = basicKnowledgeTotal(attributeAcronym, skillName)

    private fun equippedBonus(type: ItemBonusType, target: String): Int = equippedModifiers(type, target).sumOf { it.value }

    private fun equippedModifiers(type: ItemBonusType, target: String, legacyTarget: String = target): List<ValueModifier> =
        equippedItems().flatMap { item ->
            item.bonuses
                .filter { bonus ->
                    bonus.type == type &&
                        (bonus.target.equals(target, true) || bonus.target.equals(legacyTarget, true))
                }
                .map { bonus ->
                    ValueModifier(
                        sourceType = ModifierSourceType.ITEM,
                        sourceId = item.id,
                        label = item.name.ifBlank { "Item sem nome" },
                        value = bonus.value,
                    )
                }
        }
}

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
