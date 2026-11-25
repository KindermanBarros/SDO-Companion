package com.kinderman.sdo.domain.model

import java.util.UUID
import kotlin.math.ceil
import kotlinx.serialization.Serializable

/** Mechanical scale separating tactical combat sheets from conceptual Pillar-driven deities. */
@Serializable
enum class DivineScale(val label: String) {
    TACTICAL("Escala Tática"),
    CONCEPTUAL_PILLARS("Pilares Divinos"),
    COSMIC_UNPLAYABLE("Força Cósmica (Sem Ficha)"),
}

/**
 * Canonical SDO divine hierarchy (`Deidades e Avatares.md:13-22`, `Deidades.md:53-78`).
 * Note: Heralds (`Arauto`) remain at [MORTAL] tier (or any other tier) via tier-independent [DivineMark]s.
 * [PRIMORDIAL] is modeled for source-tier comparisons only; Primordials have no playable sheet.
 */
@Serializable
enum class DivineTier(
    val label: String,
    val scale: DivineScale,
    val pdPerSession: Int,
    val rankOrder: Int,
    val playableOnCharacterSheet: Boolean = true,
) {
    MORTAL("Mortal", DivineScale.TACTICAL, pdPerSession = 1, rankOrder = 0),
    AVATAR("Avatar", DivineScale.TACTICAL, pdPerSession = 2, rankOrder = 1),
    DEMIGOD("Semideus", DivineScale.TACTICAL, pdPerSession = 3, rankOrder = 2),
    LESSER_GOD("Deus Menor", DivineScale.CONCEPTUAL_PILLARS, pdPerSession = 4, rankOrder = 3),
    GREATER_GOD("Deus Maior", DivineScale.CONCEPTUAL_PILLARS, pdPerSession = 5, rankOrder = 4),
    PRIMORDIAL("Primordial", DivineScale.COSMIC_UNPLAYABLE, pdPerSession = 0, rankOrder = 5, playableOnCharacterSheet = false),
}

@Serializable
enum class DivinePillarKind(val label: String) {
    ASPECTO("Aspecto"),
    DOMINIO("Domínio"),
    SANTUARIO("Santuário"),
    PRESENCA("Presença"),
}

/** Four Divine Pillars rated 0..5 for Lesser and Greater Gods (`Deidades e Avatares.md:25-33`). */
@Serializable
data class DivinePillars(
    val aspecto: Int = 0,
    val dominio: Int = 0,
    val santuario: Int = 0,
    val presenca: Int = 0,
) {
    fun get(pillar: DivinePillarKind): Int = when (pillar) {
        DivinePillarKind.ASPECTO -> aspecto
        DivinePillarKind.DOMINIO -> dominio
        DivinePillarKind.SANTUARIO -> santuario
        DivinePillarKind.PRESENCA -> presenca
    }.coerceIn(0, 5)

    fun with(pillar: DivinePillarKind, value: Int): DivinePillars {
        val clamped = value.coerceIn(0, 5)
        return when (pillar) {
            DivinePillarKind.ASPECTO -> copy(aspecto = clamped)
            DivinePillarKind.DOMINIO -> copy(dominio = clamped)
            DivinePillarKind.SANTUARIO -> copy(santuario = clamped)
            DivinePillarKind.PRESENCA -> copy(presenca = clamped)
        }
    }

    fun clamped(maxPerPillar: Int = 5): DivinePillars {
        val cap = maxPerPillar.coerceIn(0, 5)
        return DivinePillars(
            aspecto = aspecto.coerceIn(0, cap),
            dominio = dominio.coerceIn(0, cap),
            santuario = santuario.coerceIn(0, cap),
            presenca = presenca.coerceIn(0, cap),
        )
    }

    val totalPoints: Int get() = aspecto.coerceIn(0, 5) + dominio.coerceIn(0, 5) + santuario.coerceIn(0, 5) + presenca.coerceIn(0, 5)
}

@Serializable
enum class DivineMarkKind(val code: String, val label: String) {
    ARAUTO("arauto", "Arauto"),
    MARCADO("marcado", "Marcado"),
}

/**
 * Tier-independent divine mark (`Deidades e Avatares.md:79-86`).
 * Characters may hold multiple marks regardless of [DivineTier]. Active marks act as living sensory Anchors.
 */
@Serializable
data class DivineMark(
    val id: String = UUID.randomUUID().toString(),
    val deityId: String = "",
    val deityName: String = "",
    val kind: DivineMarkKind = DivineMarkKind.ARAUTO,
    val giftName: String = "",
    val executionKind: ExecutionKind = ExecutionKind.Free,
    val effect: String = "",
    val downside: String = "",
    val active: Boolean = true,
) {
    fun displayDeityLabel(catalogById: Map<String, CatalogEntry> = emptyMap()): String =
        catalogById[deityId]?.name
            ?.takeIf(String::isNotBlank)
            ?: deityName.takeIf(String::isNotBlank)
            ?: deityId.takeIf(String::isNotBlank)
            ?: "Deidade desconhecida"
}

@Serializable
data class DivinePassiveCapability(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
)

@Serializable
data class DivineAscensionActDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val costPd: Int = 1,
    val executionKind: ExecutionKind = ExecutionKind.Action,
    val rangeBand: RangeBand = RangeBand.Short,
    val durationKind: DurationKind = DurationKind.Scene,
    val description: String = "",
)

@Serializable
data class DivineHeraldGiftDraft(
    val name: String = "",
    val executionKind: ExecutionKind = ExecutionKind.Free,
    val effect: String = "",
    val downside: String = "",
)

@Serializable
data class DivineAscensionDraft(
    val step: Int = 1,
    val dominio: String = "",
    val santuario: String = "",
    val postura: String = "",
    val linhaDePensamento: String = "",
    val tabu: String = "",
    val materialRestrictionNote: String = "",
    val pillars: DivinePillars = DivinePillars(),
    val passiveCapabilities: List<DivinePassiveCapability> = emptyList(),
    val domainActs: List<DivineAscensionActDraft> = emptyList(),
    val heraldGift: DivineHeraldGiftDraft = DivineHeraldGiftDraft(),
)

/**
 * GM-controlled ascension profile stored separately from player runtime state so write authorization
 * can restrict tier, patron, Domain, Postura, Tabu, Pillars, and Divine Passives to GMs and Admins,
 * while allowing a GM-unlocked [pendingPromotionTier] (`LESSER_GOD` / `GREATER_GOD`) to be completed by the sheet owner.
 */
@Serializable
data class CharacterDivineAscension(
    val tier: DivineTier = DivineTier.MORTAL,
    val patronDeityId: String = "",
    val patronName: String = "",
    val dominio: String = "",
    val santuario: String = "",
    val postura: String = "",
    val linhaDePensamento: String = "",
    val tabu: String = "",
    val pillars: DivinePillars = DivinePillars(),
    val passiveCapabilities: List<DivinePassiveCapability> = emptyList(),
    val materialRestrictionNote: String = "",
    val heraldGift: DivineHeraldGiftDraft = DivineHeraldGiftDraft(),
    val pendingPromotionTier: DivineTier? = null,
) {
    fun displayPatronLabel(catalogById: Map<String, CatalogEntry> = emptyMap()): String =
        catalogById[patronDeityId]?.name
            ?.takeIf(String::isNotBlank)
            ?: patronName.takeIf(String::isNotBlank)
            ?: patronDeityId.takeIf(String::isNotBlank)
            ?: ""
}

/** Session-scoped divine runtime state (marks, scene Tabu lock, scene PD expenditure, and in-progress ascension draft). */
@Serializable
data class CharacterDivineRuntime(
    val marks: List<DivineMark> = emptyList(),
    val tabuViolatedInScene: Boolean = false,
    val pdSpentInCurrentScene: Int = 0,
    val channeledInCurrentScene: Boolean = false,
    val links: List<DivineLink> = emptyList(),
    val pillarSuppression: DivinePillars = DivinePillars(),
    val scenePillarSuppression: DivinePillars = DivinePillars(),
    val sceneSuppressionCauseIds: Set<String> = emptySet(),
    val suppressionCauseIds: Set<String> = emptySet(),
    val restorativeSceneIds: Set<String> = emptySet(),
    val restorationSessionIds: Set<String> = emptySet(),
    val growthArcIds: Set<String> = emptySet(),
    val lossArcIds: Set<String> = emptySet(),
    val pillarMilestoneIds: Set<String> = emptySet(),
    val linkScenePayments: Map<String, Int> = emptyMap(),
    val divineFate: DivineFate = DivineFate.ACTIVE,
    val divineFateCauseIds: Set<String> = emptySet(),
    val ascensionDraft: DivineAscensionDraft? = null,
)

/** Official corruption threshold stages (`Corrupção Divina.md:11-17`). */
enum class DivineCorruptionStage(val label: String, val summary: String) {
    RESIDUO("Resíduo (0–24%)", "Sinais leves ou inexistentes."),
    MARCA("Marca (25–49%)", "Alterações perceptíveis e consequências narrativas."),
    INSTABILIDADE("Instabilidade (50–74%)", "Falha Crítica em teste arcano causa +5% adicional; Runas Darno geram Eco Emocional."),
    SATURACAO("Saturação (75–99%)", "Ao canalizar poder arcano ou divino pela 1ª vez na cena, recebe +1 Exaustão."),
    RUPTURA("Ruptura (100%)", "Crise de Corrupção; encerra efeitos sustentados e perde canalização voluntária."),
    ;

    companion object {
        fun fromPercentage(value: Int): DivineCorruptionStage {
            val pct = value.coerceIn(0, 100)
            return when {
                pct >= 100 -> RUPTURA
                pct >= 75 -> SATURACAO
                pct >= 50 -> INSTABILIDADE
                pct >= 25 -> MARCA
                else -> RESIDUO
            }
        }
    }
}

enum class DeityClashWinner { ATTACKER, DEFENDER, TIE }

data class DeityClashResult(
    val attackerTotal: Int,
    val defenderTotal: Int,
    val winner: DeityClashWinner,
    val reason: String,
)

/** Single authoritative derived divine summary for any character (`Deidades e Avatares.md:13-22, 124-136`). */
data class DerivedDivineEffects(
    val tier: DivineTier,
    val mechanicalScale: DivineScale,
    val usesConceptualPillars: Boolean,
    val pdPerSession: Int,
    val destinyBaseCeiling: Int,
    val immuneToMortalMentalEffects: Boolean,
    val hasInstantDarnoRunes: Boolean,
    val canReadDarnoScript: Boolean,
    val darnoEmotionalEchoActive: Boolean,
    val hasMaterialRestriction: Boolean,
    val canConvertSanityOrAnchorToPd: Boolean,
    val activeMarks: List<DivineMark>,
    val isLivingSensoryAnchor: Boolean,
    val domainActsBlockedByTabu: Boolean,
    val corruptionStage: DivineCorruptionStage,
    val presencaQualitativeSummary: String,
    val avatarAutonomyCheckDc: Int?,
)

// --- Library / Compendium Deity View Models (Data-layer Spoiler Separated) ---

@Serializable
data class DeityPublicProfile(
    val id: String,
    val navigationOrder: Int,
    val name: String,
    val publicTierLabel: String,
    val tier: DivineTier,
    val paraJogadoresHeading: String = "Para Jogadores (O Que Se Sabe)",
    val paraJogadores: String,
    val source: String = "Compêndio de Deidades",
    val ruleReference: String = "02 - Compêndio/Entidades/NPCs/Deidades/Deidades.md",
)

@Serializable
data class DeityGmIdentity(
    val categoria: String = "",
    val dominio: String = "",
    val santuario: String = "",
    val postura: String = "",
    val culto: String = "",
    val manifestacao: String = "",
)

@Serializable
data class DeityGmTacticalStats(
    val pv: Int = 0,
    val pe: Int = 0,
    val pm: Int = 0,
    val ps: Int = 0,
    val pd: Int = 0,
    val attributes: Map<String, Int> = emptyMap(),
    val protections: Map<String, Int> = emptyMap(),
)

@Serializable
data class DeityIrradiatedCorruption(
    val intensidade: String = "",
    val efeitoPeculiar: String = "",
)

@Serializable
data class DeityDomainAct(
    val name: String,
    val costPd: Int = 0,
    val listedDc: Int? = null,
    val description: String = "",
)

@Serializable
data class DeityHeraldGiftSpec(
    val heading: String = "",
    val name: String = "",
    val execution: String = "Livre",
    val headerNote: String = "",
    val effect: String = "",
    val downside: String = "",
)

@Serializable
data class DeityExtraSection(
    val heading: String,
    val body: String,
)

@Serializable
data class DeityGmContent(
    val id: String,
    val navigationOrder: Int = 0,
    val name: String = "",
    val tierCode: String = "LESSER_GOD",
    val publicTierLabel: String = "",
    val rawCategory: String = "",
    val mechanicalScale: String = "CONCEPTUAL_PILLARS",
    val epigraph: String = "",
    val overview: String = "",
    val publicImageSummary: String = "",
    val centralMystery: String = "",
    val identity: DeityGmIdentity = DeityGmIdentity(),
    val paraJogadoresHeading: String = "Para Jogadores (O Que Se Sabe)",
    val paraJogadores: String = "",
    val paraOMestreHeading: String = "Para o Mestre (Origem, Motivos e Postura)",
    val paraOMestre: String = "",
    val linhaDePensamento: String = "",
    val tabu: String = "",
    val pillars: DivinePillars? = null,
    val tacticalStats: DeityGmTacticalStats? = null,
    val destinyPoints: Int = 0,
    val irradiatedCorruption: DeityIrradiatedCorruption = DeityIrradiatedCorruption(),
    val actsHeading: String = "",
    val domainActs: List<DeityDomainAct> = emptyList(),
    val passiveCapabilities: List<DivinePassiveCapability> = emptyList(),
    val heraldGift: DeityHeraldGiftSpec = DeityHeraldGiftSpec(),
    val extraSections: List<DeityExtraSection> = emptyList(),
    val behavior: String = "",
    val tableHooks: String = "",
    val tags: List<String> = emptyList(),
)

/**
 * Data-layer response for opening a deity.
 * [gmContent] is strictly null whenever the caller lacks verified GM/Admin privileges in the relevant campaign context.
 */
data class DeityDetailView(
    val publicProfile: DeityPublicProfile,
    val gmContent: DeityGmContent? = null,
)

object DivineRules {
    fun validAscensionPillars(previous: CharacterDivineAscension, next: CharacterDivineAscension): Boolean {
        if (next.tier.scale != DivineScale.CONCEPTUAL_PILLARS) return true
        val cap = if (next.tier == DivineTier.LESSER_GOD) 4 else 5
        if (next.pillars != next.pillars.clamped(cap)) return false
        if (previous.tier == next.tier) return next.pillars == previous.pillars
        return when (next.tier) {
            DivineTier.LESSER_GOD -> next.pillars.totalPoints == 8
            DivineTier.GREATER_GOD -> previous.tier == DivineTier.LESSER_GOD &&
                next.pillars.totalPoints == previous.pillars.totalPoints + 4 &&
                DivinePillarKind.entries.all { next.pillars.get(it) >= previous.pillars.get(it) }
            else -> true
        }
    }

    const val DEFAULT_CORRUPTION_GAIN = 5
    const val DEFAULT_CORRUPTION_PURIFICATION = -5
    const val TABU_VIOLATION_PD_COST = 2
    const val DARNO_INSTANT_RUNE_PD_COST = 3

    /**
     * Direct PJ resistance DC against a Domain Act (`Deidades e Avatares.md:74`):
     * `CD = 15 + Pilar relevante da deidade + PD extras investidos`.
     */
    fun calculateDirectResistanceDc(relevantPillar: Int, extraPdInvested: Int = 0): Int =
        15 + relevantPillar.coerceIn(0, 5) + extraPdInvested.coerceAtLeast(0)

    /**
     * Domain clash between two deities (`Deidades e Avatares.md:75`):
     * Compares `Pilar relevante + PD investidos`. Ties break by standing inside own Santuário, then higher tier.
     */
    fun resolveDeityClash(
        attackerPillar: Int,
        attackerPdInvested: Int,
        defenderPillar: Int,
        defenderPdInvested: Int,
        attackerInOwnSanctuary: Boolean = false,
        defenderInOwnSanctuary: Boolean = false,
        attackerTier: DivineTier = DivineTier.LESSER_GOD,
        defenderTier: DivineTier = DivineTier.LESSER_GOD,
    ): DeityClashResult {
        val atk = attackerPillar.coerceIn(0, 5) + attackerPdInvested.coerceAtLeast(0)
        val def = defenderPillar.coerceIn(0, 5) + defenderPdInvested.coerceAtLeast(0)
        return when {
            atk > def -> DeityClashResult(atk, def, DeityClashWinner.ATTACKER, "Maior total de Pilar + PD ($atk vs $def).")
            def > atk -> DeityClashResult(atk, def, DeityClashWinner.DEFENDER, "Maior total de Pilar + PD ($def vs $atk).")
            attackerInOwnSanctuary && !defenderInOwnSanctuary ->
                DeityClashResult(atk, def, DeityClashWinner.ATTACKER, "Empate ($atk) decidido pelo Santuário próprio.")
            defenderInOwnSanctuary && !attackerInOwnSanctuary ->
                DeityClashResult(atk, def, DeityClashWinner.DEFENDER, "Empate ($atk) decidido pelo Santuário próprio.")
            attackerTier.rankOrder > defenderTier.rankOrder ->
                DeityClashResult(atk, def, DeityClashWinner.ATTACKER, "Empate ($atk) decidido pela categoria divina superior (${attackerTier.label}).")
            defenderTier.rankOrder > attackerTier.rankOrder ->
                DeityClashResult(atk, def, DeityClashWinner.DEFENDER, "Empate ($atk) decidido pela categoria divina superior (${defenderTier.label}).")
            else -> DeityClashResult(atk, def, DeityClashWinner.TIE, "Empate absoluto ($atk vs $def); impasse conceitual.")
        }
    }

    /**
     * Source- and target-aware Divine Corruption calculation (`Deidades e Avatares.md:17-20, 93`, `Corrupção Divina.md:49-51`):
     * - Avatar is immune (`0`) to its linked patron deity and halves other gains rounding up.
     * - Lesser and Greater Gods are immune (`0`) to sources of equal or lower tier, taking full gain from strictly higher tiers.
     * - Mortals, Heralds, and Demigods take full gain unless a specific rule applies.
     * - Negative values represent explicit purification (`Corrupção Divina.md:65`).
     */
    fun calculateCorruptionDelta(
        targetTier: DivineTier,
        targetPatronDeityId: String = "",
        sourceTier: DivineTier? = null,
        sourceDeityId: String = "",
        rawAmount: Int = DEFAULT_CORRUPTION_GAIN,
    ): Int {
        if (rawAmount <= 0) return rawAmount
        if (targetTier == DivineTier.PRIMORDIAL) return 0

        if (targetTier == DivineTier.LESSER_GOD || targetTier == DivineTier.GREATER_GOD) {
            if (sourceTier != null && sourceTier.rankOrder <= targetTier.rankOrder) {
                return 0
            }
            return rawAmount
        }

        if (targetTier == DivineTier.AVATAR) {
            if (targetPatronDeityId.isNotBlank() && targetPatronDeityId == sourceDeityId) {
                return 0
            }
            return (rawAmount + 1) / 2
        }

        return rawAmount
    }

    /** Qualitative relationship between Presença (0–5) and sustained Anchors/Avatars (`Deidades e Avatares.md:32`). */
    fun presencaQualitativeDescription(presenca: Int): String = when (presenca.coerceIn(0, 5)) {
        0 -> "Presença nula ou encarcerada: atua apenas por relíquias/Chaves ou vínculos pré-existentes."
        1 -> "Presença tênue: sustenta poucas Âncoras próximas com esforço perceptível; projeção remota rara."
        2 -> "Presença focalizada: mantém um círculo restrito de Arautos/Âncoras locais sem desgaste."
        3 -> "Presença estável: sustenta múltiplos Arautos regionais e canalizações pontuais sem atrito."
        4 -> "Presença ampla: mantém rede consistente de Âncoras e Avatares em várias regiões sem desgaste."
        else -> "Presença continental/planar: sustenta múltiplos Avatares e cultos inteiros como Âncoras simultâneas."
    }

    /** Single authoritative calculation for all tier- and mark-dependent properties. */
    fun deriveEffects(character: Character, patronPresenca: Int? = null): DerivedDivineEffects {
        val ascension = character.divineAscension
        val runtime = character.divineRuntime
        val tier = ascension.tier.takeIf { it.playableOnCharacterSheet } ?: DivineTier.MORTAL
        val activeMarks = runtime.marks.filter { it.active }
        val usesPillars = tier.scale == DivineScale.CONCEPTUAL_PILLARS
        val basePdCeiling = if (tier == DivineTier.GREATER_GOD) 10 else 5
        val immuneMental = tier == DivineTier.LESSER_GOD || tier == DivineTier.GREATER_GOD
        val instantDarno = tier == DivineTier.DEMIGOD
        val readsDarno = instantDarno || usesPillars || activeMarks.any {
            it.deityId == "deity.kaelith" || it.giftName.contains("Leitura das Margens", ignoreCase = true)
        }
        val distressedConditions = character.canonicalConditions().any {
            it.payload.kind == ConditionKind.Abalado ||
                it.payload.kind.label.equals("Amedrontado", ignoreCase = true) ||
                it.name.equals("Abalado", ignoreCase = true) ||
                it.name.equals("Amedrontado", ignoreCase = true)
        }
        val sanityRupture = !usesPillars && character.sanityMaximum > 0 && character.sanity.current <= 0
        val darnoEcho = instantDarno && (character.corruption.current >= 50 || sanityRupture || distressedConditions)
        val autonomyDc = if (tier == DivineTier.AVATAR && (runtime.pdSpentInCurrentScene >= 2 || runtime.tabuViolatedInScene) && patronPresenca != null) {
            15 + patronPresenca.coerceIn(0, 5)
        } else {
            null
        }
        return DerivedDivineEffects(
            tier = tier,
            mechanicalScale = tier.scale,
            usesConceptualPillars = usesPillars,
            pdPerSession = tier.pdPerSession,
            destinyBaseCeiling = basePdCeiling,
            immuneToMortalMentalEffects = immuneMental,
            hasInstantDarnoRunes = instantDarno,
            canReadDarnoScript = readsDarno,
            darnoEmotionalEchoActive = darnoEcho,
            hasMaterialRestriction = tier == DivineTier.GREATER_GOD,
            canConvertSanityOrAnchorToPd = tier != DivineTier.MORTAL,
            activeMarks = activeMarks,
            isLivingSensoryAnchor = activeMarks.isNotEmpty(),
            domainActsBlockedByTabu = runtime.tabuViolatedInScene,
            corruptionStage = DivineCorruptionStage.fromPercentage(character.corruption.current),
            presencaQualitativeSummary = presencaQualitativeDescription(character.currentDivinePillars().presenca),
            avatarAutonomyCheckDc = autonomyDc,
        )
    }
}

data class DeityOption(
    val id: String,
    val name: String,
    val tier: DivineTier,
    val subtitle: String,
    val isCampaignCharacter: Boolean = false,
)

fun resolveAvailableDeityOptions(
    currentCharacter: Character,
    catalog: List<CatalogEntry>,
    characters: List<Character> = emptyList(),
): List<DeityOption> {
    val currentCampaignId = normalizeCampaignId(currentCharacter.campaignId)
    val campaignDeityOptions = if (currentCampaignId.isBlank()) {
        emptyList()
    } else {
        characters.asSequence()
            .filter { other ->
                other.id != currentCharacter.id &&
                    !other.deleted &&
                    normalizeCampaignId(other.campaignId) == currentCampaignId &&
                    (other.divineAscension.tier == DivineTier.LESSER_GOD ||
                        other.divineAscension.tier == DivineTier.GREATER_GOD)
            }
            .map { deityChar ->
                val domainHint = deityChar.divineAscension.dominio.takeIf(String::isNotBlank)
                DeityOption(
                    id = "sheet:${deityChar.id}",
                    name = deityChar.name.ifBlank { "Divindade da campanha" },
                    tier = deityChar.divineAscension.tier,
                    subtitle = listOfNotNull(
                        "${deityChar.divineAscension.tier.label} · Mesa atual",
                        domainHint,
                    ).joinToString(" // "),
                    isCampaignCharacter = true,
                )
            }
            .sortedBy(DeityOption::name)
            .toList()
    }
    val catalogOptions = catalog.asSequence()
        .filter { it.kind == CatalogKind.DEITY }
        .distinctBy(CatalogEntry::id)
        .map { entry ->
            val profile = entry.toDeityPublicProfile()
            DeityOption(
                id = entry.id,
                name = entry.name,
                tier = profile.tier,
                subtitle = profile.publicTierLabel,
                isCampaignCharacter = false,
            )
        }
        .sortedWith(compareBy<DeityOption> { catalogEntry -> catalog.indexOfFirst { it.id == catalogEntry.id } })
        .toList()
    return campaignDeityOptions + catalogOptions
}

fun DivineMark.displayDeityLabel(
    catalogById: Map<String, CatalogEntry>,
    charactersById: Map<String, Character>,
): String {
    val sheetId = deityId.removePrefix("sheet:").takeIf { deityId.startsWith("sheet:") }
    val fromSheet = sheetId?.let { charactersById[it]?.name }?.takeIf(String::isNotBlank)
    return fromSheet
        ?: displayDeityLabel(catalogById)
}

fun CharacterDivineAscension.displayPatronLabel(
    catalogById: Map<String, CatalogEntry>,
    charactersById: Map<String, Character>,
): String {
    val sheetId = patronDeityId.removePrefix("sheet:").takeIf { patronDeityId.startsWith("sheet:") }
    val fromSheet = sheetId?.let { charactersById[it]?.name }?.takeIf(String::isNotBlank)
    return fromSheet
        ?: displayPatronLabel(catalogById)
}

fun CatalogEntry.toDeityPublicProfile(): DeityPublicProfile {
    require(kind == CatalogKind.DEITY) { "CatalogEntry $id is not a DEITY" }
    val tier = when (group.lowercase()) {
        "deus maior" -> DivineTier.GREATER_GOD
        "deus menor" -> DivineTier.LESSER_GOD
        "semideus", "semideusa" -> DivineTier.DEMIGOD
        "avatar" -> DivineTier.AVATAR
        else -> DivineTier.LESSER_GOD
    }
    return DeityPublicProfile(
        id = id,
        navigationOrder = initialValue ?: Int.MAX_VALUE,
        name = name,
        publicTierLabel = group,
        tier = tier,
        paraJogadores = summary,
        source = source,
        ruleReference = ruleReference,
    )
}

fun Character.derivedDivineEffects(patronPresenca: Int? = null): DerivedDivineEffects =
    DivineRules.deriveEffects(this, patronPresenca)

/** GM-authorized ascension transition (`Deidades e Avatares.md:99-121`). Supports direct Mortal -> Demigod progression. */
fun Character.withDivineAscension(next: CharacterDivineAscension): Character {
    require(next.tier.playableOnCharacterSheet) {
        "Primordiais são forças cósmicas sem ficha jogável (Deidades e Avatares.md:21)."
    }
    require(DivineRules.validAscensionPillars(divineAscension, next)) {
        "Distribuição de Pilares inválida para esta ascensão."
    }
    val pillarCap = if (next.tier == DivineTier.LESSER_GOD) 4 else 5
    val sanitized = when (next.tier) {
        DivineTier.MORTAL -> CharacterDivineAscension(tier = DivineTier.MORTAL)
        DivineTier.AVATAR -> CharacterDivineAscension(
            tier = DivineTier.AVATAR,
            patronDeityId = next.patronDeityId.trim(),
            patronName = next.patronName.trim(),
        )
        DivineTier.DEMIGOD -> CharacterDivineAscension(
            tier = DivineTier.DEMIGOD,
            patronDeityId = next.patronDeityId.trim(),
            patronName = next.patronName.trim(),
            dominio = next.dominio.trim(),
            postura = next.postura.trim(),
            linhaDePensamento = next.linhaDePensamento.trim(),
            tabu = next.tabu.trim(),
        )
        DivineTier.LESSER_GOD, DivineTier.GREATER_GOD -> next.copy(
            pillars = next.pillars.clamped(pillarCap),
            patronDeityId = "",
            patronName = "",
            dominio = next.dominio.trim(),
            santuario = next.santuario.trim(),
            postura = next.postura.trim(),
            linhaDePensamento = next.linhaDePensamento.trim(),
            tabu = next.tabu.trim(),
            materialRestrictionNote = if (next.tier == DivineTier.GREATER_GOD) {
                next.materialRestrictionNote.ifBlank {
                    "Sem permanência física livre em Nulvara; atua via Âncoras, Avatares, Santuários ou Intervenção Direta (5 PD)."
                }
            } else {
                ""
            },
        )
        DivineTier.PRIMORDIAL -> error("Unreachable")
    }
    val updated = copy(divineAscension = sanitized)
    val newCeiling = updated.destinyMaximum
    val grantedPd = destiny.current.coerceIn(0, newCeiling)
    val syncedAbilities = buildList {
        addAll(updated.allCanonicalAbilitiesSafely())
        if (sanitized.tier.scale == DivineScale.CONCEPTUAL_PILLARS) {
            sanitized.passiveCapabilities.forEach { passive ->
                val trimmedName = passive.name.trim()
                if (trimmedName.isNotBlank() && none { it.id == "divine-passive:${passive.id}" || it.name.equals(trimmedName, ignoreCase = true) }) {
                    add(
                        Ability(
                            id = "divine-passive:${passive.id}",
                            name = trimmedName,
                            kind = AbilityKind.POWER,
                            source = Source.narrative(NarrativeSourceId("divine-ascension")),
                            execution = Execution(ExecutionKind.Passive),
                            duration = null,
                            cost = AbilityCost.PowerCost(0, SpendableResource.PD, isDivineOrLuckBased = true),
                            effect = passive.description.trim(),
                            powerData = PowerData(passiveState = PassiveState.ManualToggle(active = true)),
                        ),
                    )
                }
            }
        }
    }
    return updated.copy(
        abilities = syncedAbilities,
        destiny = destiny.copy(current = grantedPd, maximum = newCeiling),
    )
}

/** Spend PD on a Domain Act (base cost + optional extra PD investment). Blocked when Tabu was violated in scene. */
fun Character.performDomainAct(baseCostPd: Int, extraPdInvested: Int = 0): Character {
    require(!divineRuntime.tabuViolatedInScene) {
        "Atos de Domínio estão bloqueados nesta cena por violação de Tabu."
    }
    require(baseCostPd >= 0 && extraPdInvested >= 0) { "Custos de Destino não podem ser negativos." }
    require(baseCostPd <= destiny.current && extraPdInvested <= destiny.current - baseCostPd) {
        "Pontos de Destino insuficientes."
    }
    val totalCost = baseCostPd + extraPdInvested
    require(destiny.current >= totalCost) {
        "Pontos de Destino insuficientes (necessário $totalCost PD, saldo atual ${destiny.current} PD)."
    }
    return channelDivineOrArcanePower().copy(
        destiny = destiny.copy(current = destiny.current - totalCost, maximum = destinyMaximum),
        divineRuntime = divineRuntime.copy(
            pdSpentInCurrentScene = divineRuntime.pdSpentInCurrentScene + totalCost,
            channeledInCurrentScene = true,
        ),
    )
}

/** Violate Tabu: immediately deducts up to 2 PD and blocks Domain Acts for the remainder of the scene (`Deidades e Avatares.md:42`). */
fun Character.violateDivineTabu(): Character {
    val nextPd = (destiny.current - DivineRules.TABU_VIOLATION_PD_COST).coerceAtLeast(0)
    val spent = destiny.current - nextPd
    return copy(
        destiny = destiny.copy(current = nextPd, maximum = destinyMaximum),
        divineRuntime = divineRuntime.copy(
            tabuViolatedInScene = true,
            pdSpentInCurrentScene = divineRuntime.pdSpentInCurrentScene + spent,
        ),
    )
}

/** Scene reset clears scene-scoped Tabu Domain Act lock and scene PD counter (`Deidades e Avatares.md:42`). */
fun Character.resetDivineScene(): Character = copy(
    divineRuntime = divineRuntime.copy(
        scenePillarSuppression = DivinePillars(),
        sceneSuppressionCauseIds = emptySet(),
        tabuViolatedInScene = false,
        pdSpentInCurrentScene = 0,
        channeledInCurrentScene = false,
    ),
)

/** Recover per-session PD allowance up to [destinyMaximum] (`Deidades e Avatares.md:13-20`). */
fun Character.grantSessionDestinyAllowance(): Character {
    val allowance = derivedDivineEffects().pdPerSession
    val cap = destinyMaximum
    val nextCurrent = (destiny.current + allowance).coerceAtMost(cap)
    return copy(destiny = destiny.copy(current = nextCurrent, maximum = cap))
}

/**
 * Avatar+ recovery (`Deidades e Avatares.md:52`): spend 10% of max Sanity (rounded up, min 1) for tactical
 * entities—or record an Anchor/Sanctuary sacrifice for PS-immune gods—to recover +1 PD up to [destinyMaximum].
 */
fun Character.convertVitalityOrAnchorToDestiny(): Character {
    val effects = derivedDivineEffects()
    require(effects.canConvertSanityOrAnchorToPd) {
        "Apenas entidades de categoria Avatar ou superior podem converter reserva em Destino."
    }
    val cap = destinyMaximum
    require(destiny.current < cap) { "Pontos de Destino já estão no limite máximo ($cap PD)." }
    return if (effects.usesConceptualPillars) {
        copy(destiny = destiny.copy(current = (destiny.current + 1).coerceAtMost(cap), maximum = cap))
    } else {
        val psCost = ceil(sanityMaximum * 0.10).toInt().coerceAtLeast(1)
        require(sanity.current >= psCost) {
            "Sanidade insuficiente para conversão (necessário $psCost PS)."
        }
        copy(
            sanity = sanity.copy(current = sanity.current - psCost),
            destiny = destiny.copy(current = (destiny.current + 1).coerceAtMost(cap), maximum = cap),
        )
    }
}

/** Apply source- and tier-aware Divine Corruption delta, clamped to 0..100 (`Corrupção Divina.md:23, 49-51`). */
fun Character.applyDivineCorruption(
    sourceTier: DivineTier? = null,
    sourceDeityId: String = "",
    rawAmount: Int = DivineRules.DEFAULT_CORRUPTION_GAIN,
): Character {
    val appliedDelta = DivineRules.calculateCorruptionDelta(
        targetTier = divineAscension.tier,
        targetPatronDeityId = divineAscension.patronDeityId,
        sourceTier = sourceTier,
        sourceDeityId = sourceDeityId,
        rawAmount = rawAmount,
    )
    val nextValue = (corruption.current + appliedDelta).coerceIn(0, 100)
    return withDivineCorruption(corruption.copy(current = nextValue, maximum = 100))
}

fun Character.withAddedDivineMark(mark: DivineMark): Character =
    copy(divineRuntime = divineRuntime.copy(marks = divineRuntime.marks + mark))

fun Character.withUpdatedDivineMark(mark: DivineMark): Character =
    copy(
        divineRuntime = divineRuntime.copy(
            marks = divineRuntime.marks.map { if (it.id == mark.id) mark else it },
        ),
    )

fun Character.withRemovedDivineMark(markId: String): Character =
    copy(
        divineRuntime = divineRuntime.copy(
            marks = divineRuntime.marks.filterNot { it.id == markId },
        ),
    )

/** Apply an absolute Destiny edit while preserving cumulative Avatar expenditure. */
fun Character.withTrackedDestiny(value: ResourceValue): Character {
    val next = value.copy(current = value.current.coerceIn(0, destinyMaximum), maximum = destinyMaximum)
    val spent = (destiny.current - next.current).coerceAtLeast(0)
    return copy(
        destiny = next,
        divineRuntime = if (divineAscension.tier == DivineTier.AVATAR) {
            divineRuntime.copy(pdSpentInCurrentScene = divineRuntime.pdSpentInCurrentScene + spent)
        } else divineRuntime,
    )
}

/** Register voluntary channeling, including zero-cost uses, once per scene. */
fun Character.channelDivineOrArcanePower(): Character {
    require(divineRuntime.divineFate != DivineFate.SEALED && divineRuntime.divineFate != DivineFate.ERASED) {
        "Entidade selada ou apagada n\u00e3o pode canalizar poder."
    }
    require(corruption.current < 100) { "Ruptura impede canalização voluntária." }
    val exhausted = corruption.current >= 75 && !divineRuntime.channeledInCurrentScene
    return copy(
        exhaustion = if (exhausted) exhaustion.copy(current = exhaustion.current + 1) else exhaustion,
        divineRuntime = divineRuntime.copy(channeledInCurrentScene = true),
    )
}

/** Absolute corruption update shared by exposure, session commands, and manual edits. */
fun Character.withDivineCorruption(value: ResourceValue): Character {
    val updated = copy(corruption = value.copy(current = value.current.coerceIn(0, 100), maximum = 100))
    if (updated.corruption.current < 100) return updated
    return updated.copy(
        activeModifiers = updated.activeModifiers.filterNot { it.duration?.requiresChanneling == true },
    ).withCanonicalConditions(
        updated.canonicalConditions().filterNot { it.duration?.requiresChanneling == true },
    )
}
