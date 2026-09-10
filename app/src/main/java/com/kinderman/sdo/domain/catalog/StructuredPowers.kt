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
    canonicalSource = sourceType.toCanonicalSource(),
    costType = canonicalCostType(cost),
    costValue = canonicalCostValue(cost),
    executionType = canonicalExecution(action),
    rangeType = canonicalRange(range),
    durationType = canonicalDuration(duration),
    durationValue = canonicalDurationValue(duration),
    durationUnit = canonicalDurationUnit(duration),
    resistance = canonicalResistance(listOf(summary, mechanicalEffect).joinToString("\n")),
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

private fun PathPower.toStructuredPower(path: CatalogEntry): Power {
    val normalized = effect.replace("\r\n", "\n")
    val category = Regex("(?im)^Categoria:\\s*(.+)$").find(normalized)?.groupValues?.get(1)?.trim().orEmpty()
    val limit = extractLimit(normalized)
    val enhancement = extractEnhancement(normalized)
    val cost = extractCost(normalized)
    val action = extractAction(normalized)
    val range = extractRange(normalized)
    val duration = extractDuration(normalized)
    val activation = extractActivation(normalized)
    val deactivation = extractDeactivation(normalized)
    val enhancementStart = Regex("(?im)^Aprimoramento\\s*[—-]").find(normalized)?.range?.first ?: normalized.length
    val mainEffect = normalized
        .substring(0, enhancementStart)
        .lines()
        .filterNot { line ->
            line.startsWith("Origem:", true) || line.startsWith("Categoria:", true)
        }
        .joinToString("\n")
        .trim()

    return Power(
        name = name,
        origin = "Caminho — ${path.name}",
        cost = cost.ifBlank { "Sem custo adicional expresso; consulte as condições do efeito" },
        action = action.ifBlank { "Vinculada à ação ou condição descrita no efeito" },
        range = range.ifBlank { "Alvo ou situação descrita no efeito" },
        duration = duration.ifBlank { "Enquanto a condição descrita no efeito se aplicar" },
        limit = limit.ifBlank { "Sem limite adicional expresso" },
        effect = mainEffect,
        category = category.ifBlank { "Poder de Caminho" },
        prerequisites = emptyList(),
        activationCondition = activation.ifBlank { "Conforme condição descrita no efeito" },
        enhancements = enhancement.ifBlank { "Sem aprimoramento publicado" },
        deactivationCondition = deactivation.ifBlank { "Quando encerrar a condição ou duração do efeito" },
        ruleReference = path.ruleReference,
        sourceType = PowerSourceType.PATH,
        sourceId = path.id,
        catalogEntryId = path.id,
        catalogVersion = path.version,
        canonicalSource = AbilitySource.PATH,
        costType = canonicalCostType(cost),
        costValue = canonicalCostValue(cost),
        executionType = canonicalExecution(action),
        rangeType = canonicalRange(range),
        durationType = canonicalDuration(duration),
        durationValue = canonicalDurationValue(duration),
        durationUnit = canonicalDurationUnit(duration),
        resistance = canonicalResistance(mainEffect),
    )
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

private fun extractCost(text: String): String {
    val patterns = listOf(
        Regex("(?i)(\\d+d\\d+\\s+HP)"),
        Regex("(?i)(\\d+\\s*(?:PE|PM|HP|Energia|Arcano|Destino))"),
    )
    return patterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.get(1)?.trim() }.orEmpty()
}

private fun extractAction(text: String): String = when {
    Regex("(?i)1\\s+ação\\s+completa").containsMatchIn(text) -> "1 ação completa"
    Regex("(?i)1\\s+ação").containsMatchIn(text) -> "1 ação"
    Regex("(?i)reação").containsMatchIn(text) -> "Reação"
    else -> ""
}

private fun extractRange(text: String): String {
    val meters = Regex("(?i)(?:a até|até|a)\\s+(\\d+)\\s+metros?").find(text)?.groupValues?.get(1)
    return when {
        meters != null -> "$meters metros"
        Regex("(?i)toque|tocar|contato físico").containsMatchIn(text) -> "Toque"
        Regex("(?i)em si|você recebe|próprio corpo").containsMatchIn(text) -> "Pessoal"
        else -> ""
    }
}

private fun extractDuration(text: String): String {
    val explicit = Regex("(?i)(?:dura|até)\\s+(?:o|a)?\\s*(fim[^.\\n]*|próximo descanso|final da cena|final do dia)")
        .find(text)?.value?.trim()
    return explicit.orEmpty()
}

private fun extractLimit(text: String): String {
    val match = Regex("(?i)(uma vez|duas vezes|\\d+ vezes)\\s+por\\s+(turno|cena|dia|descanso|sessão)").find(text)
    return match?.value?.replaceFirstChar { it.uppercase() }.orEmpty()
}

private fun extractEnhancement(text: String): String {
    val marker = Regex("(?im)^Aprimoramento\\s*[—-]\\s*(.*)$").find(text) ?: return ""
    return text.substring(marker.range.first).trim()
}

private fun extractActivation(text: String): String {
    val match = Regex("(?im)^(quando|enquanto|ao |durante |se )(.+)$").find(text) ?: return ""
    return match.value.trim().take(240)
}

private fun extractDeactivation(text: String): String {
    val match = Regex("(?im)^(?:o poder |este poder )?(?:termina|não funciona|se desfaz|é encerrado)(.+)$").find(text)
    return match?.value?.trim().orEmpty()
}
