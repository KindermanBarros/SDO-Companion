package com.kinderman.sdo.domain.creation

import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.AbilityCost
import com.kinderman.sdo.domain.model.AbilityKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterDivineAscension
import com.kinderman.sdo.domain.model.DivineAscensionActDraft
import com.kinderman.sdo.domain.model.DivineAscensionDraft
import com.kinderman.sdo.domain.model.DivineHeraldGiftDraft
import com.kinderman.sdo.domain.model.DivinePassiveCapability
import com.kinderman.sdo.domain.model.DivinePillarKind
import com.kinderman.sdo.domain.model.DivinePillars
import com.kinderman.sdo.domain.model.DivineRules
import com.kinderman.sdo.domain.model.DivineTier
import com.kinderman.sdo.domain.model.Duration
import com.kinderman.sdo.domain.model.Execution
import com.kinderman.sdo.domain.model.NarrativeSourceId
import com.kinderman.sdo.domain.model.RangeSpec
import com.kinderman.sdo.domain.model.Source
import com.kinderman.sdo.domain.model.SpendableResource
import com.kinderman.sdo.domain.model.allCanonicalAbilitiesSafely
import com.kinderman.sdo.domain.model.withDivineAscension

object DivineAscensionCreation {
    const val STEP_COUNT = 4
    const val LESSER_GOD_PILLAR_BUDGET = 8
    const val GREATER_GOD_PILLAR_BONUS = 4

    val Character.hasPendingDivinePromotion: Boolean
        get() = divineAscension.pendingPromotionTier in setOf(DivineTier.LESSER_GOD, DivineTier.GREATER_GOD)

    fun targetTier(character: Character): DivineTier? =
        character.divineAscension.pendingPromotionTier?.takeIf {
            it == DivineTier.LESSER_GOD || it == DivineTier.GREATER_GOD
        }

    fun initialDraftFor(character: Character, targetTier: DivineTier): DivineAscensionDraft {
        val current = character.divineAscension
        val basePillars = if (targetTier == DivineTier.GREATER_GOD) current.pillars else DivinePillars()
        val seededPassives = if (current.passiveCapabilities.size >= 2) {
            current.passiveCapabilities
        } else {
            val missing = (2 - current.passiveCapabilities.size).coerceAtLeast(0)
            current.passiveCapabilities + List(missing) { DivinePassiveCapability() }
        }
        val existingPdActs = character.allCanonicalAbilitiesSafely()
            .filter { ability ->
                ability.kind == AbilityKind.POWER &&
                    ability.execution.kind != com.kinderman.sdo.domain.model.ExecutionKind.Passive &&
                    (ability.cost as? AbilityCost.PowerCost)?.resource == SpendableResource.PD
            }
            .map { ability ->
                DivineAscensionActDraft(
                    id = ability.id.removePrefix("divine-act:"),
                    name = ability.name,
                    costPd = ability.cost.amount,
                    executionKind = ability.execution.kind,
                    rangeBand = ability.range.band,
                    durationKind = ability.duration?.kind ?: com.kinderman.sdo.domain.model.DurationKind.Scene,
                    description = ability.effect,
                )
            }
        return DivineAscensionDraft(
            step = 1,
            dominio = current.dominio,
            santuario = current.santuario,
            postura = current.postura,
            linhaDePensamento = current.linhaDePensamento,
            tabu = current.tabu,
            materialRestrictionNote = if (targetTier == DivineTier.GREATER_GOD) {
                current.materialRestrictionNote.ifBlank {
                    "Sem permanência física livre em Nulvara; atua via Âncoras, Avatares, Santuários ou Intervenção Direta (5 PD)."
                }
            } else {
                ""
            },
            pillars = basePillars,
            passiveCapabilities = seededPassives,
            domainActs = existingPdActs.ifEmpty { listOf(DivineAscensionActDraft(costPd = 1)) },
            heraldGift = current.heraldGift,
        )
    }

    /** GM/Admin unlocks the guided promotion wizard on a character sheet. */
    fun unlockPromotion(character: Character, targetTier: DivineTier): Character {
        require(targetTier == DivineTier.LESSER_GOD || targetTier == DivineTier.GREATER_GOD) {
            "Apenas promoções a Deus Menor ou Deus Maior usam o fluxo de criação divina."
        }
        if (targetTier == DivineTier.GREATER_GOD) {
            require(character.divineAscension.tier == DivineTier.LESSER_GOD) {
                "A promoção a Deus Maior exige que o personagem já seja um Deus Menor."
            }
        } else {
            require(character.divineAscension.tier in setOf(DivineTier.MORTAL, DivineTier.AVATAR, DivineTier.DEMIGOD)) {
                "A promoção a Deus Menor aplica-se a Mortais, Avatares ou Semideuses."
            }
        }
        val draft = character.divineRuntime.ascensionDraft ?: initialDraftFor(character, targetTier)
        return character.copy(
            divineAscension = character.divineAscension.copy(pendingPromotionTier = targetTier),
            divineRuntime = character.divineRuntime.copy(ascensionDraft = draft.copy(step = draft.step.coerceIn(1, STEP_COUNT))),
        )
    }

    /** GM/Admin cancels an unlocked promotion before completion. */
    fun cancelPromotion(character: Character): Character = character.copy(
        divineAscension = character.divineAscension.copy(pendingPromotionTier = null),
        divineRuntime = character.divineRuntime.copy(ascensionDraft = null),
    )

    fun currentDraft(character: Character): DivineAscensionDraft {
        val target = targetTier(character) ?: DivineTier.LESSER_GOD
        return character.divineRuntime.ascensionDraft ?: initialDraftFor(character, target)
    }

    fun updateDraft(character: Character, draft: DivineAscensionDraft): Character = character.copy(
        divineRuntime = character.divineRuntime.copy(
            ascensionDraft = draft.copy(step = draft.step.coerceIn(1, STEP_COUNT)),
        ),
    )

    fun requiredTotalPillars(character: Character, targetTier: DivineTier): Int = when (targetTier) {
        DivineTier.LESSER_GOD -> LESSER_GOD_PILLAR_BUDGET
        DivineTier.GREATER_GOD -> character.divineAscension.pillars.totalPoints + GREATER_GOD_PILLAR_BONUS
        else -> 0
    }

    fun maxPerPillar(targetTier: DivineTier): Int = if (targetTier == DivineTier.LESSER_GOD) 4 else 5

    fun minForPillar(character: Character, targetTier: DivineTier, pillar: DivinePillarKind): Int =
        if (targetTier == DivineTier.GREATER_GOD) character.divineAscension.pillars.get(pillar) else 0

    fun validateStep(step: Int, character: Character, draft: DivineAscensionDraft = currentDraft(character)): String? {
        val target = targetTier(character) ?: return "Nenhuma promoção divina pendente."
        return when (step) {
            1 -> when {
                draft.dominio.isBlank() -> "Defina o Domínio governado pela divindade."
                draft.santuario.isBlank() -> "Defina o Santuário, território ou âncora planar."
                draft.postura.isBlank() -> "Defina a Postura diante do Domínio (ex.: Preservação, Cobrança, Barganha)."
                draft.linhaDePensamento.isBlank() -> "Defina a Linha de Pensamento da divindade."
                draft.tabu.isBlank() -> "Defina o Tabu mecânico (violar custa 2 PD e bloqueia Atos na cena)."
                target == DivineTier.GREATER_GOD && draft.materialRestrictionNote.isBlank() ->
                    "Confirme a Restrição Material do Deus Maior."
                else -> null
            }

            2 -> {
                val cap = maxPerPillar(target)
                val expectedTotal = requiredTotalPillars(character, target)
                val belowPrevious = target == DivineTier.GREATER_GOD && DivinePillarKind.entries.any {
                    draft.pillars.get(it) < character.divineAscension.pillars.get(it)
                }
                val aboveCap = DivinePillarKind.entries.any { draft.pillars.get(it) !in 0..cap }
                when {
                    aboveCap -> "Cada Pilar deve permanecer entre 0 e $cap para ${target.label}."
                    belowPrevious -> "Na ascensão a Deus Maior, nenhum Pilar anterior pode ser reduzido."
                    draft.pillars.totalPoints != expectedTotal ->
                        if (target == DivineTier.LESSER_GOD) {
                            "Distribua exatamente 8 pontos entre os 4 Pilares Divinos (atual: ${draft.pillars.totalPoints}/8)."
                        } else {
                            "Distribua exatamente +4 pontos adicionais nos Pilares Divinos (total esperado: $expectedTotal, atual: ${draft.pillars.totalPoints})."
                        }
                    else -> null
                }
            }

            3 -> {
                val validPassives = draft.passiveCapabilities.filter { it.name.isNotBlank() && it.description.isNotBlank() }
                val minMaxOk = if (target == DivineTier.LESSER_GOD) validPassives.size in 2..3 else validPassives.size >= 2
                val incompletePassive = draft.passiveCapabilities.any {
                    (it.name.isBlank() && it.description.isNotBlank()) || (it.name.isNotBlank() && it.description.isBlank())
                }
                val validActs = draft.domainActs.filter { it.name.isNotBlank() && it.description.isNotBlank() }
                val incompleteAct = draft.domainActs.any {
                    (it.name.isBlank() && it.description.isNotBlank()) || (it.name.isNotBlank() && it.description.isBlank())
                }
                val maxActCost = if (target == DivineTier.GREATER_GOD) 7 else 5
                val invalidActCost = validActs.any { it.costPd !in 0..maxActCost }
                val incompleteGift = draft.heraldGift.let { g ->
                    val anyFilled = g.name.isNotBlank() || g.effect.isNotBlank() || g.downside.isNotBlank()
                    anyFilled && (g.name.isBlank() || g.effect.isBlank() || g.downside.isBlank())
                }
                when {
                    incompletePassive -> "Preencha nome e efeito de todas as Capacidades Passivas adicionadas."
                    !minMaxOk -> if (target == DivineTier.LESSER_GOD) {
                        "Defina de 2 a 3 Capacidades Passivas completas para o Deus Menor."
                    } else {
                        "Mantenha pelo menos 2 Capacidades Passivas completas."
                    }
                    incompleteAct -> "Preencha nome e efeito de todos os Atos de Domínio adicionados."
                    validActs.isEmpty() -> "Defina pelo menos 1 Ato de Domínio ativo em PD."
                    invalidActCost -> "O custo de cada Ato de Domínio deve ficar entre 0 e $maxActCost PD."
                    incompleteGift -> "Complete nome, efeito e contrapartida da Dádiva de Arauto (ou deixe os três em branco)."
                    else -> null
                }
            }

            4 -> (1..3).firstNotNullOfOrNull { validateStep(it, character, draft) }
            else -> null
        }
    }

    /** Validates whether a non-GM sheet owner's `divineAscension` mutation is an authorized completion of `pendingPromotionTier`. */
    fun isAuthorizedPlayerPromotionCompletion(
        before: CharacterDivineAscension,
        after: CharacterDivineAscension,
    ): Boolean {
        val pending = before.pendingPromotionTier ?: return false
        if (pending != DivineTier.LESSER_GOD && pending != DivineTier.GREATER_GOD) return false
        if (after.pendingPromotionTier != null || after.tier != pending) return false
        if (after.dominio.isBlank() || after.santuario.isBlank() || after.postura.isBlank() ||
            after.linhaDePensamento.isBlank() || after.tabu.isBlank()
        ) {
            return false
        }
        val cleanBefore = before.copy(pendingPromotionTier = null)
        return DivineRules.validAscensionPillars(cleanBefore, after)
    }

    /** Completes the guided ascension wizard, promoting the sheet, syncing PD powers, and granting the new tier's session PD. */
    fun finish(character: Character): Character {
        val target = targetTier(character) ?: throw IllegalArgumentException("Nenhuma promoção divina pendente.")
        val draft = currentDraft(character)
        val validationError = validateStep(STEP_COUNT, character, draft)
        require(validationError == null) { validationError ?: "Dados de ascensão incompletos." }

        val cleanedPassives = draft.passiveCapabilities
            .filter { it.name.isNotBlank() && it.description.isNotBlank() }
            .map { it.copy(name = it.name.trim(), description = it.description.trim()) }
        val cleanedGift = draft.heraldGift.let { g ->
            if (g.name.isNotBlank() && g.effect.isNotBlank() && g.downside.isNotBlank()) {
                DivineHeraldGiftDraft(
                    name = g.name.trim(),
                    executionKind = g.executionKind,
                    effect = g.effect.trim(),
                    downside = g.downside.trim(),
                )
            } else {
                DivineHeraldGiftDraft()
            }
        }

        val nextAscension = CharacterDivineAscension(
            tier = target,
            dominio = draft.dominio.trim(),
            santuario = draft.santuario.trim(),
            postura = draft.postura.trim(),
            linhaDePensamento = draft.linhaDePensamento.trim(),
            tabu = draft.tabu.trim(),
            pillars = draft.pillars,
            passiveCapabilities = cleanedPassives,
            materialRestrictionNote = if (target == DivineTier.GREATER_GOD) draft.materialRestrictionNote.trim() else "",
            heraldGift = cleanedGift,
            pendingPromotionTier = null,
        )

        val ascended = character
            .copy(divineAscension = character.divineAscension.copy(pendingPromotionTier = null))
            .withDivineAscension(nextAscension)

        val actAbilities = draft.domainActs
            .filter { it.name.isNotBlank() && it.description.isNotBlank() }
            .map { act ->
                Ability(
                    id = "divine-act:${act.id}",
                    name = act.name.trim(),
                    kind = AbilityKind.POWER,
                    source = Source.narrative(NarrativeSourceId("divine-ascension")),
                    execution = Execution(act.executionKind),
                    range = RangeSpec(act.rangeBand),
                    duration = Duration(act.durationKind),
                    cost = AbilityCost.PowerCost(
                        amount = act.costPd.coerceAtLeast(0),
                        resource = SpendableResource.PD,
                        isDivineOrLuckBased = true,
                    ),
                    effect = act.description.trim(),
                )
            }

        val existingIds = actAbilities.mapTo(hashSetOf(), Ability::id)
        val existingNames = actAbilities.mapTo(hashSetOf()) { it.name.lowercase() }
        val mergedAbilities = ascended.allCanonicalAbilitiesSafely()
            .filterNot { it.id in existingIds || it.name.lowercase() in existingNames } + actAbilities

        val minGrantedPd = maxOf(ascended.destiny.current, target.pdPerSession).coerceIn(0, ascended.destinyMaximum)
        return ascended.copy(
            abilities = mergedAbilities,
            destiny = ascended.destiny.copy(current = minGrantedPd, maximum = ascended.destinyMaximum),
            divineRuntime = ascended.divineRuntime.copy(ascensionDraft = null),
        )
    }
}
