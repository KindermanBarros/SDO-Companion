package com.kinderman.sdo.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DivineLinkKind(val capacityCost: Int) { ANCHOR(1), AVATAR(2) }

/** List order is creation order; unsupported links suspend newest first. */
@Serializable
data class DivineLink(
    val id: String = "",
    val kind: DivineLinkKind = DivineLinkKind.ANCHOR,
    val active: Boolean = true,
)

@Serializable
enum class DivineFate { ACTIVE, DEFEATED, SEALED, ERASED }

fun Character.currentDivinePillars(): DivinePillars =
    DivinePillarKind.entries.fold(DivinePillars()) { result, pillar ->
        result.with(pillar, divineAscension.pillars.get(pillar) -
            (divineRuntime.pillarSuppression.get(pillar) +
                divineRuntime.scenePillarSuppression.get(pillar)).coerceAtMost(2))
    }

fun Character.divineLinkCapacity(): Int = 2 * currentDivinePillars().presenca

fun Character.divineLinkLoad(): Int =
    divineRuntime.links.filter { it.active }.sumOf { it.kind.capacityCost }

fun Character.divineLinkOverloadCost(): Int =
    (divineLinkLoad() - divineLinkCapacity()).coerceAtLeast(0)

/** Charges only the additional overload needed in this scene; never refunds or creates debt. */
fun Character.settleDivineLinks(sceneId: String): Character {
    if (sceneId.isBlank()) return this
    val paid = divineRuntime.linkScenePayments[sceneId]?.coerceIn(0, 2) ?: 0
    val allowance = if (divineLinkCapacity() == 0) 0 else
        minOf(2, paid + destiny.current.coerceIn(0, 2))
    var load = divineLinkLoad()
    val links = divineRuntime.links.toMutableList()
    for (index in links.indices.reversed()) {
        if (load <= divineLinkCapacity() + allowance) break
        if (links[index].active) {
            load -= links[index].kind.capacityCost
            links[index] = links[index].copy(active = false)
        }
    }
    val cost = ((load - divineLinkCapacity()).coerceAtLeast(0) - paid).coerceAtLeast(0)
    return copy(
        destiny = destiny.copy(current = destiny.current - cost),
        divineRuntime = divineRuntime.copy(
            links = links,
            pdSpentInCurrentScene = divineRuntime.pdSpentInCurrentScene + cost,
            linkScenePayments = divineRuntime.linkScenePayments + (sceneId to (paid + cost)),
        ),
    )
}

fun Character.withDivineLink(link: DivineLink, sceneId: String): Character {
    if (divineLinkValidationError(link, sceneId) != null) return this
    val existing = divineRuntime.links.indexOfFirst { it.id == link.id }
    val links = divineRuntime.links.toMutableList()
    if (existing < 0) links.add(link) else links[existing] = link
    return copy(divineRuntime = divineRuntime.copy(links = links)).settleDivineLinks(sceneId)
}

fun Character.withRemovedDivineLink(linkId: String): Character = copy(
    divineRuntime = divineRuntime.copy(links = divineRuntime.links.filterNot { it.id == linkId }),
)

fun Character.suppressDivinePillar(
    pillar: DivinePillarKind,
    amount: Int,
    causeId: String,
    sceneId: String,
    sceneOnly: Boolean = false,
): Character {
    if (divineSuppressionValidationError(amount, causeId, sceneId) != null) return this
    val suppression = if (sceneOnly) divineRuntime.scenePillarSuppression else
        divineRuntime.pillarSuppression
    val next = suppression.with(pillar, (suppression.get(pillar) + amount).coerceAtMost(2))
    return copy(divineRuntime = divineRuntime.copy(
        pillarSuppression = if (sceneOnly) divineRuntime.pillarSuppression else next,
        scenePillarSuppression = if (sceneOnly) next else divineRuntime.scenePillarSuppression,
        sceneSuppressionCauseIds = if (sceneOnly) divineRuntime.sceneSuppressionCauseIds + causeId
            else divineRuntime.sceneSuppressionCauseIds,
        suppressionCauseIds = divineRuntime.suppressionCauseIds + causeId,
    )).settleDivineLinks(sceneId)
}

fun Character.restoreDivinePillar(
    pillar: DivinePillarKind,
    sceneId: String,
    sessionId: String,
    causeResolved: Boolean = false,
): Character {
    if (divineRestorationValidationError(pillar, sceneId, sessionId, causeResolved) != null) return this
    return copy(divineRuntime = divineRuntime.copy(
        pillarSuppression = divineRuntime.pillarSuppression.with(
            pillar, divineRuntime.pillarSuppression.get(pillar) - 1,
        ),
        restorativeSceneIds = divineRuntime.restorativeSceneIds + sceneId,
        restorationSessionIds = divineRuntime.restorationSessionIds + sessionId,
    ))
}

/** Permanent changes retain the existing GM authorization boundary on divineAscension. */
fun Character.changePermanentDivinePillar(
    pillar: DivinePillarKind,
    delta: Int,
    arcId: String,
    milestoneId: String,
    sceneId: String,
): Character {
    if (permanentDivinePillarValidationError(pillar, delta, arcId, milestoneId, sceneId) != null) {
        return this
    }
    val value = divineAscension.pillars.get(pillar) + delta
    return copy(
        divineAscension = divineAscension.copy(pillars = divineAscension.pillars.with(pillar, value)),
        divineRuntime = divineRuntime.copy(
            growthArcIds = divineRuntime.growthArcIds + if (delta > 0) setOf(arcId) else emptySet(),
            lossArcIds = divineRuntime.lossArcIds + if (delta < 0) setOf(arcId) else emptySet(),
            pillarMilestoneIds = divineRuntime.pillarMilestoneIds + milestoneId,
        ),
    ).settleDivineLinks(sceneId)
}

/** Defeat and sealing never imply death. Vulnerability IDs attest GM-established fiction. */
fun Character.withDivineFate(
    fate: DivineFate,
    causeId: String,
    establishedVulnerabilityId: String = "",
): Character {
    if (divineFateValidationError(fate, causeId, establishedVulnerabilityId) != null) return this
    return copy(divineRuntime = divineRuntime.copy(
        divineFate = fate,
        links = if (fate == DivineFate.SEALED) {
            divineRuntime.links.map { it.copy(active = false) }
        } else divineRuntime.links,
        divineFateCauseIds = divineRuntime.divineFateCauseIds + causeId,
    ))
}

/** Returns attacker and target unchanged unless a Greater God pays 7 PD against a vulnerable Lesser. */
fun Character.eraseLesserDeity(
    target: Character,
    causeId: String,
    establishedVulnerabilityId: String,
    contestResolved: Boolean = false,
    trueEssenceAccessible: Boolean = false,
    safeguardsOvercome: Boolean = false,
): Pair<Character, Character> {
    if (divineErasureValidationError(target, causeId, establishedVulnerabilityId,
            contestResolved, trueEssenceAccessible, safeguardsOvercome) != null) return this to target
    return copy(
        destiny = destiny.copy(current = destiny.current - 7),
        divineRuntime = divineRuntime.copy(
            pdSpentInCurrentScene = divineRuntime.pdSpentInCurrentScene + 7,
        ),
    ) to target.copy(divineRuntime = target.divineRuntime.copy(
        divineFate = DivineFate.ERASED,
        links = target.divineRuntime.links.map { it.copy(active = false) },
        divineFateCauseIds = target.divineRuntime.divineFateCauseIds + causeId,
    ))
}

/** Defeat is banishment, not death; metaphysical acts remain available. */
fun Character.canPerformDivineActs(): Boolean =
    divineRuntime.divineFate != DivineFate.SEALED &&
        divineRuntime.divineFate != DivineFate.ERASED

fun Character.canDelegateDivinePower(): Boolean = canPerformDivineActs()

fun Character.divineLinkValidationError(link: DivineLink, sceneId: String): String? = when {
    !canDelegateDivinePower() -> "A deidade não pode delegar poder neste estado."
    link.id.isBlank() || sceneId.isBlank() -> "Informe o vínculo e a cena."
    else -> null
}

fun Character.divineFateValidationError(
    fate: DivineFate,
    causeId: String,
    establishedVulnerabilityId: String = "",
): String? = when {
    causeId.isBlank() -> "Informe a causa narrativa."
    causeId in divineRuntime.divineFateCauseIds -> "Esta causa já foi aplicada."
    divineRuntime.divineFate == DivineFate.ERASED -> "A essência foi apagada."
    fate == DivineFate.ERASED -> "O apagamento exige conflito divino e acesso à essência."
    fate == DivineFate.SEALED && establishedVulnerabilityId.isBlank() ->
        "O selamento exige uma vulnerabilidade estabelecida."
    else -> null
}

fun Character.divineSuppressionValidationError(
    amount: Int,
    causeId: String,
    sceneId: String,
): String? = when {
    amount !in 1..2 -> "A supressão deve ser de 1 ou 2 pontos."
    causeId.isBlank() || sceneId.isBlank() -> "Informe a causa e a cena."
    causeId in divineRuntime.suppressionCauseIds -> "Esta causa já foi aplicada."
    else -> null
}

fun Character.divineRestorationValidationError(
    pillar: DivinePillarKind,
    sceneId: String,
    sessionId: String,
    causeResolved: Boolean = false,
): String? = when {
    !causeResolved -> "O Mestre deve confirmar a resolução da causa."
    sceneId.isBlank() || sessionId.isBlank() -> "Informe a cena e a sessão."
    sceneId in divineRuntime.restorativeSceneIds -> "Esta cena já restaurou um Pilar."
    sessionId in divineRuntime.restorationSessionIds -> "A restauração desta sessão já foi usada."
    divineRuntime.pillarSuppression.get(pillar) == 0 -> "Este Pilar não está suprimido."
    else -> null
}

fun Character.permanentDivinePillarValidationError(
    pillar: DivinePillarKind,
    delta: Int,
    arcId: String,
    milestoneId: String,
    sceneId: String,
): String? {
    val cap = when (divineAscension.tier) {
        DivineTier.LESSER_GOD -> 4
        DivineTier.GREATER_GOD -> 5
        else -> return "Apenas deuses Menores e Maiores usam progressão de Pilares."
    }
    val usedArcs = if (delta > 0) divineRuntime.growthArcIds else divineRuntime.lossArcIds
    return when {
        delta != 1 && delta != -1 -> "A mudança deve ser de 1 ponto."
        arcId.isBlank() || milestoneId.isBlank() || sceneId.isBlank() ->
            "Informe o arco, o marco narrativo e a cena."
        arcId in usedArcs -> "A mudança deste arco já foi usada."
        milestoneId in divineRuntime.pillarMilestoneIds -> "Este marco já foi usado."
        divineAscension.pillars.get(pillar) + delta !in 0..cap ->
            "A mudança excede os limites da categoria."
        else -> null
    }
}

fun Character.divineErasureValidationError(
    target: Character,
    causeId: String,
    establishedVulnerabilityId: String,
    contestResolved: Boolean = false,
    trueEssenceAccessible: Boolean = false,
    safeguardsOvercome: Boolean = false,
): String? = when {
    !contestResolved || !trueEssenceAccessible || !safeguardsOvercome ->
        "Resolva o conflito, alcance a essência e supere suas salvaguardas."
    !canPerformDivineActs() -> "A deidade não pode realizar atos neste estado."
    divineAscension.tier != DivineTier.GREATER_GOD ||
        target.divineAscension.tier != DivineTier.LESSER_GOD ->
        "O apagamento exige um Deus Maior contra um Deus Menor."
    destiny.current < 7 -> "O apagamento exige 7 PD."
    causeId.isBlank() || establishedVulnerabilityId.isBlank() ->
        "Informe a causa e a vulnerabilidade estabelecida."
    target.divineRuntime.divineFate == DivineFate.ERASED ||
        causeId in target.divineRuntime.divineFateCauseIds -> "Este apagamento já foi resolvido."
    else -> null
}
