package com.kinderman.sdo.domain.model

import java.text.Normalizer
import java.util.UUID
import kotlin.math.ceil
import kotlinx.serialization.Serializable

enum class AbilitySource(val label: String) {
    KNOWLEDGE("Conhecimento"),
    RACE("Raça"),
    PATH("Caminho"),
    ITEM("Item"),
    HISTORY("Histórico"),
    PROFESSION("Profissão"),
    NARRATIVE("Narrativa"),
}

enum class AbilityCostType(val label: String) {
    NONE("Nenhum"),
    ARCANE("Arcano"),
    ENERGY("Energia"),
    DESTINY("Destino"),
    LIFE("Vida"),
    SANITY("Sanidade"),
    DOSE("Dose"),
}

enum class AbilityExecution(val label: String) {
    ACTION("Ação"),
    TURN("Turno"),
    FREE("Livre"),
    REACTION("Reação"),
    PASSIVE("Passiva"),
    TIME("Tempo"),
}

enum class AbilityRange(val label: String) {
    PERSONAL("Pessoal"),
    SHORT("Curto (até 9 m)"),
    MEDIUM("Médio (até 30 m)"),
    LONG("Longo (até 90 m)"),
    INDEFINITE("Indefinido"),
}

enum class AbilityDuration(val label: String) {
    INSTANT("Instantânea"),
    TURNS("Turnos"),
    SCENE("Cena"),
    SESSION("Sessão"),
    TIME("Tempo"),
}

enum class AbilityResistance(val label: String) {
    NONE("Nenhuma"),
    GENERAL("Geral"),
    DODGE("Esquiva"),
    POSTURE("Postura"),
    MENTAL("Mental"),
    ARCANE("Arcana"),
}

enum class AbilityTimeUnit(val label: String) { MINUTES("Minutos"), HOURS("Horas"), DAYS("Dias") }

enum class AshSource(val label: String) {
    FIRE("Fogo"), COLD("Frio"), LIGHTNING("Raio"), HEALING("Cura"), ONEIRIC("Onírico"),
    ACID("Ácido"), CONCUSSIVE("Concussivo"), EARTH("Terra"), NATURE("Natureza"),
    POISON("Venenoso"), WATER("Água"), AIR("Ar"), SOUND("Som"), LIGHT("Luz"),
    DARKNESS("Trevas"), MENTAL("Mental"), ILLUSION("Ilusão"), BLOOD("Sangue"),
    TECHNOLOGY("Tecnologia"), DECAY("Decadência"), DEATH("Morte"), DIVINE("Divino"),
}

@Serializable
enum class AshPurity(val label: String, val dosesPerLoad: Int) {
    RAW("Bruta", 1), REFINED("Refinada", 2), PURE("Pura", 3);

    companion object {
        fun fromName(value: String): AshPurity = entries.firstOrNull {
            it.name.equals(value, true) || it.label.equals(value, true)
        } ?: RAW
    }
}

val AshPurity.heritageCostPerDose: Int
    get() = when (this) {
        AshPurity.RAW -> 1
        AshPurity.REFINED -> 2
        AshPurity.PURE -> 4
    }

enum class AbilityModifierTarget(val label: String) {
    ATTRIBUTE("Atributo"), KNOWLEDGE("Conhecimento"), RESOURCE_MAXIMUM("Máximo de recurso"),
    PROTECTION("Proteção"),
}

data class AbilityModifier(
    val id: String = UUID.randomUUID().toString(),
    val targetType: AbilityModifierTarget = AbilityModifierTarget.ATTRIBUTE,
    val targetId: String = "",
    val value: Int = 0,
)

data class AbilityDuplicateGroup(val key: String, val type: String, val entries: List<Pair<String, String>>)

fun normalizeAbilityName(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase()
    .replace(Regex("[^\\p{L}\\p{N}]+"), "")

fun InventoryItem.effectiveLoad(): Int = if (linkedAshId.isBlank()) load else {
    if (quantity <= 0) 0 else ceil(quantity.toDouble() / ashPurity.dosesPerLoad).toInt()
}

fun Character.withAddedAbility(ability: MysticAbility, reuseExistingAsh: Boolean = false): Character {
    if (reuseExistingAsh && ability.isAsh) {
        mysticAbilities.firstOrNull { it.uniqueKey == ability.uniqueKey }?.let { return this }
    }
    require(canUseAbilityKey(ability)) { "Já existe uma entrada com esse nome" }
    if (!ability.isAsh) return copy(mysticAbilities = mysticAbilities + ability.canonicalized())
    val item = InventoryItem(
        name = ability.name,
        category = "Cinza",
        linkedAshId = ability.id,
        ashPurity = ability.ashPurity,
        quantity = 0,
    )
    return copy(
        mysticAbilities = mysticAbilities + ability.canonicalized().copy(linkedInventoryItemId = item.id),
        inventory = inventory + item,
    )
}

fun Character.withAddedAsh(ability: MysticAbility, doses: Int, initialCreation: Boolean): Character {
    require(ability.isAsh) { "A habilidade selecionada não é uma Cinza" }
    val safeDoses = doses.coerceAtLeast(1)
    val cost = safeDoses * ability.ashPurity.heritageCostPerDose
    val existing = mysticAbilities.firstOrNull { it.uniqueKey == ability.uniqueKey }
    if (existing != null) return copy(inventory = inventory.map { item ->
        if (item.id == existing.linkedInventoryItemId || item.linkedAshId == existing.id) item.copy(
            quantity = item.quantity + safeDoses,
            acquisitionSource = if (initialCreation) ItemAcquisitionSource.HERITAGE else item.acquisitionSource,
            heritageCost = if (initialCreation) (item.heritageCost ?: 0) + cost else item.heritageCost,
        ) else item
    })

    val canonical = ability.canonicalized()
    val item = InventoryItem(
        name = canonical.name,
        category = "Cinza",
        linkedAshId = canonical.id,
        ashPurity = canonical.ashPurity,
        quantity = safeDoses,
        acquisitionSource = if (initialCreation) ItemAcquisitionSource.HERITAGE else ItemAcquisitionSource.NARRATIVE,
        heritageCost = cost.takeIf { initialCreation },
        catalogEntryId = canonical.catalogEntryId,
        canonical = canonical.catalogEntryId.isNotBlank(),
    )
    return copy(
        mysticAbilities = mysticAbilities + canonical.copy(linkedInventoryItemId = item.id),
        inventory = inventory + item,
    )
}

fun Character.withAddedPower(power: Power): Character {
    require(power.name.isBlank() || powers.none { normalizeAbilityName(it.name) == normalizeAbilityName(power.name) }) {
        "Já existe uma entrada com esse nome"
    }
    return copy(powers = powers + power.canonicalized())
}

fun Character.withUpdatedPower(power: Power): Character {
    require(power.name.isBlank() || powers.none { it.id != power.id && normalizeAbilityName(it.name) == normalizeAbilityName(power.name) }) {
        "Já existe uma entrada com esse nome"
    }
    return copy(powers = powers.map { if (it.id == power.id) power.canonicalized() else it }).constrainedToResourceMaximums()
}

fun Power.canonicalized(): Power {
    val normalizedCostType = when {
        executionType == AbilityExecution.PASSIVE -> AbilityCostType.ENERGY
        costType == AbilityCostType.DESTINY && destinyCostEligible -> AbilityCostType.DESTINY
        costType in setOf(AbilityCostType.ENERGY, AbilityCostType.LIFE, AbilityCostType.SANITY) -> costType
        else -> AbilityCostType.ENERGY
    }
    val normalizedCostValue = if (executionType == AbilityExecution.PASSIVE) 0 else costValue.coerceAtLeast(1)
    return copy(
        cost = when (normalizedCostType) {
            AbilityCostType.ENERGY -> "$normalizedCostValue PE"
            AbilityCostType.LIFE -> "$normalizedCostValue PV"
            AbilityCostType.SANITY -> "$normalizedCostValue PS"
            AbilityCostType.DESTINY -> "$normalizedCostValue PD"
            else -> error("Tipo de custo inválido para Poder")
        },
        costType = normalizedCostType,
        costValue = normalizedCostValue,
        timeValue = timeValue.coerceAtLeast(0),
        durationValue = durationValue.coerceAtLeast(0),
        knowledgeId = knowledgeId.takeIf { canonicalSource == AbilitySource.KNOWLEDGE }.orEmpty(),
        knowledgeLevel = knowledgeLevel.takeIf { canonicalSource == AbilitySource.KNOWLEDGE },
        linkedItemId = linkedItemId.takeIf { canonicalSource == AbilitySource.ITEM }.orEmpty(),
        revision = revision.coerceAtLeast(1),
    )
}

fun Character.abilityDuplicates(): List<AbilityDuplicateGroup> {
    val powerGroups = powers.filter { it.name.isNotBlank() }
        .groupBy { normalizeAbilityName(it.name) }
        .filterValues { it.size > 1 }
        .map { (key, entries) -> AbilityDuplicateGroup("power:$key", "Poder", entries.map { it.id to it.name }) }
    val mysticGroups = mysticAbilities.filter { it.name.isNotBlank() }
        .groupBy(MysticAbility::uniqueKey)
        .filterValues { it.size > 1 }
        .map { (key, entries) -> AbilityDuplicateGroup(key, entries.first().type, entries.map { it.id to it.name }) }
    return powerGroups + mysticGroups
}

fun Character.resolveAbilityDuplicate(group: AbilityDuplicateGroup, keepId: String): Character {
    require(group.entries.any { it.first == keepId }) { "Escolha qual registro manter." }
    val removedIds = group.entries.map { it.first }.filterNot { it == keepId }.toSet()
    return copy(
        powers = powers.filterNot { it.id in removedIds },
        mysticAbilities = mysticAbilities.filterNot { it.id in removedIds },
        inventory = inventory.filterNot { it.linkedAshId in removedIds },
    )
}

fun Character.withUpdatedAbility(ability: MysticAbility): Character {
    require(canUseAbilityKey(ability, ability.id)) { "Já existe uma entrada com esse nome" }
    val old = mysticAbilities.firstOrNull { it.id == ability.id } ?: return withAddedAbility(ability)
    var updated = copy(mysticAbilities = mysticAbilities.map { if (it.id == ability.id) ability.canonicalized() else it })
    if (old.isAsh && !ability.isAsh) {
        val oldItem = inventory.firstOrNull { it.id == old.linkedInventoryItemId || it.linkedAshId == old.id }
        require(oldItem == null || oldItem.quantity <= 0) { "Ainda existem essas cinzas no inventário" }
        return updated.copy(inventory = updated.inventory.filterNot { it.id == oldItem?.id })
    }
    if (ability.isAsh) {
        val itemId = old.linkedInventoryItemId.ifBlank { ability.linkedInventoryItemId }
        val item = inventory.firstOrNull { it.id == itemId || it.linkedAshId == ability.id }
        updated = if (item == null) {
            val created = InventoryItem(name = ability.name, category = "Cinza", linkedAshId = ability.id, ashPurity = ability.ashPurity)
            updated.copy(
                mysticAbilities = updated.mysticAbilities.map { if (it.id == ability.id) it.copy(linkedInventoryItemId = created.id) else it },
                inventory = updated.inventory + created,
            )
        } else updated.copy(
            mysticAbilities = updated.mysticAbilities.map { if (it.id == ability.id) it.copy(linkedInventoryItemId = item.id) else it },
            inventory = updated.inventory.map {
                if (it.id == item.id) it.copy(name = ability.name, linkedAshId = ability.id, ashPurity = ability.ashPurity) else it
            },
        )
    }
    return updated
}

fun Character.withRemovedAbility(abilityId: String): Character {
    val ability = mysticAbilities.firstOrNull { it.id == abilityId } ?: return this
    val item = inventory.firstOrNull { it.id == ability.linkedInventoryItemId || it.linkedAshId == ability.id }
    require(item == null || item.quantity <= 0) { "Ainda existem essas cinzas no inventário" }
    return copy(
        mysticAbilities = mysticAbilities.filterNot { it.id == abilityId },
        inventory = inventory.filterNot { it.id == item?.id },
    )
}

fun Character.withAshDoses(abilityId: String, doses: Int): Character = copy(inventory = inventory.map {
    if (it.linkedAshId == abilityId) it.copy(quantity = doses.coerceAtLeast(0)) else it
})

fun Character.canUseAbilityKey(candidate: MysticAbility, ignoredId: String? = null): Boolean {
    if (candidate.name.isBlank()) return true
    val key = candidate.uniqueKey
    return mysticAbilities.none { it.id != ignoredId && it.uniqueKey == key }
}

val MysticAbility.isAsh: Boolean get() = type.equals("Cinza", true)
val MysticAbility.uniqueKey: String
    get() = if (isAsh) "${normalizeAbilityName(name)}:${ashPurity.name}" else "${type.lowercase()}:${normalizeAbilityName(name)}"

fun MysticAbility.canonicalized(): MysticAbility = copy(
    costType = when {
        isAsh -> AbilityCostType.DOSE
        else -> AbilityCostType.ARCANE
    },
    costValue = costValue.coerceAtLeast(0),
    timeValue = timeValue.coerceAtLeast(0),
    durationValue = durationValue.coerceAtLeast(0),
    knowledgeId = knowledgeId.takeIf { !isAsh && canonicalSource == AbilitySource.KNOWLEDGE }.orEmpty(),
    knowledgeLevel = knowledgeLevel.takeIf { !isAsh && canonicalSource == AbilitySource.KNOWLEDGE },
    revision = revision.coerceAtLeast(1),
)

fun formattedAbilityCost(type: AbilityCostType, value: Int): String = when (type) {
    AbilityCostType.NONE -> type.label
    else -> "${value.coerceAtLeast(0)} ${type.label}"
}

fun formattedAbilityExecution(type: AbilityExecution, value: Int, unit: AbilityTimeUnit): String = when (type) {
    AbilityExecution.TIME -> "${type.label}: ${value.coerceAtLeast(0)} ${unit.label.lowercase()}"
    else -> type.label
}

fun formattedAbilityDuration(type: AbilityDuration, value: Int, unit: AbilityTimeUnit): String = when (type) {
    AbilityDuration.TURNS -> "${value.coerceAtLeast(0)} turnos"
    AbilityDuration.TIME -> "${value.coerceAtLeast(0)} ${unit.label.lowercase()}"
    else -> type.label
}

fun Character.isPowerActive(power: Power): Boolean = when (power.canonicalSource) {
    AbilitySource.ITEM -> inventory.any {
        it.id == power.linkedItemId && !it.isScrap && !it.isBroken &&
            it.inventoryState in setOf(InventoryState.EQUIPPED, InventoryState.WIELDED)
    }
    else -> power.active
}

fun Character.activePowerModifiers(): List<Pair<Power, AbilityModifier>> = powers
    .filter { it.executionType == AbilityExecution.PASSIVE && it.grantsPermanentBonus && isPowerActive(it) }
    .flatMap { power -> power.modifiers.map { power to it } }

fun Character.constrainedToResourceMaximums(): Character = copy(
    life = life.copy(current = life.current.coerceIn(0, lifeMaximum)),
    sanity = sanity.copy(current = sanity.current.coerceIn(0, sanityMaximum)),
    arcane = arcane.copy(current = arcane.current.coerceIn(0, arcaneMaximum)),
    energy = energy.copy(current = energy.current.coerceIn(0, energyMaximum)),
    destiny = destiny.copy(current = destiny.current.coerceIn(0, destinyMaximum)),
)
