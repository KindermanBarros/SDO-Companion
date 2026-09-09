package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.MysticAbility
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.PowerSourceType

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
    limit = extractLimit(summary),
    effect = mechanicalEffect.ifBlank { summary },
    category = group,
    prerequisites = prerequisites,
    activationCondition = "",
    enhancements = extractEnhancement(summary),
    deactivationCondition = "",
    ruleReference = ruleReference,
    sourceType = sourceType,
    sourceId = sourceId,
    catalogEntryId = id,
    catalogVersion = version,
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
)

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
        cost = cost,
        action = action,
        range = range,
        duration = duration,
        limit = limit,
        effect = mainEffect,
        category = category,
        prerequisites = emptyList(),
        activationCondition = activation,
        enhancements = enhancement,
        deactivationCondition = deactivation,
        ruleReference = path.ruleReference,
        sourceType = PowerSourceType.PATH,
        sourceId = path.id,
        catalogEntryId = path.id,
        catalogVersion = path.version,
    )
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
