package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.MysticAbility
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.PowerSourceType
import com.kinderman.sdo.domain.model.AbilityCostType
import com.kinderman.sdo.domain.model.AbilityDuration
import com.kinderman.sdo.domain.model.AbilityExecution
import com.kinderman.sdo.domain.model.AbilityRange
import com.kinderman.sdo.domain.model.AbilityResistance
import com.kinderman.sdo.domain.model.AbilitySource
import com.kinderman.sdo.domain.model.AshPurity
import com.kinderman.sdo.domain.model.AshSource
import com.kinderman.sdo.domain.model.canonicalized
import com.kinderman.sdo.domain.model.AbilityTimeUnit

data class PathChangePreview(
    val pathName: String,
    val removed: List<Power>,
    val added: List<Power>,
)

fun Character.previewPathChange(entry: CatalogEntry): PathChangePreview {
    val preset = PathPresets.find(entry.id)
    val removed = powers.filter { power ->
        power.sourceType == PowerSourceType.PATH || power.origin.startsWith("Caminho — ", ignoreCase = true)
    }
    val added = preset?.powers.orEmpty().map { pathPower ->
        pathPower.toStructuredPower(entry)
    }
    return PathChangePreview(entry.name, removed, added)
}

fun Character.withStructuredPathPreset(entry: CatalogEntry): Character {
    val preset = PathPresets.find(entry.id) ?: return copy(pathName = entry.name)
    val preview = previewPathChange(entry)
    val retainedPowers = powers.filterNot { it in preview.removed }
    return copy(
        pathName = entry.name,
        pathMotto = preset.motto,
        pathKeywords = preset.keywords,
        pathPillars = preset.pillars,
        powers = retainedPowers + preview.added,
    )
}

fun CatalogEntry.toStructuredPower(
    sourceType: PowerSourceType = PowerSourceType.CATALOG,
    sourceId: String = id,
): Power = Power(
    name = name,
    origin = listOf(group, source).filter(String::isNotBlank).joinToString(" — "),
    cost = cost,
    action = action,
    range = range,
    duration = duration,
    limit = limit,
    effect = mechanicalEffect.ifBlank { summary },
    category = group,
    prerequisites = prerequisites,
    activationCondition = activationCondition,
    enhancements = enhancements,
    deactivationCondition = deactivationCondition,
    ruleReference = ruleReference,
    sourceType = sourceType,
    sourceId = sourceId,
    catalogEntryId = id,
    catalogVersion = version,
    canonicalSource = abilitySource ?: sourceType.toCanonicalSource(),
    knowledgeLevel = sourceLevel,
    costType = abilityCostType ?: canonicalCostType(cost),
    costValue = abilityCostValue ?: canonicalCostValue(cost),
    executionType = abilityExecution ?: canonicalExecution(action),
    timeValue = executionValue,
    timeUnit = executionUnit,
    rangeType = abilityRange ?: canonicalRange(range),
    targetArea = targetArea,
    durationType = abilityDuration ?: canonicalDuration(duration),
    durationValue = durationValue.takeIf { abilityDuration != null } ?: canonicalDurationValue(duration),
    durationUnit = durationUnit.takeIf { abilityDuration != null } ?: canonicalDurationUnit(duration),
    resistance = abilityResistance ?: canonicalResistance(listOf(summary, mechanicalEffect).joinToString("\n")),
)

fun CatalogEntry.toMysticAbility(): MysticAbility = MysticAbility(
    type = when (kind) {
        CatalogKind.MAGIC -> "Magia"
        CatalogKind.ASH -> "Cinza"
        CatalogKind.RUNE -> "Runa"
        else -> kind.name
    },
    name = name,
    cost = cost,
    action = action,
    range = range,
    duration = duration,
    effect = mechanicalEffect.ifBlank { summary },
    category = group,
    source = source,
    ruleReference = ruleReference,
    catalogEntryId = id,
    catalogVersion = version,
    canonicalSource = abilitySource ?: AbilitySource.NARRATIVE,
    knowledgeLevel = sourceLevel,
    costType = abilityCostType ?: canonicalCostType(cost),
    costValue = abilityCostValue ?: canonicalCostValue(cost),
    executionType = abilityExecution ?: canonicalExecution(action),
    timeValue = executionValue,
    timeUnit = executionUnit,
    rangeType = abilityRange ?: canonicalRange(range),
    targetArea = targetArea,
    durationType = abilityDuration ?: canonicalDuration(duration),
    durationValue = durationValue.takeIf { abilityDuration != null } ?: canonicalDurationValue(duration),
    durationUnit = durationUnit.takeIf { abilityDuration != null } ?: canonicalDurationUnit(duration),
    resistance = abilityResistance ?: canonicalResistance(listOf(summary, mechanicalEffect).joinToString("\n")),
    ashSource = catalogAshSource
        ?: AshSource.entries.firstOrNull { ash -> group.substringBefore('/').trim().equals(ash.label, true) }
        ?: AshSource.FIRE,
    ashPurity = catalogAshPurity
        ?: AshPurity.entries.firstOrNull { purity -> group.substringAfter('/', "").trim().equals(purity.label, true) }
        ?: AshPurity.RAW,
)

fun CatalogEntry.toMysticAbility(character: Character): MysticAbility {
    val ability = toMysticAbility()
    if (ability.canonicalSource != AbilitySource.KNOWLEDGE) return ability
    val requiredLevel = sourceLevel ?: 0
    val knowledge = (character.learnedKnowledges + character.arcaneKnowledges + character.battleTechniques)
        .firstOrNull { com.kinderman.sdo.domain.model.normalizeAbilityName(it.name) == com.kinderman.sdo.domain.model.normalizeAbilityName(sourceKnowledge) }
    require(knowledge != null && knowledge.value >= requiredLevel) {
        "Conhecimento necessário: $sourceKnowledge $requiredLevel"
    }
    return ability.copy(knowledgeId = knowledge.id, knowledgeLevel = requiredLevel)
}

internal fun PathPower.toStructuredPower(path: CatalogEntry): Power = Power(
    name = name,
    origin = "Caminho — ${path.name}",
    cost = cost,
    action = action,
    range = range,
    duration = duration,
    limit = limit,
    effect = effect,
    category = category,
    prerequisites = emptyList(),
    activationCondition = activationCondition,
    enhancements = enhancements,
    deactivationCondition = deactivationCondition,
    ruleReference = path.ruleReference,
    sourceType = PowerSourceType.PATH,
    sourceId = path.id,
    catalogEntryId = path.id,
    catalogVersion = path.version,
    canonicalSource = AbilitySource.PATH,
    costType = costType,
    costValue = costValue,
    destinyCostEligible = destinyCostEligible,
    executionType = executionType,
    rangeType = rangeType,
    durationType = durationType,
    durationValue = durationValue,
    durationUnit = durationUnit,
    resistance = resistance,
)

fun Character.withRefreshedPresetPowers(): Character {
    val currentPathPreset = PathPresets.entries.firstOrNull { preset ->
        preset.catalogId == powers.firstOrNull { it.sourceType == PowerSourceType.PATH }?.sourceId ||
            BuiltInCatalog.entries.any { entry ->
                entry.id == preset.catalogId && entry.name.equals(pathName, ignoreCase = true)
            }
    }
    val currentPathEntry = currentPathPreset?.let { preset ->
        BuiltInCatalog.entries.firstOrNull { it.id == preset.catalogId }
    }
    val racialPresets = buildList {
        RaceCatalog.race(race)?.powers?.let(::addAll)
        RaceCatalog.subRaces.firstOrNull { it.name.equals(subRace, ignoreCase = true) }?.powers?.let(::addAll)
    }

    return copy(powers = powers.map { stored ->
        val refreshed = when {
            stored.sourceType == PowerSourceType.PATH ||
                stored.origin.startsWith("Caminho — ", ignoreCase = true) -> {
                val preset = currentPathPreset?.powers?.firstOrNull { it.name.equals(stored.name, ignoreCase = true) }
                if (preset != null && currentPathEntry != null) preset.toStructuredPower(currentPathEntry) else null
            }
            stored.sourceType == PowerSourceType.RACE ||
                stored.origin.startsWith("Raça — ", ignoreCase = true) ||
                stored.origin.startsWith("Sub-raça — ", ignoreCase = true) -> {
                racialPresets.firstOrNull { it.name.equals(stored.name, ignoreCase = true) }
                    ?.toStructuredPower(stored.origin)
            }
            else -> null
        }

        refreshed?.copy(
            id = stored.id,
            favorite = stored.favorite,
            available = stored.available,
            active = stored.active,
            linkedItemId = stored.linkedItemId,
            revision = maxOf(stored.revision, refreshed.revision),
        )?.canonicalized() ?: stored.canonicalized()
    })
}

private fun PowerSourceType.toCanonicalSource(): AbilitySource = when (this) {
    PowerSourceType.PATH -> AbilitySource.PATH
    PowerSourceType.RACE -> AbilitySource.RACE
    PowerSourceType.ITEM -> AbilitySource.ITEM
    PowerSourceType.KNOWLEDGE -> AbilitySource.KNOWLEDGE
    else -> AbilitySource.NARRATIVE
}

private fun canonicalCostValue(value: String): Int = Regex("\\d+").find(value)?.value?.toIntOrNull() ?: 0

private fun canonicalCostType(value: String): AbilityCostType = when {
    value.contains("Arcano", true) || value.contains("PM", true) -> AbilityCostType.ARCANE
    value.contains("Energia", true) || value.contains("PE", true) -> AbilityCostType.ENERGY
    value.contains("Destino", true) -> AbilityCostType.DESTINY
    value.contains("Sanidade", true) || value.contains("PS", true) -> AbilityCostType.SANITY
    value.contains("Vida", true) || value.contains("PV", true) || value.contains("HP", true) -> AbilityCostType.LIFE
    else -> AbilityCostType.NONE
}

private fun canonicalExecution(value: String): AbilityExecution = when {
    value.contains("Reação", true) -> AbilityExecution.REACTION
    value.contains("Turno", true) -> AbilityExecution.TURN
    value.contains("Livre", true) -> AbilityExecution.FREE
    value.contains("Passiv", true) -> AbilityExecution.PASSIVE
    value.contains("Minuto", true) || value.contains("Hora", true) || value.contains("Dia", true) -> AbilityExecution.TIME
    else -> AbilityExecution.ACTION
}

private fun canonicalRange(value: String): AbilityRange = when {
    value.contains("Indefin", true) -> AbilityRange.INDEFINITE
    value.contains("Pessoal", true) || value.contains("Toque", true) -> AbilityRange.PERSONAL
    Regex("(?:9|[1-8])\\s*m", RegexOption.IGNORE_CASE).containsMatchIn(value) -> AbilityRange.SHORT
    Regex("(?:[12]\\d|30)\\s*m", RegexOption.IGNORE_CASE).containsMatchIn(value) -> AbilityRange.MEDIUM
    else -> AbilityRange.LONG
}

private fun canonicalDuration(value: String): AbilityDuration = when {
    value.contains("Turno", true) -> AbilityDuration.TURNS
    value.contains("Cena", true) -> AbilityDuration.SCENE
    value.contains("Sessão", true) -> AbilityDuration.SESSION
    value.contains("Instant", true) -> AbilityDuration.INSTANT
    else -> AbilityDuration.TIME
}

private fun canonicalDurationValue(value: String): Int = Regex("\\d+").find(value)?.value?.toIntOrNull() ?: 0

private fun canonicalDurationUnit(value: String): AbilityTimeUnit = when {
    value.contains("dia", true) -> AbilityTimeUnit.DAYS
    value.contains("hora", true) -> AbilityTimeUnit.HOURS
    else -> AbilityTimeUnit.HOURS
}

private fun canonicalResistance(value: String): AbilityResistance = when {
    value.contains("Proteção Geral", true) -> AbilityResistance.GENERAL
    value.contains("Proteção de Esquiva", true) -> AbilityResistance.DODGE
    value.contains("Proteção de Postura", true) -> AbilityResistance.POSTURE
    value.contains("Proteção Mental", true) -> AbilityResistance.MENTAL
    value.contains("Proteção Arcana", true) -> AbilityResistance.ARCANE
    else -> AbilityResistance.NONE
}
