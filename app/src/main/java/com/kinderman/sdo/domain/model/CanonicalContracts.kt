package com.kinderman.sdo.domain.model

import java.util.UUID
import kotlin.math.ceil

/** Schema implemented from the canonical rules contract. Never reuse a revision number. */
const val CANONICAL_SCHEMA_VERSION = 1
const val CANONICAL_RULES_COMMIT = "86102242603058895e49129ee0f88d66a9a317bb"

@JvmInline value class AbilityCatalogId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class ItemCatalogId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class ItemInstanceId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class ConditionId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class ConditionInstanceId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class SpecialKnowledgeId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class CharacterId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class PathTemplateId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class BackgroundId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class ProfessionId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class NarrativeSourceId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class AmmoTypeId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class InstallationId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class EntityId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class CatalogEntryId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class KnowledgeId(val value: String) { init { require(value.isNotBlank()) } }

data class CatalogReference<T>(val id: T, val revision: Int) {
    init { require(revision > 0) { "A revisão de catálogo deve ser positiva." } }
}

enum class CatalogScope { Canonical, Campaign, Character }

enum class Attribute { FOR, VIG, AGI, POD, INT, CAR }

enum class Resource { PV, PS, PM, PE, PD, Exaustao, CorrupcaoDivina }

enum class SpendableResource { PV, PS, PM, PE, PD }

enum class TrackedMarker { Exaustao, CorrupcaoDivina }

enum class BasicKnowledge(val attribute: Attribute) {
    Atletismo(Attribute.FOR), Brutalidade(Attribute.FOR), Luta(Attribute.FOR), Arremesso(Attribute.FOR),
    Energia(Attribute.VIG), Vitalidade(Attribute.VIG), Tolerancia(Attribute.VIG), Regeneracao(Attribute.VIG),
    Furtividade(Attribute.AGI), Reflexos(Attribute.AGI), Movimento(Attribute.AGI), Pontaria(Attribute.AGI),
    Arcano(Attribute.POD), Sentidos(Attribute.POD), Controle(Attribute.POD), Recuperacao(Attribute.POD),
    Sanidade(Attribute.INT), Intuicao(Attribute.INT), Religiao(Attribute.INT), Raciocinio(Attribute.INT),
    Politica(Attribute.CAR), Labia(Attribute.CAR), Enganacao(Attribute.CAR), Intimidacao(Attribute.CAR);

    val label: String get() = when (this) {
        Tolerancia -> "Tolerância"
        Regeneracao -> "Regeneração"
        Intuicao -> "Intuição"
        Religiao -> "Religião"
        Raciocinio -> "Raciocínio"
        Politica -> "Política"
        Labia -> "Lábia"
        Enganacao -> "Enganação"
        Intimidacao -> "Intimidação"
        Recuperacao -> "Recuperação"
        else -> name
    }
}

enum class Protection { Geral, Esquiva, Postura, Mental, Arcana }

enum class Resistance {
    None, Geral, Esquiva, Postura, Mental, Arcana;

    companion object {
        fun fromLegacy(value: String): Resistance = when (value.uppercase().trim()) {
            "PG", "GERAL" -> Geral
            "PE", "ESQUIVA" -> Esquiva
            "PP", "POSTURA" -> Postura
            "PMENTAL", "MENTAL" -> Mental
            "PA", "ARCANA" -> Arcana
            else -> None
        }
    }
}

enum class RaceId {
    Skayra, Humano, Elfo, Ascendido, ElfoDoCrepusculo, Golm,
    Ciuvati, CriaDaNeblina, Kaltoch, Anao, Sonaris, Goblin,
    Ovaryn, Orc, Tritao, Fada, SangueVil, Aviano, Lumen
}

enum class KaltochVariant { Andarilho, Aumentado }

enum class SubraceKind { Oraculo, BestialContaminado, BestialCompleto, Aumentado }

enum class KnowledgeCategory { Adquirido, Arcano, TecnicaDeCombate }

enum class ArcaneKnowledgeKind { ArcaneSchool, ArcaneStudy }

enum class BodyRegionSlot(val label: String, val roll: Int) {
    Head("Cabeça", 1),
    Torso("Torso", 2),
    ArmLeft("Braço esquerdo", 3),
    ArmRight("Braço direito", 4),
    HandLeft("Mão esquerda", 5),
    HandRight("Mão direita", 6),
    LegLeft("Perna esquerda", 7),
    LegRight("Perna direita", 8),
    FootLeft("Pé esquerdo", 9),
    FootRight("Pé direito", 10);

    companion object {
        fun fromName(value: String): BodyRegionSlot? {
            val norm = normalizeAbilityName(value)
            return entries.firstOrNull {
                normalizeAbilityName(it.label) == norm || normalizeAbilityName(it.name) == norm
            }
        }
    }
}

enum class OrganSlot(val label: String) {
    Brain("Cérebro"),
    HeartOrCore("Coração / Núcleo"),
    LungsOrRespiratory("Pulmões / Respiratório"),
    LiverOrFilter("Fígado / Filtro"),
    Other("Outro");

    companion object {
        fun fromName(value: String): OrganSlot = entries.firstOrNull {
            normalizeAbilityName(it.label) == normalizeAbilityName(value) ||
            normalizeAbilityName(it.name) == normalizeAbilityName(value)
        } ?: Other
    }
}

enum class BodyIntegrity(val label: String) {
    Intact("Intacto"),
    Stabilized("Estabilizado"),
    Damaged("Danificado"),
    Destroyed("Destruído"),
    Missing("Ausente")
}

enum class ConditionKind(val label: String) {
    Abalado("Abalado"),
    Caido("Caído"),
    Cego("Cego"),
    Surpreendido("Surpreendido"),
    Imobilizado("Imobilizado"),
    Sangramento("Sangramento"),
    Queimando("Queimando"),
    Envenenado("Envenenado"),
    Paralisia("Paralisia"),
    Asfixia("Asfixia"),
    Dor("Dor"),
    Intoxicacao("Intoxicação"),
    Dependencia("Dependência"),
    Exaustao("Exaustão"),
    Inconsciente("Inconsciente");

    companion object {
        fun fromName(value: String): ConditionKind? = entries.firstOrNull {
            normalizeAbilityName(it.label) == normalizeAbilityName(value) ||
            normalizeAbilityName(it.name) == normalizeAbilityName(value)
        }
    }
}

enum class AshOrigin(val label: String) {
    Fire("Fogo"), Cold("Frio"), Lightning("Raio"), Healing("Cura"), Oneiric("Onírico"),
    Acid("Ácido"), Concussive("Concussivo"), Earth("Terra"), Nature("Natureza"),
    Poison("Venenoso"), Water("Água"), Air("Ar"), Sound("Som"), Light("Luz"),
    Darkness("Trevas"), Mental("Mental"), Illusion("Ilusão"), Blood("Sangue"),
    Technology("Tecnologia"), Decay("Decadência"), Death("Morte"), Divine("Divino");

    companion object {
        fun fromName(value: String): AshOrigin = entries.firstOrNull {
            it.name.equals(value, true) || it.label.equals(value, true)
        } ?: Fire
    }
}

enum class ExecutionKind(val label: String) {
    Action("Ação"), Turn("Turno"), Free("Livre"), Reaction("Reação"), Passive("Passiva"), Timed("Tempo");

    companion object {
        fun fromName(value: String): ExecutionKind = entries.firstOrNull {
            it.name.equals(value, true) || it.label.equals(value, true)
        } ?: Action
    }
}

enum class ExecutionTimeUnit(val label: String) {
    Minutes("Minutos"), Hours("Horas"), Days("Dias")
}

enum class RangeBand(val label: String) {
    Personal("Pessoal"), Short("Curto (até 9 m)"), Medium("Médio (até 30 m)"),
    Long("Longo (até 90 m)"), Undefined("Indefinido");

    companion object {
        fun fromName(value: String): RangeBand = entries.firstOrNull {
            it.name.equals(value, true) || it.label.startsWith(value, true)
        } ?: Personal
    }
}

enum class DurationKind(val label: String) {
    Instant("Instantânea"), Turns("Turnos"), Scene("Cena"), Session("Sessão"), Timed("Tempo");

    companion object {
        fun fromName(value: String): DurationKind = entries.firstOrNull {
            it.name.equals(value, true) || it.label.equals(value, true)
        } ?: Instant
    }
}

enum class DurationTimeUnit(val label: String) {
    Hours("Horas"), Days("Dias")
}

enum class DamageType(val label: String) {
    Physical("Físico"), Fire("Fogo"), Cold("Frio"), Lightning("Raio"), Acid("Ácido"),
    Poison("Veneno"), Mental("Mental"), Arcane("Arcano"), True("Verdadeiro")
}

enum class StackingRule(val label: String) {
    Add("Acumular"), HighestOnly("Apenas o maior"), ReplaceBySource("Substituir por fonte")
}

enum class AbilityKind(val label: String) {
    POWER("Poder"), SPELL("Magia"), RUNE("Runa"), ASH("Cinza");

    companion object {
        fun fromName(value: String): AbilityKind = entries.firstOrNull {
            it.name.equals(value, true) || it.label.equals(value, true)
        } ?: POWER
    }
}

enum class QualityGrade(val label: String) {
    Mundana("Mundana"), Comum("Comum"), Aprimorada("Aprimorada"),
    Iconica("Icônica"), ObraPrima("Obra-Prima"), Artefato("Artefato"), Ancia("Anciã")
}

enum class DerivedFormulaId {
    VidaMax, SanidadeMax, ArcanoMax, EnergiaMax, DestinoMax,
    ProtecaoGeral, ProtecaoEsquiva, ProtecaoPostura, ProtecaoMental,
    ProtecaoArcana, CargaMaxima, LimiteImplantes, Deslocamento,
}

enum class SourceKind { Knowledge, Race, Path, Item, Background, Profession, Narrative }

sealed interface KnowledgeSourceTarget {
    data class Basic(val basic: BasicKnowledge) : KnowledgeSourceTarget
    data class Special(val specialId: SpecialKnowledgeId) : KnowledgeSourceTarget
}

sealed interface SourceRef {
    val kind: SourceKind

    data class Knowledge(val target: KnowledgeSourceTarget) : SourceRef {
        override val kind: SourceKind get() = SourceKind.Knowledge
    }
    data class Race(val raceId: RaceId, val powerCatalogId: AbilityCatalogId? = null) : SourceRef {
        override val kind: SourceKind get() = SourceKind.Race
    }
    data class Path(val pathId: PathTemplateId, val powerCatalogId: AbilityCatalogId? = null) : SourceRef {
        override val kind: SourceKind get() = SourceKind.Path
    }
    data class Item(val itemInstanceId: ItemInstanceId) : SourceRef {
        override val kind: SourceKind get() = SourceKind.Item
    }
    data class Background(val backgroundId: BackgroundId) : SourceRef {
        override val kind: SourceKind get() = SourceKind.Background
    }
    data class Profession(val professionId: ProfessionId) : SourceRef {
        override val kind: SourceKind get() = SourceKind.Profession
    }
    data class Narrative(val narrativeSourceId: NarrativeSourceId) : SourceRef {
        override val kind: SourceKind get() = SourceKind.Narrative
    }
}

data class SourceKnowledge(
    val target: KnowledgeSourceTarget,
    val level: Int,
) {
    init { require(level >= 0) { "Nível de conhecimento deve ser >= 0." } }
}

data class Source(
    val kind: SourceKind,
    val sourceRef: SourceRef,
    val knowledge: SourceKnowledge? = null,
) {
    init {
        require(sourceRef.kind == kind) { "sourceRef.kind (${sourceRef.kind}) must match kind ($kind)" }
        require((kind == SourceKind.Knowledge) == (knowledge != null)) {
            "knowledge must be present if and only if kind is Knowledge"
        }
    }

    companion object {
        fun knowledge(target: KnowledgeSourceTarget, level: Int) = Source(
            kind = SourceKind.Knowledge,
            sourceRef = SourceRef.Knowledge(target),
            knowledge = SourceKnowledge(target, level),
        )
        fun race(raceId: RaceId, powerCatalogId: AbilityCatalogId? = null) = Source(
            kind = SourceKind.Race,
            sourceRef = SourceRef.Race(raceId, powerCatalogId),
        )
        fun path(pathId: PathTemplateId, powerCatalogId: AbilityCatalogId? = null) = Source(
            kind = SourceKind.Path,
            sourceRef = SourceRef.Path(pathId, powerCatalogId),
        )
        fun item(itemInstanceId: ItemInstanceId) = Source(
            kind = SourceKind.Item,
            sourceRef = SourceRef.Item(itemInstanceId),
        )
        fun background(backgroundId: BackgroundId) = Source(
            kind = SourceKind.Background,
            sourceRef = SourceRef.Background(backgroundId),
        )
        fun profession(professionId: ProfessionId) = Source(
            kind = SourceKind.Profession,
            sourceRef = SourceRef.Profession(professionId),
        )
        fun narrative(narrativeSourceId: NarrativeSourceId) = Source(
            kind = SourceKind.Narrative,
            sourceRef = SourceRef.Narrative(narrativeSourceId),
        )
    }
}

data class TimedExecution(val amount: Int, val unit: ExecutionTimeUnit) {
    init { require(amount > 0) }
}

data class Execution(
    val kind: ExecutionKind = ExecutionKind.Action,
    val timed: TimedExecution? = null,
) {
    init {
        require((kind == ExecutionKind.Timed) == (timed != null)) {
            "timed deve existir se e somente se kind for Timed."
        }
    }
}

data class RangeSpec(
    val band: RangeBand = RangeBand.Personal,
    val targetArea: String? = null,
)

data class TimedDuration(val amount: Int, val unit: DurationTimeUnit) {
    init { require(amount > 0) }
}

data class Duration(
    val kind: DurationKind,
    val turns: Int? = null,
    val timed: TimedDuration? = null,
) {
    init {
        require((kind == DurationKind.Turns) == (turns != null)) {
            "turns deve existir se e somente se kind for Turns."
        }
        require((kind == DurationKind.Timed) == (timed != null)) {
            "timed deve existir se e somente se kind for Timed."
        }
        turns?.let { require(it > 0) }
    }
}

data class Dice(val count: Int = 1, val die: Int = 6, val bonus: Int = 0) {
    init {
        require(count > 0) { "Quantidade de dados deve ser positiva." }
        require(die in setOf(4, 6, 8, 10, 12, 20)) { "Dado deve ser 4, 6, 8, 10, 12 ou 20." }
    }
}

sealed interface DiceOrNumber {
    data class Fixed(val value: Int) : DiceOrNumber
    data class Roll(val dice: Dice) : DiceOrNumber
}

sealed interface TargetSpec {
    val kind: String
    data object Self : TargetSpec { override val kind: String get() = "Self" }
    data class TargetCharacter(val characterId: CharacterId) : TargetSpec { override val kind: String get() = "Character" }
    data class TargetRegion(val slot: BodyRegionSlot) : TargetSpec { override val kind: String get() = "Region" }
    data class TargetOrgan(val slot: OrganSlot) : TargetSpec { override val kind: String get() = "Organ" }
    data class TargetArea(val description: String) : TargetSpec { override val kind: String get() = "Area" }
}

sealed interface BonusTarget {
    val kind: String
    data class AttributeTarget(val attribute: Attribute) : BonusTarget { override val kind: String get() = "Attribute" }
    data class BasicKnowledgeTarget(val knowledge: BasicKnowledge) : BonusTarget { override val kind: String get() = "BasicKnowledge" }
    data class SpecialKnowledgeTarget(val specialKnowledgeId: SpecialKnowledgeId) : BonusTarget { override val kind: String get() = "SpecialKnowledge" }
    data class ResourceMaxTarget(val resource: SpendableResource) : BonusTarget { override val kind: String get() = "ResourceMax" }
    data class ProtectionTarget(val protection: Protection) : BonusTarget { override val kind: String get() = "Protection" }
}

data class PermanentBonus(
    val target: BonusTarget,
    val amount: Int = 0,
)

sealed interface PassiveState {
    data class ManualToggle(val active: Boolean = false) : PassiveState
    data object ItemBound : PassiveState
}

enum class EffectOperationKind { Damage, Healing, ApplyCondition, AddModifier, SpendResource, ConsumeItemState }
enum class ConsumableStateKind { Dose, Charge, Ammo }

sealed interface EffectOperation {
    val kind: EffectOperationKind

    data class Damage(
        val damageType: DamageType = DamageType.Physical,
        val amount: DiceOrNumber = DiceOrNumber.Fixed(1),
        val target: TargetSpec = TargetSpec.Self,
    ) : EffectOperation {
        override val kind: EffectOperationKind get() = EffectOperationKind.Damage
    }

    data class Healing(
        val resource: SpendableResource = SpendableResource.PV,
        val amount: DiceOrNumber = DiceOrNumber.Fixed(1),
        val target: TargetSpec = TargetSpec.Self,
    ) : EffectOperation {
        override val kind: EffectOperationKind get() = EffectOperationKind.Healing
    }

    data class ApplyCondition(
        val condition: ConditionPayload,
        val target: TargetSpec = TargetSpec.Self,
    ) : EffectOperation {
        override val kind: EffectOperationKind get() = EffectOperationKind.ApplyCondition
    }

    data class AddModifier(
        val modifier: ActiveModifier,
    ) : EffectOperation {
        override val kind: EffectOperationKind get() = EffectOperationKind.AddModifier
    }

    data class SpendResource(
        val resource: SpendableResource = SpendableResource.PE,
        val amount: Int = 1,
    ) : EffectOperation {
        init { require(amount > 0) { "Quantidade a gastar deve ser positiva." } }
        override val kind: EffectOperationKind get() = EffectOperationKind.SpendResource
    }

    data class ConsumeItemState(
        val itemInstanceId: ItemInstanceId,
        val amount: Int = 1,
        val state: ConsumableStateKind = ConsumableStateKind.Dose,
    ) : EffectOperation {
        init { require(amount > 0) { "Quantidade a consumir deve ser positiva." } }
        override val kind: EffectOperationKind get() = EffectOperationKind.ConsumeItemState
    }
}

data class MechanicalEffect(
    val operations: List<EffectOperation> = emptyList(),
    val usage: String? = null,
    val trigger: String? = null,
) {
    init { require(operations.isNotEmpty()) { "Um efeito mecânico deve possuir operações." } }
}

enum class ConditionCadence { OnApply, EndOfTurn, StartOfTurn, Daily }

data class ConditionPayload(
    val kind: ConditionKind = ConditionKind.Abalado,
    val intensity: Int? = null,
    val damage: Dice? = null,
    val difficultyClass: Int? = null,
    val duration: Duration? = null,
    val cadence: ConditionCadence? = null,
    val targetRegion: BodyRegionSlot? = null,
    val targetOrgan: OrganSlot? = null,
    val endsWhen: List<String> = emptyList(),
) {
    init {
        intensity?.let { require(it > 0) { "Intensidade deve ser positiva." } }
        difficultyClass?.let { require(it > 0) { "CD deve ser positiva." } }
    }
}

data class ConditionInstance(
    val instanceId: ConditionInstanceId = ConditionInstanceId(UUID.randomUUID().toString()),
    val payload: ConditionPayload = ConditionPayload(),
    val source: Source = Source.narrative(NarrativeSourceId("init")),
    val appliedAt: Long = System.currentTimeMillis(),
) {
    val kind: ConditionKind get() = payload.kind
    val name: String get() = payload.kind.label
    val intensity: Int? get() = payload.intensity
    val duration: Duration? get() = payload.duration
}

data class ConditionDefinition(
    val reference: CatalogReference<ConditionId>,
    val name: String,
    val description: String,
    val stacking: StackingRule = StackingRule.Add,
    val defaultPayload: ConditionPayload,
)

data class ActiveModifier(
    val modifierId: String = UUID.randomUUID().toString(),
    val target: BonusTarget,
    val amount: Int = 0,
    val source: Source,
    val duration: Duration? = null,
    val stacking: StackingRule = StackingRule.Add,
    val activation: String? = null,
)

data class DamageAmount(val type: DamageType, val amount: Int) {
    init { require(amount >= 0) }
}

data class InjuryEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val source: Source = Source.narrative(NarrativeSourceId("injury")),
    val occurredAt: Long = System.currentTimeMillis(),
    val damage: DamageAmount? = null,
    val failuresAdded: Int = 0,
)

data class EquippedProtection(
    val region: BodyRegionSlot = BodyRegionSlot.Torso,
    val generalProtectionBonus: Int = 0,
    val localProtection: Int = 0,
)

data class BodyRegionState(
    val region: BodyRegionSlot = BodyRegionSlot.Torso,
    val state: BodyIntegrity = BodyIntegrity.Intact,
    val failures: Int = 0,
    val protection: EquippedProtection = EquippedProtection(region),
    val implantInstanceIds: List<ItemInstanceId> = emptyList(),
    val prosthesisInstanceId: ItemInstanceId? = null,
    val injuries: List<InjuryEvent> = emptyList(),
    val equippedItemIds: List<ItemInstanceId> = emptyList(),
) {
    init { require(failures in 0..4) { "Falhas na região corporal devem estar entre 0 e 4." } }
    val name: String get() = region.label
    val roll: Int get() = region.roll
}

data class OrganState(
    val organ: OrganSlot = OrganSlot.HeartOrCore,
    val state: BodyIntegrity = BodyIntegrity.Intact,
    val failures: Int = 0,
    val implantInstanceId: ItemInstanceId? = null,
    val injuries: List<InjuryEvent> = emptyList(),
    val customName: String = "",
    val effectNotes: String = "",
) {
    init { require(failures in 0..3) { "Falhas no órgão devem estar entre 0 e 3." } }
    val name: String get() = customName.ifBlank { organ.label }
}

data class BodyState(
    val regions: List<BodyRegionState> = BodyRegionSlot.entries.map { BodyRegionState(it) },
    val organs: List<OrganState> = emptyList(),
) {
    fun region(slot: BodyRegionSlot): BodyRegionState =
        regions.firstOrNull { it.region == slot } ?: BodyRegionState(slot)

    fun withUpdatedRegion(slot: BodyRegionSlot, transform: (BodyRegionState) -> BodyRegionState): BodyState {
        val updated = regions.map { if (it.region == slot) transform(it) else it }
        return copy(regions = updated)
    }

    fun withInjury(slot: BodyRegionSlot, event: InjuryEvent): BodyState = withUpdatedRegion(slot) { current ->
        val newFailures = (current.failures + event.failuresAdded).coerceIn(0, 4)
        val newState = if (newFailures >= 4) BodyIntegrity.Destroyed else if (newFailures > 0) BodyIntegrity.Damaged else current.state
        current.copy(
            failures = newFailures,
            state = newState,
            injuries = current.injuries + event,
        )
    }
}

sealed interface AbilityCost {
    val amount: Int

    data class PowerCost(
        override val amount: Int = 0,
        val resource: SpendableResource = SpendableResource.PE,
        val isDivineOrLuckBased: Boolean = false,
    ) : AbilityCost {
        init {
            require(resource != SpendableResource.PM) { "Poder nunca usa PM." }
            require(amount >= 0) { "Quantidade de custo de Poder deve ser >= 0." }
        }
    }

    data class SpellCost(override val amount: Int = 1) : AbilityCost {
        init { require(amount >= 0) { "Custo de Magia deve ser >= 0." } }
        val resource: SpendableResource get() = SpendableResource.PM
    }

    data class RuneCost(override val amount: Int = 1) : AbilityCost {
        init { require(amount >= 0) { "Custo de Runa deve ser >= 0." } }
        val resource: SpendableResource get() = SpendableResource.PM
    }

    data class AshCost(override val amount: Int = 1) : AbilityCost {
        init { require(amount >= 1) { "Custo de Cinza deve ser no mínimo 1 dose." } }
        val resource: String get() = "Dose"
    }
}

data class SpellData(val level: Int = 1) {
    init { require(level >= 1) { "Nível de Magia deve ser >= 1." } }
}

data class RuneData(
    val level: Int = 1,
    val inscriberId: CharacterId? = null,
    val inscriberPower: Int? = null,
    val inscriberRunicKnowledge: Int? = null,
) {
    init { require(level in 1..3) { "Nível de Runa deve ser 1, 2 ou 3." } }
}

data class AshData(
    val origin: AshOrigin = AshOrigin.Fire,
    val purity: AshPurity = AshPurity.RAW,
    val linkedItemId: ItemInstanceId,
)

data class PowerData(
    val grantsPermanentBonus: Boolean = false,
    val permanentBonuses: List<PermanentBonus> = emptyList(),
    val passiveState: PassiveState? = null,
)

data class Ability(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: AbilityKind,
    val source: Source? = null,
    val execution: Execution = Execution(ExecutionKind.Action),
    val range: RangeSpec = RangeSpec(RangeBand.Personal),
    val duration: Duration? = Duration(DurationKind.Instant),
    val resistance: Resistance = Resistance.None,
    val cost: AbilityCost,
    val effect: String = "",
    val mechanicalEffect: MechanicalEffect? = null,
    val definition: CatalogReference<AbilityCatalogId>? = null,
    val spellData: SpellData? = null,
    val runeData: RuneData? = null,
    val ashData: AshData? = null,
    val powerData: PowerData? = null,
    val favorite: Boolean = false,
    val available: Boolean = true,
    val revision: Int = 1,
) {
    init {
        require(name.isNotBlank()) { "O nome da habilidade é obrigatório." }
        require(revision > 0) { "A revisão deve ser positiva." }
    }
}

data class UniquenessKey(
    val type: AbilityKind,
    val normalizedName: String,
    val purity: AshPurity? = null,
)

fun Ability.uniquenessKey(): UniquenessKey = UniquenessKey(
    type = kind,
    normalizedName = normalizeAbilityName(name),
    purity = ashData?.purity,
)

fun ashCargo(purity: AshPurity, doses: Int): Int {
    if (doses <= 0) return 0
    val divisor = when (purity) {
        AshPurity.RAW -> 1
        AshPurity.REFINED -> 2
        AshPurity.PURE -> 3
    }
    return ceil(doses.toDouble() / divisor).toInt()
}

data class DerivedValue(val formulaId: DerivedFormulaId, val value: Int)

object DerivedValueService {
    fun resources(character: Character): List<DerivedValue> = listOf(
        DerivedValue(DerivedFormulaId.VidaMax, character.lifeMaximum),
        DerivedValue(DerivedFormulaId.SanidadeMax, character.sanityMaximum),
        DerivedValue(DerivedFormulaId.ArcanoMax, character.arcaneMaximum),
        DerivedValue(DerivedFormulaId.EnergiaMax, character.energyMaximum),
        DerivedValue(DerivedFormulaId.DestinoMax, character.destinyMaximum),
    )

    fun protections(character: Character): List<DerivedValue> = listOf(
        DerivedValue(DerivedFormulaId.ProtecaoGeral, character.protectionTotal("Geral")),
        DerivedValue(DerivedFormulaId.ProtecaoEsquiva, character.protectionTotal("Esquiva")),
        DerivedValue(DerivedFormulaId.ProtecaoPostura, character.protectionTotal("Postura")),
        DerivedValue(DerivedFormulaId.ProtecaoMental, character.protectionTotal("Mental")),
        DerivedValue(DerivedFormulaId.ProtecaoArcana, character.protectionTotal("Arcana")),
    )

    fun load(character: Character) = DerivedValue(DerivedFormulaId.CargaMaxima, character.maximumLoad)
    fun implantLimit(character: Character) = DerivedValue(
        DerivedFormulaId.LimiteImplantes,
        (2 + (character.attributes.firstOrNull { it.acronym.equals("VIG", true) }?.value ?: 0)).coerceAtLeast(2),
    )
    fun movement(character: Character, baseMeters: Int = 9) = DerivedValue(
        DerivedFormulaId.Deslocamento,
        (baseMeters + character.movementPenaltyMeters).coerceAtLeast(0),
    )
}

sealed class DomainError(message: String) : IllegalArgumentException(message) {
    class InvalidReference(message: String) : DomainError(message)
    class DuplicateIdentity(message: String) : DomainError(message)
    class InsufficientResource(message: String) : DomainError(message)
    class RevisionConflict(message: String) : DomainError(message)
    class LegacyWriteRejected(message: String = "Novas gravações no schema legado não são permitidas.") : DomainError(message)
    class ValidationError(message: String) : DomainError(message)
}

object CanonicalValidator {
    fun validate(ability: Ability): List<DomainError> = buildList {
        if (ability.name.isBlank()) add(DomainError.InvalidReference("Habilidade sem nome."))
        if (ability.revision <= 0) add(DomainError.InvalidReference("Habilidade sem revisão válida."))
        if (ability.kind == AbilityKind.ASH) {
            if (ability.source != null) add(DomainError.InvalidReference("Cinza não deve possuir Source."))
            if (ability.ashData == null) add(DomainError.InvalidReference("Cinza exige dados específicos (origem, pureza e item vinculado)."))
            if (ability.cost !is AbilityCost.AshCost) add(DomainError.InvalidReference("Cinza exige custo em doses."))
        } else {
            if (ability.source == null) add(DomainError.InvalidReference("Habilidade exige SourceRef."))
        }
        if (ability.execution.kind == ExecutionKind.Passive) {
            if (ability.duration != null) add(DomainError.InvalidReference("Habilidade passiva não deve ter duração."))
            if (ability.kind == AbilityKind.POWER && ability.cost.amount != 0) {
                add(DomainError.InvalidReference("Poder passivo deve ter custo zero."))
            }
        } else {
            if (ability.duration == null) add(DomainError.InvalidReference("Habilidade ativa exige duração."))
        }
        when (val cost = ability.cost) {
            is AbilityCost.PowerCost -> {
                if (ability.kind != AbilityKind.POWER) add(DomainError.InvalidReference("Custo de Poder em tipo não-Poder."))
                if (cost.resource == SpendableResource.PM) add(DomainError.InvalidReference("Poder nunca pode ter custo em PM."))
                if (cost.resource == SpendableResource.PD && !cost.isDivineOrLuckBased) {
                    add(DomainError.InvalidReference("Destino (PD) requer que a habilidade seja divina ou ligada a sorte/probabilidade."))
                }
            }
            is AbilityCost.SpellCost -> {
                if (ability.kind != AbilityKind.SPELL) add(DomainError.InvalidReference("Custo de Magia em tipo não-Magia."))
            }
            is AbilityCost.RuneCost -> {
                if (ability.kind != AbilityKind.RUNE) add(DomainError.InvalidReference("Custo de Runa em tipo não-Runa."))
            }
            is AbilityCost.AshCost -> {
                if (ability.kind != AbilityKind.ASH) add(DomainError.InvalidReference("Custo de Cinza em tipo não-Cinza."))
                if (cost.amount < 1) add(DomainError.InvalidReference("Doses de Cinza devem ser no mínimo 1."))
            }
        }
    }
}

data class PublishedCatalogEntry<T>(val reference: CatalogReference<*>, val value: T, val publishedAt: Long)

class ImmutableCatalog<T>(entries: Iterable<PublishedCatalogEntry<T>> = emptyList()) {
    private val revisions = entries.associateByTo(linkedMapOf()) { it.reference }

    fun publish(entry: PublishedCatalogEntry<T>) {
        if (revisions.putIfAbsent(entry.reference, entry) != null) {
            throw DomainError.RevisionConflict("A revisão ${entry.reference.revision} já foi publicada para ${entry.reference.id}.")
        }
    }

    fun resolve(reference: CatalogReference<*>): T = revisions[reference]?.value
        ?: throw DomainError.InvalidReference("Referência de catálogo inexistente: ${reference.id}@${reference.revision}.")

    fun snapshot(): List<PublishedCatalogEntry<T>> = revisions.values.toList()
}

data class RevisionedWrite<T>(val expectedRevision: Int, val value: T)

fun <T> compareAndAdvanceRevision(current: PublishedCatalogEntry<T>, write: RevisionedWrite<T>): PublishedCatalogEntry<T> {
    if (write.expectedRevision != current.reference.revision) {
        throw DomainError.RevisionConflict(
            "Conflito de revisão: esperado ${write.expectedRevision}, atual ${current.reference.revision}.",
        )
    }
    return PublishedCatalogEntry(
        reference = current.reference.copy(revision = current.reference.revision + 1),
        value = write.value,
        publishedAt = current.publishedAt,
    )
}

enum class ChoiceKind { CREATION, DOMAIN, PATH, PROGRESSION }

data class AuditableChoice(
    val id: EntityId,
    val kind: ChoiceKind,
    val option: CatalogReference<*>,
    val source: SourceRef,
    val grantedEntityIds: List<EntityId>,
    val chosenAt: Long,
)

data class CharacterProgression(val choices: List<AuditableChoice> = emptyList()) {
    fun register(choice: AuditableChoice): CharacterProgression {
        if (choices.any { it.id == choice.id }) return this
        val duplicatedGrant = choice.grantedEntityIds.toSet().intersect(choices.flatMap { it.grantedEntityIds }.toSet())
        if (duplicatedGrant.isNotEmpty()) {
            throw DomainError.DuplicateIdentity("Uma escolha não pode conceder a mesma entidade duas vezes.")
        }
        return copy(choices = choices + choice)
    }
}

enum class MigrationState { CURRENT, NEEDS_REVIEW }

data class NeedsReview(val field: String, val legacyValue: String, val reason: String)

data class MigrationResult<T>(val value: T, val state: MigrationState, val reviews: List<NeedsReview>)

object CanonicalMigration {
    fun migrateMechanicalText(description: String, mechanicalText: String): MigrationResult<String> {
        if (mechanicalText.isBlank()) return MigrationResult(description, MigrationState.CURRENT, emptyList())
        return MigrationResult(
            value = description,
            state = MigrationState.NEEDS_REVIEW,
            reviews = listOf(NeedsReview("mechanicalEffect", mechanicalText, "Efeito textual requer operação estruturada.")),
        )
    }
}

data class StackState(val quantity: Int = 1) { init { require(quantity >= 0) } }
data class ConsumableState(val doses: Int = 0, val charges: Int = 0, val ammunition: Int = 0) {
    init { require(doses >= 0 && charges >= 0 && ammunition >= 0) }
}
data class ReloadState(val loaded: Int = 0, val capacity: Int = 0) {
    init { require(loaded >= 0 && capacity >= 0 && loaded <= capacity) }
}
data class ContainerState(val containedItemIds: List<EntityId> = emptyList(), val capacity: Int = 0) {
    init { require(capacity >= 0 && containedItemIds.size <= capacity) }
}
data class ComponentInstallation(val id: InstallationId, val component: CatalogReference<*>, val installedAt: Long)
data class ItemState(
    val id: EntityId,
    val definition: CatalogReference<*>,
    val scope: CatalogScope,
    val material: CatalogReference<*>? = null,
    val installations: List<ComponentInstallation> = emptyList(),
    val stack: StackState = StackState(),
    val consumable: ConsumableState? = null,
    val reload: ReloadState? = null,
    val container: ContainerState? = null,
)

data class AshConsumptionResult(val ability: Ability, val item: ItemState)

fun consumeAshAtomically(ability: Ability, item: ItemState, doses: Int): AshConsumptionResult {
    require(ability.kind == AbilityKind.ASH) { "A habilidade não é uma Cinza." }
    require(doses > 0) { "Doses devem ser > 0." }
    val state = item.consumable ?: throw DomainError.InvalidReference("A Cinza não referencia um item consumível.")
    if (state.doses < doses) throw DomainError.InsufficientResource("Doses de Cinza insuficientes.")
    return AshConsumptionResult(ability, item.copy(consumable = state.copy(doses = state.doses - doses)))
}
