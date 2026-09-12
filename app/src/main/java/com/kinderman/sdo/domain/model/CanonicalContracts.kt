package com.kinderman.sdo.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.ceil

/** Schema implemented from the canonical rules contract. Never reuse a revision number. */
const val CANONICAL_SCHEMA_VERSION = 2
const val CANONICAL_RULES_COMMIT = "86102242603058895e49129ee0f88d66a9a317bb"

@Serializable
@JvmInline
value class AbilityCatalogId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class ItemCatalogId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class ItemInstanceId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class ConditionId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class ConditionInstanceId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class SpecialKnowledgeId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class CharacterId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class PathTemplateId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class BackgroundId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class ProfessionId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class NarrativeSourceId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class AmmoTypeId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class InstallationId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class EntityId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class CatalogEntryId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
@JvmInline
value class KnowledgeId(val value: String) { init {
    require(value.isNotBlank())
}
}

@Serializable
data class CatalogReference<T>(val id: T, val revision: Int) {
    init {
        require(revision > 0) { "A revisão de catálogo deve ser positiva." }
    }
}

@Serializable
enum class CatalogScope { Canonical, Campaign, Character }

@Serializable
enum class Attribute { FOR, VIG, AGI, POD, INT, CAR }

@Serializable
enum class Resource { PV, PS, PM, PE, PD, Exaustao, CorrupcaoDivina }

@Serializable
enum class SpendableResource { PV, PS, PM, PE, PD }

@Serializable
enum class TrackedMarker { Exaustao, CorrupcaoDivina }

@Serializable
enum class BasicKnowledge(val attribute: Attribute) {
    Atletismo(Attribute.FOR), Brutalidade(Attribute.FOR), Luta(Attribute.FOR), Arremesso(Attribute.FOR),
    Energia(Attribute.VIG), Vitalidade(Attribute.VIG), Tolerancia(Attribute.VIG), Regeneracao(
        Attribute.VIG
    ),
    Furtividade(Attribute.AGI), Reflexos(Attribute.AGI), Movimento(Attribute.AGI), Pontaria(
        Attribute.AGI
    ),
    Arcano(Attribute.POD), Sentidos(Attribute.POD), Controle(Attribute.POD), Recuperacao(Attribute.POD),
    Sanidade(Attribute.INT), Intuicao(Attribute.INT), Religiao(Attribute.INT), Raciocinio(Attribute.INT),
    Politica(Attribute.CAR), Labia(Attribute.CAR), Enganacao(Attribute.CAR), Intimidacao(Attribute.CAR);

    val label: String
        get() = when (this) {
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

@Serializable
enum class Protection { Geral, Esquiva, Postura, Mental, Arcana }

@Serializable
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

@Serializable
enum class RaceId {
    Skayra, Humano, Elfo, Ascendido, ElfoDoCrepusculo, Golm,
    Ciuvati, CriaDaNeblina, Kaltoch, Anao, Sonaris, Goblin,
    Ovaryn, Orc, Tritao, Fada, SangueVil, Aviano, Lumen
}

@Serializable
enum class KaltochVariant { Andarilho, Aumentado }

@Serializable
enum class SubraceKind { Oraculo, BestialContaminado, BestialCompleto, Aumentado }

@Serializable
enum class KnowledgeCategory { Adquirido, Arcano, TecnicaDeCombate }

@Serializable
enum class ArcaneKnowledgeKind { ArcaneSchool, ArcaneStudy }

@Serializable
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

@Serializable
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

@Serializable
enum class BodyIntegrity(val label: String) {
    Intact("Intacto"),
    Stabilized("Estabilizado"),
    Damaged("Danificado"),
    Destroyed("Destruído"),
    Missing("Ausente")
}

@Serializable
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

@Serializable
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

@Serializable
enum class ExecutionKind(val label: String) {
    Action("Ação"), Turn("Turno"), Free("Livre"), Reaction("Reação"), Passive("Passiva"), Timed("Tempo");

    companion object {
        fun fromName(value: String): ExecutionKind = entries.firstOrNull {
            it.name.equals(value, true) || it.label.equals(value, true)
        } ?: Action
    }
}

@Serializable
enum class ExecutionTimeUnit(val label: String) {
    Minutes("Minutos"), Hours("Horas"), Days("Dias")
}

@Serializable
enum class RangeBand(val label: String) {
    Personal("Pessoal"), Short("Curto (até 9 m)"), Medium("Médio (até 30 m)"),
    Long("Longo (até 90 m)"), Undefined("Indefinido");

    companion object {
        fun fromName(value: String): RangeBand = entries.firstOrNull {
            it.name.equals(value, true) || it.label.startsWith(value, true)
        } ?: Personal
    }
}

@Serializable
enum class DurationKind(val label: String) {
    Instant("Instantânea"), Turns("Turnos"), Scene("Cena"), Session("Sessão"), Timed("Tempo");

    companion object {
        fun fromName(value: String): DurationKind = entries.firstOrNull {
            it.name.equals(value, true) || it.label.equals(value, true)
        } ?: Instant
    }
}

@Serializable
enum class DurationTimeUnit(val label: String) {
    Hours("Horas"), Days("Dias")
}

@Serializable
enum class DamageType(val label: String) {
    Physical("Físico"), Fire("Fogo"), Cold("Frio"), Lightning("Raio"), Acid("Ácido"),
    Poison("Veneno"), Mental("Mental"), Arcane("Arcano"), True("Verdadeiro")
}

@Serializable
enum class StackingRule(val label: String) {
    Add("Acumular"), HighestOnly("Apenas o maior"), ReplaceBySource("Substituir por fonte")
}

@Serializable
enum class AbilityKind(val label: String) {
    POWER("Poder"), SPELL("Magia"), RUNE("Runa"), ASH("Cinza");

    companion object {
        fun fromName(value: String): AbilityKind = entries.firstOrNull {
            it.name.equals(value, true) || it.label.equals(value, true)
        } ?: POWER
    }
}

@Serializable
enum class QualityGrade(val label: String) {
    Mundana("Mundana"), Comum("Comum"), Aprimorada("Aprimorada"),
    Iconica("Icônica"), ObraPrima("Obra-Prima"), Artefato("Artefato"), Ancia("Anciã")
}

@Serializable
enum class DerivedFormulaId {
    VidaMax, SanidadeMax, ArcanoMax, EnergiaMax, DestinoMax,
    ProtecaoGeral, ProtecaoEsquiva, ProtecaoPostura, ProtecaoMental,
    ProtecaoArcana, CargaMaxima, LimiteImplantes, Deslocamento,
}

@Serializable
enum class SourceKind { Knowledge, Race, Path, Item, Background, Profession, Narrative }

@Serializable
sealed interface KnowledgeSourceTarget {
    @Serializable
    data class Basic(val basic: BasicKnowledge) : KnowledgeSourceTarget

    @Serializable
    data class Special(val specialId: SpecialKnowledgeId) : KnowledgeSourceTarget
}

@Serializable
sealed interface SourceRef {
    val kind: SourceKind

    @Serializable
    data class Knowledge(val target: KnowledgeSourceTarget) : SourceRef {
        override val kind: SourceKind get() = SourceKind.Knowledge
    }

    @Serializable
    data class Race(val raceId: RaceId, val powerCatalogId: AbilityCatalogId? = null) : SourceRef {
        override val kind: SourceKind get() = SourceKind.Race
    }

    @Serializable
    data class Path(val pathId: PathTemplateId, val powerCatalogId: AbilityCatalogId? = null) :
        SourceRef {
        override val kind: SourceKind get() = SourceKind.Path
    }

    @Serializable
    data class Item(val itemInstanceId: ItemInstanceId) : SourceRef {
        override val kind: SourceKind get() = SourceKind.Item
    }

    @Serializable
    data class Background(val backgroundId: BackgroundId) : SourceRef {
        override val kind: SourceKind get() = SourceKind.Background
    }

    @Serializable
    data class Profession(val professionId: ProfessionId) : SourceRef {
        override val kind: SourceKind get() = SourceKind.Profession
    }

    @Serializable
    data class Narrative(val narrativeSourceId: NarrativeSourceId) : SourceRef {
        override val kind: SourceKind get() = SourceKind.Narrative
    }
}

@Serializable
data class SourceKnowledge(
    val target: KnowledgeSourceTarget,
    val level: Int,
) {
    init {
        require(level >= 0) { "Nível de conhecimento deve ser >= 0." }
    }
}

@Serializable
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

@Serializable
data class TimedExecution(val amount: Int, val unit: ExecutionTimeUnit) {
    init {
        require(amount > 0)
    }
}

@Serializable
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

@Serializable
data class RangeSpec(
    val band: RangeBand = RangeBand.Personal,
    val targetArea: String? = null,
)

@Serializable
data class TimedDuration(val amount: Int, val unit: DurationTimeUnit) {
    init {
        require(amount > 0)
    }
}

@Serializable
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

@Serializable
data class Dice(val count: Int = 1, val die: Int = 6, val bonus: Int = 0) {
    init {
        require(count > 0) { "Quantidade de dados deve ser positiva." }
        require(die in setOf(4, 6, 8, 10, 12, 20)) { "Dado deve ser 4, 6, 8, 10, 12 ou 20." }
    }
}

@Serializable
sealed interface DiceOrNumber {
    @Serializable
    data class Fixed(val value: Int) : DiceOrNumber

    @Serializable
    data class Roll(val dice: Dice) : DiceOrNumber
}

@Serializable
sealed interface TargetSpec {
    val kind: String

    @Serializable
    data object Self : TargetSpec {
        override val kind: String get() = "Self"
    }

    @Serializable
    data class TargetCharacter(val characterId: CharacterId) : TargetSpec {
        override val kind: String get() = "Character"
    }

    @Serializable
    data class TargetRegion(val slot: BodyRegionSlot) : TargetSpec {
        override val kind: String get() = "Region"
    }

    @Serializable
    data class TargetOrgan(val slot: OrganSlot) : TargetSpec {
        override val kind: String get() = "Organ"
    }

    @Serializable
    data class TargetArea(val description: String) : TargetSpec {
        override val kind: String get() = "Area"
    }
}

@Serializable
sealed interface BonusTarget {
    val kind: String

    @Serializable
    data class AttributeTarget(val attribute: Attribute) : BonusTarget {
        override val kind: String get() = "Attribute"
    }

    @Serializable
    data class BasicKnowledgeTarget(val knowledge: BasicKnowledge) : BonusTarget {
        override val kind: String get() = "BasicKnowledge"
    }

    @Serializable
    data class SpecialKnowledgeTarget(val specialKnowledgeId: SpecialKnowledgeId) : BonusTarget {
        override val kind: String get() = "SpecialKnowledge"
    }

    @Serializable
    data class ResourceMaxTarget(val resource: SpendableResource) : BonusTarget {
        override val kind: String get() = "ResourceMax"
    }

    @Serializable
    data class ProtectionTarget(val protection: Protection) : BonusTarget {
        override val kind: String get() = "Protection"
    }
}

@Serializable
data class PermanentBonus(
    val target: BonusTarget,
    val amount: Int = 0,
)

@Serializable
sealed interface PassiveState {
    @Serializable
    data class ManualToggle(val active: Boolean = false) : PassiveState

    @Serializable
    data object ItemBound : PassiveState
}

@Serializable
enum class EffectOperationKind { Damage, Healing, ApplyCondition, AddModifier, SpendResource, ConsumeItemState }

@Serializable
enum class ConsumableStateKind { Dose, Charge, Ammo }

@Serializable
enum class UsagePeriod { Turn, Scene, Session, Day, Permanent }

@Serializable
data class UsageLimit(val uses: Int, val period: UsagePeriod) {
    init {
        require(uses > 0) { "O limite de usos deve ser positivo." }
    }
}

@Serializable
sealed interface TriggerSpec {
    @Serializable
    data object Manual : TriggerSpec

    @Serializable
    data object OnApply : TriggerSpec

    @Serializable
    data object StartOfTurn : TriggerSpec

    @Serializable
    data object EndOfTurn : TriggerSpec

    @Serializable
    data class OnCondition(val condition: ConditionKind) : TriggerSpec

    @Serializable
    data class OnResourceThreshold(val resource: SpendableResource, val atMost: Int) : TriggerSpec {
        init {
            require(atMost >= 0)
        }
    }
}

@Serializable
sealed interface EndCondition {
    @Serializable
    data object EndOfScene : EndCondition

    @Serializable
    data object EndOfSession : EndCondition

    @Serializable
    data object Rest : EndCondition

    @Serializable
    data class ResourceCheck(val resource: SpendableResource, val difficultyClass: Int) :
        EndCondition {
        init {
            require(difficultyClass > 0)
        }
    }

    @Serializable
    data class Narrative(val description: String) : EndCondition {
        init {
            require(description.isNotBlank())
        }
    }
}

fun EndCondition.displayText(): String = when (this) {
    EndCondition.EndOfScene -> "Fim da cena"
    EndCondition.EndOfSession -> "Fim da sessão"
    EndCondition.Rest -> "Descanso"
    is EndCondition.ResourceCheck -> "Teste de ${resource.name} CD $difficultyClass"
    is EndCondition.Narrative -> description
}

@Serializable
sealed interface EffectOperation {
    val kind: EffectOperationKind

    @Serializable
    data class Damage(
        val damageType: DamageType = DamageType.Physical,
        val amount: DiceOrNumber = DiceOrNumber.Fixed(1),
        val target: TargetSpec = TargetSpec.Self,
    ) : EffectOperation {
        override val kind: EffectOperationKind get() = EffectOperationKind.Damage
    }

    @Serializable
    data class Healing(
        val resource: SpendableResource = SpendableResource.PV,
        val amount: DiceOrNumber = DiceOrNumber.Fixed(1),
        val target: TargetSpec = TargetSpec.Self,
    ) : EffectOperation {
        override val kind: EffectOperationKind get() = EffectOperationKind.Healing
    }

    @Serializable
    data class ApplyCondition(
        val condition: ConditionPayload,
        val target: TargetSpec = TargetSpec.Self,
    ) : EffectOperation {
        override val kind: EffectOperationKind get() = EffectOperationKind.ApplyCondition
    }

    @Serializable
    data class AddModifier(
        val modifier: ActiveModifier,
    ) : EffectOperation {
        override val kind: EffectOperationKind get() = EffectOperationKind.AddModifier
    }

    @Serializable
    data class SpendResource(
        val resource: SpendableResource = SpendableResource.PE,
        val amount: Int = 1,
    ) : EffectOperation {
        init {
            require(amount > 0) { "Quantidade a gastar deve ser positiva." }
        }

        override val kind: EffectOperationKind get() = EffectOperationKind.SpendResource
    }

    @Serializable
    data class ConsumeItemState(
        val itemInstanceId: ItemInstanceId,
        val amount: Int = 1,
        val state: ConsumableStateKind = ConsumableStateKind.Dose,
    ) : EffectOperation {
        init {
            require(amount > 0) { "Quantidade a consumir deve ser positiva." }
        }

        override val kind: EffectOperationKind get() = EffectOperationKind.ConsumeItemState
    }
}

@Serializable
data class MechanicalEffect(
    val operations: List<EffectOperation> = emptyList(),
    val usage: UsageLimit? = null,
    val trigger: TriggerSpec? = null,
) {
    init {
        require(operations.isNotEmpty()) { "Um efeito mecânico deve possuir operações." }
    }
}

@Serializable
enum class ConditionCadence { OnApply, EndOfTurn, StartOfTurn, Daily }

@Serializable
data class ConditionPayload(
    val kind: ConditionKind = ConditionKind.Abalado,
    val intensity: Int? = null,
    val damage: Dice? = null,
    val difficultyClass: Int? = null,
    val duration: Duration? = null,
    val cadence: ConditionCadence? = null,
    val targetRegion: BodyRegionSlot? = null,
    val targetOrgan: OrganSlot? = null,
    val endsWhen: List<EndCondition> = emptyList(),
) {
    init {
        intensity?.let { require(it > 0) { "Intensidade deve ser positiva." } }
        difficultyClass?.let { require(it > 0) { "CD deve ser positiva." } }
    }
}

@Serializable
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

@Serializable
data class ConditionDefinition(
    val reference: CatalogReference<ConditionId>,
    val name: String,
    val description: String,
    val stacking: StackingRule = StackingRule.Add,
    val defaultPayload: ConditionPayload,
)

@Serializable
data class ActiveModifier(
    val modifierId: String = UUID.randomUUID().toString(),
    val target: BonusTarget,
    val amount: Int = 0,
    val source: Source,
    val duration: Duration? = null,
    val stacking: StackingRule = StackingRule.Add,
    val activation: TriggerSpec? = null,
)

@Serializable
data class DamageAmount(val type: DamageType, val amount: Int) {
    init {
        require(amount >= 0)
    }
}

@Serializable
data class InjuryEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val source: Source = Source.narrative(NarrativeSourceId("injury")),
    val occurredAt: Long = System.currentTimeMillis(),
    val damage: DamageAmount? = null,
    val failuresAdded: Int = 0,
)

@Serializable
data class EquippedProtection(
    val region: BodyRegionSlot = BodyRegionSlot.Torso,
    val generalProtectionBonus: Int = 0,
    val localProtection: Int = 0,
)

@Serializable
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
    init {
        require(failures in 0..4) { "Falhas na região corporal devem estar entre 0 e 4." }
    }

    val name: String get() = region.label
    val roll: Int get() = region.roll
}

@Serializable
data class OrganState(
    val organ: OrganSlot = OrganSlot.HeartOrCore,
    val state: BodyIntegrity = BodyIntegrity.Intact,
    val failures: Int = 0,
    val implantInstanceId: ItemInstanceId? = null,
    val injuries: List<InjuryEvent> = emptyList(),
    val customName: String = "",
    val effectNotes: String = "",
) {
    init {
        require(failures in 0..3) { "Falhas no órgão devem estar entre 0 e 3." }
    }

    val name: String get() = customName.ifBlank { organ.label }
}

@Serializable
data class BodyState(
    val regions: List<BodyRegionState> = BodyRegionSlot.entries.map { BodyRegionState(it) },
    val organs: List<OrganState> = emptyList(),
) {
    fun region(slot: BodyRegionSlot): BodyRegionState =
        regions.firstOrNull { it.region == slot } ?: BodyRegionState(slot)

    fun withUpdatedRegion(
        slot: BodyRegionSlot,
        transform: (BodyRegionState) -> BodyRegionState
    ): BodyState {
        val updated = regions.map { if (it.region == slot) transform(it) else it }
        return copy(regions = updated)
    }

    fun withInjury(slot: BodyRegionSlot, event: InjuryEvent): BodyState =
        withUpdatedRegion(slot) { current ->
            val newFailures = (current.failures + event.failuresAdded).coerceIn(0, 4)
            val newState =
                if (newFailures >= 4) BodyIntegrity.Destroyed else if (newFailures > 0) BodyIntegrity.Damaged else current.state
            current.copy(
                failures = newFailures,
                state = newState,
                injuries = current.injuries + event,
            )
        }
}

@Serializable
sealed interface AbilityCost {
    val amount: Int

    @Serializable
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

    @Serializable
    data class SpellCost(override val amount: Int = 1) : AbilityCost {
        init {
            require(amount >= 0) { "Custo de Magia deve ser >= 0." }
        }

        val resource: SpendableResource get() = SpendableResource.PM
    }

    @Serializable
    data class RuneCost(override val amount: Int = 1) : AbilityCost {
        init {
            require(amount >= 0) { "Custo de Runa deve ser >= 0." }
        }

        val resource: SpendableResource get() = SpendableResource.PM
    }

    @Serializable
    data class AshCost(override val amount: Int = 1) : AbilityCost {
        init {
            require(amount >= 1) { "Custo de Cinza deve ser no mínimo 1 dose." }
        }

        val resource: String get() = "Dose"
    }
}

@Serializable
data class SpellData(val level: Int = 1) {
    init {
        require(level >= 1) { "Nível de Magia deve ser >= 1." }
    }
}

@Serializable
data class RuneData(
    val level: Int = 1,
    val inscriberId: CharacterId? = null,
    val inscriberPower: Int? = null,
    val inscriberRunicKnowledge: Int? = null,
) {
    init {
        require(level in 1..3) { "Nível de Runa deve ser 1, 2 ou 3." }
    }
}

@Serializable
data class AshData(
    val origin: AshOrigin = AshOrigin.Fire,
    val purity: AshPurity = AshPurity.RAW,
    val linkedItemId: ItemInstanceId,
)

@Serializable
data class PowerData(
    val grantsPermanentBonus: Boolean = false,
    val permanentBonuses: List<PermanentBonus> = emptyList(),
    val passiveState: PassiveState? = null,
)

@Serializable
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

    fun load(character: Character) =
        DerivedValue(DerivedFormulaId.CargaMaxima, character.maximumLoad)

    fun implantLimit(character: Character) = DerivedValue(
        DerivedFormulaId.LimiteImplantes,
        (2 + (character.attributes.firstOrNull { it.acronym.equals("VIG", true) }?.value
            ?: 0)).coerceAtLeast(2),
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
    class UnsupportedSchemaVersion(message: String) : DomainError(message)
    class LegacyWriteRejected(message: String = "Novas gravações no schema legado não são permitidas.") :
        DomainError(message)

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
            if (ability.kind == AbilityKind.POWER && ability.cost.amount < 1) {
                add(DomainError.ValidationError("Poder ativo deve ter custo mínimo 1."))
            }
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
                if (ability.spellData == null) add(DomainError.ValidationError("Magia exige nível tipado."))
            }

            is AbilityCost.RuneCost -> {
                if (ability.kind != AbilityKind.RUNE) add(DomainError.InvalidReference("Custo de Runa em tipo não-Runa."))
                if (ability.runeData == null) add(DomainError.ValidationError("Runa exige dados de inscrição tipados."))
            }

            is AbilityCost.AshCost -> {
                if (ability.kind != AbilityKind.ASH) add(DomainError.InvalidReference("Custo de Cinza em tipo não-Cinza."))
                if (cost.amount < 1) add(DomainError.InvalidReference("Doses de Cinza devem ser no mínimo 1."))
            }
        }
    }

    fun validate(effect: MechanicalEffect): List<DomainError> = buildList {
        if (effect.operations.isEmpty()) add(DomainError.ValidationError("Efeito mecânico sem operações."))
        effect.operations.forEach { operation ->
            when (operation) {
                is EffectOperation.Damage -> validateAmount(operation.amount, "Dano")?.let(::add)
                is EffectOperation.Healing -> validateAmount(operation.amount, "Cura")?.let(::add)
                is EffectOperation.ApplyCondition -> addAll(validate(operation.condition))
                is EffectOperation.AddModifier -> addAll(validate(operation.modifier))
                is EffectOperation.SpendResource -> if (operation.amount <= 0) add(
                    DomainError.ValidationError(
                        "Gasto deve ser positivo."
                    )
                )

                is EffectOperation.ConsumeItemState -> if (operation.amount <= 0) add(
                    DomainError.ValidationError(
                        "Consumo deve ser positivo."
                    )
                )
            }
        }
    }

    fun validate(condition: ConditionPayload): List<DomainError> = buildList {
        if (condition.intensity != null && condition.intensity <= 0) add(
            DomainError.ValidationError(
                "Intensidade deve ser positiva."
            )
        )
        if (condition.difficultyClass != null && condition.difficultyClass <= 0) add(
            DomainError.ValidationError(
                "CD deve ser positiva."
            )
        )
        if (condition.targetRegion != null && condition.targetOrgan != null) add(
            DomainError.ValidationError(
                "Condição não pode apontar simultaneamente para região e órgão."
            )
        )
    }

    fun validate(modifier: ActiveModifier): List<DomainError> = buildList {
        if (modifier.modifierId.isBlank()) add(DomainError.InvalidReference("Modificador sem ID."))
        if (modifier.target is BonusTarget.ResourceMaxTarget && modifier.target.resource !in SpendableResource.entries) {
            add(DomainError.InvalidReference("Recurso inválido para máximo."))
        }
    }

    fun validate(body: BodyState): List<DomainError> = buildList {
        val duplicated =
            body.regions.groupingBy { it.region }.eachCount().filterValues { it > 1 }.keys
        if (duplicated.isNotEmpty()) add(DomainError.DuplicateIdentity("Regiões corporais duplicadas: ${duplicated.joinToString()}."))
        body.regions.filter { it.failures !in 0..4 }.forEach {
            add(DomainError.ValidationError("Falhas inválidas em ${it.region.label}."))
        }
        body.organs.filter { it.failures !in 0..3 }.forEach {
            add(DomainError.ValidationError("Falhas inválidas em ${it.organ.label}."))
        }
    }

    fun validate(item: ItemState): List<DomainError> = buildList {
        val duplicateInstallations =
            item.installations.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
        if (duplicateInstallations.isNotEmpty()) add(DomainError.DuplicateIdentity("InstallationId duplicado no item."))
        item.ammo?.let { ammo ->
            if (ammo.loaded !in 0..ammo.capacity) add(DomainError.ValidationError("Munição carregada fora da capacidade."))
        }
    }

    private fun validateAmount(amount: DiceOrNumber, label: String): DomainError? = when (amount) {
        is DiceOrNumber.Fixed -> if (amount.value < 0) DomainError.ValidationError("$label não pode ser negativo.") else null
        is DiceOrNumber.Roll -> null
    }

    fun validateSpecialKnowledge(
        knowledge: SpecialKnowledge,
        allKnowledges: List<SpecialKnowledge> = emptyList(),
    ): List<DomainError> = buildList {
        if (knowledge.name.isBlank()) add(DomainError.InvalidReference("Conhecimento sem nome."))
        val isSpecialization = knowledge.category.equals(
            "Especialização",
            true
        ) || knowledge.specializationParentId.isNotBlank()
        if (isSpecialization) {
            if (knowledge.specializationParentId.isBlank()) {
                add(DomainError.ValidationError("Especialização requer parentKnowledgeId obrigatório."))
            } else if (allKnowledges.isNotEmpty() && allKnowledges.none { it.id == knowledge.specializationParentId }) {
                add(DomainError.InvalidReference("parentKnowledgeId não encontrado entre os conhecimentos da ficha."))
            }
        }
    }
}

data class PublishedCatalogEntry<T>(
    val reference: CatalogReference<*>,
    val value: T,
    val publishedAt: Long
)

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

fun <T> compareAndAdvanceRevision(
    current: PublishedCatalogEntry<T>,
    write: RevisionedWrite<T>
): PublishedCatalogEntry<T> {
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
        val duplicatedGrant = choice.grantedEntityIds.toSet()
            .intersect(choices.flatMap { it.grantedEntityIds }.toSet())
        if (duplicatedGrant.isNotEmpty()) {
            throw DomainError.DuplicateIdentity("Uma escolha não pode conceder a mesma entidade duas vezes.")
        }
        return copy(choices = choices + choice)
    }
}

enum class MigrationState { CURRENT, NEEDS_REVIEW }

data class NeedsReview(val field: String, val legacyValue: String, val reason: String)

data class MigrationResult<T>(
    val value: T,
    val state: MigrationState,
    val reviews: List<NeedsReview>
)

/** Applies a typed correction and removes its review marker only after the correction succeeds. */
fun Character.resolveMigrationReview(
    field: String,
    correction: (Character) -> Character,
): Character {
    require(migrationReviews.any { it.field == field }) { "Pendência de migração inexistente: $field." }
    val corrected = correction(this)
    require(corrected.canonicalSchemaVersion == CANONICAL_SCHEMA_VERSION) {
        "A correção deve produzir o schema canônico atual."
    }
    return corrected.copy(migrationReviews = corrected.migrationReviews.filterNot { it.field == field })
}

object CanonicalMigration {
    fun migrateMechanicalText(
        description: String,
        mechanicalText: String
    ): MigrationResult<String> {
        if (mechanicalText.isBlank()) return MigrationResult(
            description,
            MigrationState.CURRENT,
            emptyList()
        )
        return MigrationResult(
            value = description,
            state = MigrationState.NEEDS_REVIEW,
            reviews = listOf(
                NeedsReview(
                    "mechanicalEffect",
                    mechanicalText,
                    "Efeito textual requer operação estruturada."
                )
            ),
        )
    }
}

@Serializable
data class StackState(val quantity: Int = 1, val groupingKey: String? = null) {
    init {
        require(quantity >= 0)
    }
}

@Serializable
data class ConsumableState(val doses: Int = 0, val charges: Int? = null) {
    init {
        require(doses >= 0 && (charges == null || charges >= 0))
    }
}

@Serializable
data class AmmoState(val ammoTypeId: AmmoTypeId, val loaded: Int = 0, val capacity: Int = 0) {
    init {
        require(loaded >= 0 && capacity >= 0 && loaded <= capacity)
    }
}

@Serializable
enum class ReloadPhase { Loaded, Empty, Reloading }

@Serializable
data class ReloadState(val state: ReloadPhase = ReloadPhase.Empty, val completesAt: Long? = null) {
    init {
        require(state == ReloadPhase.Reloading || completesAt == null)
    }
}

@Serializable
data class ContainerState(
    val containedItemIds: List<ItemInstanceId> = emptyList(),
)

@Serializable
data class ComponentInstallation(
    val id: InstallationId,
    val component: CatalogReference<ItemCatalogId>,
    val installedAt: Long
)

@Serializable
data class ItemState(
    val id: ItemInstanceId,
    val definition: CatalogReference<ItemCatalogId>,
    val scope: CatalogScope,
    val material: ComponentInstallation? = null,
    val installations: List<ComponentInstallation> = emptyList(),
    val stack: StackState = StackState(),
    val consumable: ConsumableState? = null,
    val ammo: AmmoState? = null,
    val reload: ReloadState? = null,
    val container: ContainerState? = null,
)

/** Character/campaign-owned catalog entry. The reference is immutable once published. */
@Serializable
data class ScopedItemDefinition(
    val reference: CatalogReference<ItemCatalogId>,
    val scope: CatalogScope,
    val ownerScopeId: String,
    val name: String,
    val description: String = "",
    val mechanicalEffect: MechanicalEffect? = null,
)

data class AshConsumptionResult(val ability: Ability, val item: ItemState)

fun consumeAshAtomically(ability: Ability, item: ItemState, doses: Int): AshConsumptionResult {
    require(ability.kind == AbilityKind.ASH) { "A habilidade não é uma Cinza." }
    require(doses > 0) { "Doses devem ser > 0." }
    val ash = ability.ashData
        ?: throw DomainError.InvalidReference("A Cinza não possui vínculo de inventário.")
    if (ash.linkedItemId != item.id) {
        throw DomainError.InvalidReference("O item informado não é o linkedItemId da Cinza.")
    }
    val state = item.consumable
        ?: throw DomainError.InvalidReference("A Cinza não referencia um item consumível.")
    if (state.doses < doses) throw DomainError.InsufficientResource("Doses de Cinza insuficientes.")
    return AshConsumptionResult(
        ability,
        item.copy(consumable = state.copy(doses = state.doses - doses))
    )
}

fun consumeAshAtomically(ability: Ability, item: ItemState): AshConsumptionResult {
    val cost = ability.cost as? AbilityCost.AshCost
        ?: throw DomainError.InvalidReference("A habilidade não possui AshCost.")
    return consumeAshAtomically(ability, item, cost.amount)
}

data class SpecializationKnowledge(
    val id: SpecialKnowledgeId,
    val name: String,
    val parentKnowledgeId: SpecialKnowledgeId,
    val value: Int = 0,
    val category: KnowledgeCategory = KnowledgeCategory.Adquirido,
    val mechanicalEffect: MechanicalEffect? = null,
) {
    init {
        require(parentKnowledgeId.value.isNotBlank()) {
            "parentKnowledgeId é obrigatório em especializações."
        }
    }
}

data class StructuredTrait(
    val id: EntityId = EntityId(UUID.randomUUID().toString()),
    val name: String,
    val isPositive: Boolean = true,
    val description: String = "",
    val mechanicalEffect: MechanicalEffect? = null,
) {
    init {
        require(name.isNotBlank()) { "Nome do traço é obrigatório." }
    }
}

data class StructuredOccupation(
    val id: BackgroundId = BackgroundId("occupation-default"),
    val name: String,
    val description: String = "",
    val mechanicalEffect: MechanicalEffect? = null,
) {
    init {
        require(name.isNotBlank()) { "Nome da ocupação é obrigatório." }
    }
}

data class StructuredProfession(
    val id: ProfessionId = ProfessionId("profession-default"),
    val name: String,
    val description: String = "",
    val mechanicalEffect: MechanicalEffect? = null,
) {
    init {
        require(name.isNotBlank()) { "Nome da profissão é obrigatório." }
    }
}

data class StructuredFaction(
    val id: EntityId = EntityId(UUID.randomUUID().toString()),
    val name: String,
    val standing: String = "",
    val description: String = "",
    val mechanicalEffect: MechanicalEffect? = null,
) {
    init {
        require(name.isNotBlank()) { "Nome da facção é obrigatório." }
    }
}

data class StructuredBond(
    val id: EntityId = EntityId(UUID.randomUUID().toString()),
    val name: String,
    val target: String = "",
    val bondType: String = "",
    val description: String = "",
    val mechanicalEffect: MechanicalEffect? = null,
) {
    init {
        require(name.isNotBlank()) { "Nome do vínculo é obrigatório." }
    }
}

fun Power.toCanonicalAbility(): Ability {
    fun required(value: String, field: String): String = value.takeIf(String::isNotBlank)
        ?: throw DomainError.InvalidReference("$field ausente; o legado exige NeedsReview.")

    val executionKind = when (executionType) {
        AbilityExecution.ACTION -> ExecutionKind.Action
        AbilityExecution.TURN -> ExecutionKind.Turn
        AbilityExecution.FREE -> ExecutionKind.Free
        AbilityExecution.REACTION -> ExecutionKind.Reaction
        AbilityExecution.PASSIVE -> ExecutionKind.Passive
        AbilityExecution.TIME -> ExecutionKind.Timed
    }
    val timedExec = if (executionKind == ExecutionKind.Timed && timeValue > 0) {
        val unit = when (timeUnit) {
            AbilityTimeUnit.HOURS -> ExecutionTimeUnit.Hours
            AbilityTimeUnit.DAYS -> ExecutionTimeUnit.Days
            AbilityTimeUnit.MINUTES -> ExecutionTimeUnit.Minutes
        }
        TimedExecution(timeValue, unit)
    } else null

    val rangeBand = when (rangeType) {
        AbilityRange.PERSONAL -> RangeBand.Personal
        AbilityRange.SHORT -> RangeBand.Short
        AbilityRange.MEDIUM -> RangeBand.Medium
        AbilityRange.LONG -> RangeBand.Long
        AbilityRange.INDEFINITE -> RangeBand.Undefined
    }

    val durKind = when (durationType) {
        AbilityDuration.INSTANT -> DurationKind.Instant
        AbilityDuration.TURNS -> DurationKind.Turns
        AbilityDuration.SCENE -> DurationKind.Scene
        AbilityDuration.SESSION -> DurationKind.Session
        AbilityDuration.TIME -> DurationKind.Timed
    }
    val canonicalDuration = if (executionKind == ExecutionKind.Passive) null else Duration(
        kind = durKind,
        turns = durationValue.takeIf { durKind == DurationKind.Turns && it > 0 }
            ?: (if (durKind == DurationKind.Turns) 1 else null),
        timed = if (durKind == DurationKind.Timed) {
            val unit = when (durationUnit) {
                AbilityTimeUnit.DAYS -> DurationTimeUnit.Days
                else -> DurationTimeUnit.Hours
            }
            TimedDuration(maxOf(1, durationValue), unit)
        } else null,
    )

    val abilityCost = AbilityCost.PowerCost(
        amount = if (executionKind == ExecutionKind.Passive) 0 else costValue.coerceAtLeast(0),
        resource = when (costType) {
            AbilityCostType.ENERGY -> SpendableResource.PE
            AbilityCostType.DESTINY -> SpendableResource.PD
            AbilityCostType.LIFE -> SpendableResource.PV
            AbilityCostType.SANITY -> SpendableResource.PS
            else -> SpendableResource.PE
        },
        isDivineOrLuckBased = destinyCostEligible || costType == AbilityCostType.DESTINY,
    )

    val source = when (canonicalSource) {
        AbilitySource.KNOWLEDGE -> Source.knowledge(
            KnowledgeSourceTarget.Special(SpecialKnowledgeId(required(knowledgeId, "knowledgeId"))),
            knowledgeLevel
                ?: throw DomainError.InvalidReference("knowledgeLevel ausente; o legado exige NeedsReview."),
        )

        AbilitySource.RACE -> Source.race(
            RaceId.entries.firstOrNull { it.name == sourceId }
                ?: throw DomainError.InvalidReference("raceId ausente ou inválido; o legado exige NeedsReview."),
        )

        AbilitySource.PATH -> Source.path(PathTemplateId(required(sourceId, "pathId")))
        AbilitySource.ITEM -> Source.item(ItemInstanceId(required(linkedItemId, "itemInstanceId")))
        AbilitySource.HISTORY -> Source.background(BackgroundId(required(sourceId, "backgroundId")))
        AbilitySource.PROFESSION -> Source.profession(
            ProfessionId(
                required(
                    sourceId,
                    "professionId"
                )
            )
        )

        AbilitySource.NARRATIVE -> Source.narrative(
            NarrativeSourceId(
                required(
                    sourceId,
                    "narrativeSourceId"
                )
            )
        )
    }

    val permanentBonuses = modifiers.map { mod ->
        val bonusTarget: BonusTarget = when (mod.targetType) {
            AbilityModifierTarget.ATTRIBUTE -> BonusTarget.AttributeTarget(Attribute.entries.firstOrNull {
                it.name.equals(
                    mod.targetId,
                    true
                )
            } ?: Attribute.FOR)

            AbilityModifierTarget.KNOWLEDGE -> BonusTarget.BasicKnowledgeTarget(BasicKnowledge.entries.firstOrNull {
                it.name.equals(
                    mod.targetId,
                    true
                )
            } ?: BasicKnowledge.Atletismo)

            AbilityModifierTarget.RESOURCE_MAXIMUM -> BonusTarget.ResourceMaxTarget(
                SpendableResource.entries.firstOrNull { it.name.equals(mod.targetId, true) }
                    ?: SpendableResource.PV)

            AbilityModifierTarget.PROTECTION -> BonusTarget.ProtectionTarget(Protection.entries.firstOrNull {
                it.name.equals(
                    mod.targetId,
                    true
                )
            } ?: Protection.Geral)
        }
        PermanentBonus(bonusTarget, mod.value)
    }

    return Ability(
        id = id,
        name = name,
        kind = AbilityKind.POWER,
        source = source,
        execution = Execution(executionKind, timedExec),
        range = RangeSpec(rangeBand, targetArea.takeIf(String::isNotBlank)),
        duration = canonicalDuration,
        resistance = Resistance.fromLegacy(resistance.name),
        cost = abilityCost,
        effect = effect,
        definition = catalogEntryId.takeIf(String::isNotBlank)
            ?.let { CatalogReference(AbilityCatalogId(it), maxOf(1, catalogVersion)) },
        powerData = PowerData(
            grantsPermanentBonus = grantsPermanentBonus,
            permanentBonuses = permanentBonuses,
            passiveState = if (canonicalSource == AbilitySource.ITEM) PassiveState.ItemBound else PassiveState.ManualToggle(
                active
            ),
        ),
        favorite = favorite,
        available = available,
        revision = maxOf(1, revision),
    )
}

fun MysticAbility.toCanonicalAbility(): Ability {
    fun required(value: String, field: String): String = value.takeIf(String::isNotBlank)
        ?: throw DomainError.InvalidReference("$field ausente; o legado exige NeedsReview.")

    val kind = when {
        type.equals("Magia", true) -> AbilityKind.SPELL
        type.equals("Runa", true) -> AbilityKind.RUNE
        type.equals("Cinza", true) -> AbilityKind.ASH
        else -> AbilityKind.SPELL
    }
    val executionKind = when (executionType) {
        AbilityExecution.ACTION -> ExecutionKind.Action
        AbilityExecution.TURN -> ExecutionKind.Turn
        AbilityExecution.FREE -> ExecutionKind.Free
        AbilityExecution.REACTION -> ExecutionKind.Reaction
        AbilityExecution.PASSIVE -> ExecutionKind.Passive
        AbilityExecution.TIME -> ExecutionKind.Timed
    }
    val timedExec = if (executionKind == ExecutionKind.Timed && timeValue > 0) {
        val unit = when (timeUnit) {
            AbilityTimeUnit.HOURS -> ExecutionTimeUnit.Hours
            AbilityTimeUnit.DAYS -> ExecutionTimeUnit.Days
            AbilityTimeUnit.MINUTES -> ExecutionTimeUnit.Minutes
        }
        TimedExecution(timeValue, unit)
    } else null

    val rangeBand = when (rangeType) {
        AbilityRange.PERSONAL -> RangeBand.Personal
        AbilityRange.SHORT -> RangeBand.Short
        AbilityRange.MEDIUM -> RangeBand.Medium
        AbilityRange.LONG -> RangeBand.Long
        AbilityRange.INDEFINITE -> RangeBand.Undefined
    }

    val durKind = when (durationType) {
        AbilityDuration.INSTANT -> DurationKind.Instant
        AbilityDuration.TURNS -> DurationKind.Turns
        AbilityDuration.SCENE -> DurationKind.Scene
        AbilityDuration.SESSION -> DurationKind.Session
        AbilityDuration.TIME -> DurationKind.Timed
    }
    val canonicalDuration = if (executionKind == ExecutionKind.Passive) null else Duration(
        kind = durKind,
        turns = durationValue.takeIf { durKind == DurationKind.Turns && it > 0 }
            ?: (if (durKind == DurationKind.Turns) 1 else null),
        timed = if (durKind == DurationKind.Timed) {
            val unit = when (durationUnit) {
                AbilityTimeUnit.DAYS -> DurationTimeUnit.Days
                else -> DurationTimeUnit.Hours
            }
            TimedDuration(maxOf(1, durationValue), unit)
        } else null,
    )

    val cost: AbilityCost = when (kind) {
        AbilityKind.SPELL -> AbilityCost.SpellCost(costValue.coerceAtLeast(0))
        AbilityKind.RUNE -> AbilityCost.RuneCost(costValue.coerceAtLeast(0))
        AbilityKind.ASH -> AbilityCost.AshCost(maxOf(1, costValue))
        AbilityKind.POWER -> AbilityCost.PowerCost(
            costValue.coerceAtLeast(0),
            SpendableResource.PE,
            false
        )
    }

    val source = if (kind == AbilityKind.ASH) null else when (canonicalSource) {
        AbilitySource.KNOWLEDGE -> Source.knowledge(
            KnowledgeSourceTarget.Special(SpecialKnowledgeId(required(knowledgeId, "knowledgeId"))),
            knowledgeLevel
                ?: throw DomainError.InvalidReference("knowledgeLevel ausente; o legado exige NeedsReview."),
        )

        AbilitySource.RACE -> Source.race(
            RaceId.entries.firstOrNull { it.name == source }
                ?: throw DomainError.InvalidReference("raceId ausente ou inválido; o legado exige NeedsReview."),
        )

        AbilitySource.PATH -> Source.path(PathTemplateId(required(source, "pathId")))
        AbilitySource.ITEM -> Source.item(
            ItemInstanceId(
                required(
                    linkedInventoryItemId,
                    "itemInstanceId"
                )
            )
        )

        AbilitySource.HISTORY -> Source.background(BackgroundId(required(source, "backgroundId")))
        AbilitySource.PROFESSION -> Source.profession(
            ProfessionId(
                required(
                    source,
                    "professionId"
                )
            )
        )

        AbilitySource.NARRATIVE -> Source.narrative(
            NarrativeSourceId(
                required(
                    source,
                    "narrativeSourceId"
                )
            )
        )
    }

    val ashOrigin = AshOrigin.fromName(ashSource.label)

    return Ability(
        id = id,
        name = name,
        kind = kind,
        source = source,
        execution = Execution(executionKind, timedExec),
        range = RangeSpec(rangeBand, targetArea.takeIf(String::isNotBlank)),
        duration = canonicalDuration,
        resistance = Resistance.fromLegacy(resistance.name),
        cost = cost,
        effect = effect,
        definition = catalogEntryId.takeIf(String::isNotBlank)
            ?.let { CatalogReference(AbilityCatalogId(it), maxOf(1, catalogVersion)) },
        spellData = if (kind == AbilityKind.SPELL) SpellData(level = maxOf(1, costValue)) else null,
        runeData = if (kind == AbilityKind.RUNE) RuneData(
            level = costValue.coerceIn(1, 3),
            inscriberId = inscriberId.takeIf(String::isNotBlank)?.let { CharacterId(it) },
            inscriberPower = inscriberPower.takeIf { it > 0 },
            inscriberRunicKnowledge = inscriberRunicKnowledge.takeIf { it > 0 },
        ) else null,
        ashData = if (kind == AbilityKind.ASH) AshData(
            origin = ashOrigin,
            purity = ashPurity,
            linkedItemId = ItemInstanceId(linkedInventoryItemId.ifBlank { id }),
        ) else null,
        favorite = favorite,
        available = available,
        revision = maxOf(1, revision),
    )
}

fun Character.allCanonicalAbilities(): List<Ability> =
    abilities.ifEmpty { powers.map { it.toCanonicalAbility() } + mysticAbilities.map { it.toCanonicalAbility() } }

fun Character.allCanonicalAbilitiesSafely(): List<Ability> =
    buildList {
        addAll(abilities)
        val knownIds = abilities.mapTo(mutableSetOf(), Ability::id)
        addAll(powers.mapNotNull { runCatching { it.toCanonicalAbility() }.getOrNull() }.filter { knownIds.add(it.id) })
        addAll(mysticAbilities.mapNotNull { runCatching { it.toCanonicalAbility() }.getOrNull() }.filter { knownIds.add(it.id) })
    }

fun Character.withRemovedCanonicalAbility(abilityId: String): Character {
    val ability = allCanonicalAbilitiesSafely().firstOrNull { it.id == abilityId } ?: return this
    require(ability.source?.kind != SourceKind.Race) { "Poderes raciais são vinculados à raça e não podem ser removidos." }
    return copy(
        abilities = allCanonicalAbilitiesSafely().filterNot { it.id == abilityId },
        powers = powers.filterNot { it.id == abilityId },
        mysticAbilities = mysticAbilities.filterNot { it.id == abilityId },
        inventory = inventory.filterNot { it.linkedAshId == abilityId },
    )
}

fun BodyRegion.toCanonicalState(): BodyRegionState {
    val slot = BodyRegionSlot.fromName(name)
        ?: throw DomainError.InvalidReference("Região corporal '$name' exige NeedsReview.")
    return BodyRegionState(
        region = slot,
        state = state,
        failures = failures.coerceIn(0, 4),
        protection = EquippedProtection(slot, generalProtection, localProtection),
        implantInstanceIds = implantInstanceIds.map(::ItemInstanceId),
        prosthesisInstanceId = prosthesisInstanceId.takeIf(String::isNotBlank)
            ?.let(::ItemInstanceId),
        equippedItemIds = equippedItemIds.map { ItemInstanceId(it) },
    )
}

fun BodyRegionState.toLegacyRegion(): BodyRegion = BodyRegion(
    roll = region.roll,
    name = region.label,
    failures = failures,
    localProtection = protection.localProtection,
    generalProtection = protection.generalProtectionBonus,
    equippedItemIds = equippedItemIds.map { it.value },
    state = state,
    implantInstanceIds = implantInstanceIds.map { it.value },
    prosthesisInstanceId = prosthesisInstanceId?.value.orEmpty(),
)

fun OrganStatus.toCanonicalState(): OrganState {
    return OrganState(
        organ = slot,
        state = state,
        failures = failures.coerceIn(0, 3),
        implantInstanceId = implantInstanceId.takeIf(String::isNotBlank)?.let(::ItemInstanceId),
        customName = name,
    )
}

fun OrganState.toLegacyStatus(): OrganStatus = OrganStatus(
    name = name,
    failures = failures,
    slot = organ,
    state = state,
    implantInstanceId = implantInstanceId?.value.orEmpty(),
)

fun ConditionEffect.toCanonicalInstance(): ConditionInstance {
    val kind = ConditionKind.fromName(name)
        ?: throw DomainError.InvalidReference("ConditionKind '$name' exige NeedsReview.")
    val intVal = intensity.toIntOrNull()
    val parsedDuration = when {
        duration.isBlank() -> null
        duration.toIntOrNull() != null -> Duration(DurationKind.Turns, turns = duration.toInt())
        duration.startsWith("Timed:", true) -> {
            val fields = duration.split(':')
            val amount = fields.getOrNull(1)?.toIntOrNull()
                ?: throw DomainError.InvalidReference("Duração '$duration' exige NeedsReview.")
            val unit = fields.getOrNull(2)?.let { value ->
                DurationTimeUnit.entries.firstOrNull {
                    it.name.equals(
                        value,
                        true
                    )
                }
            }
                ?: throw DomainError.InvalidReference("Unidade de duração '$duration' exige NeedsReview.")
            Duration(DurationKind.Timed, timed = TimedDuration(amount, unit))
        }

        else -> when (val kindValue =
            DurationKind.entries.firstOrNull { it.name.equals(duration, true) }) {
            DurationKind.Instant, DurationKind.Scene, DurationKind.Session -> Duration(
                requireNotNull(kindValue)
            )

            else -> throw DomainError.InvalidReference("Duração '$duration' exige NeedsReview.")
        }
    }
    return ConditionInstance(
        instanceId = ConditionInstanceId(id),
        payload = ConditionPayload(
            kind = kind,
            intensity = intVal?.takeIf { it > 0 },
            duration = parsedDuration,
        ),
        source = Source.narrative(
            NarrativeSourceId(
                origin.takeIf(String::isNotBlank)
                    ?: throw DomainError.InvalidReference("SourceRef da condição exige NeedsReview.")
            )
        ),
    )
}

fun ConditionInstance.toLegacyEffect(): ConditionEffect = ConditionEffect(
    id = instanceId.value,
    name = name,
    intensity = intensity?.toString().orEmpty(),
    duration = when (val value = duration) {
        null -> ""
        else -> when (value.kind) {
            DurationKind.Turns -> value.turns.toString()
            DurationKind.Timed -> "Timed:${value.timed!!.amount}:${value.timed.unit.name}"
            else -> value.kind.name
        }
    },
    origin = source.sourceRef.persistenceId(),
    summary = payload.endsWhen.joinToString(", ") { it.displayText() },
)

private fun SourceRef.persistenceId(): String = when (this) {
    is SourceRef.Knowledge -> when (val value = target) {
        is KnowledgeSourceTarget.Basic -> value.basic.name
        is KnowledgeSourceTarget.Special -> value.specialId.value
    }

    is SourceRef.Race -> raceId.name
    is SourceRef.Path -> pathId.value
    is SourceRef.Item -> itemInstanceId.value
    is SourceRef.Background -> backgroundId.value
    is SourceRef.Profession -> professionId.value
    is SourceRef.Narrative -> narrativeSourceId.value
}

fun Character.canonicalBodyState(): BodyState = bodyState ?: BodyState(
    regions = bodyRegions.map { it.toCanonicalState() },
    organs = organs.map { it.toCanonicalState() },
)

fun Character.withCanonicalBodyState(bodyState: BodyState): Character = copy(
    bodyState = bodyState,
    bodyRegions = bodyState.regions.map { it.toLegacyRegion() },
    organs = bodyState.organs.map { it.toLegacyStatus() },
)

/** Associates an implant with an organ. Only an implant whose own text declares that it
 * restores or replaces organ function clears failures; unlinking and relabelling never do. */
fun Character.withOrganImplant(organIndex: Int, implantId: String?): Character {
    val body = canonicalBodyState()
    val implant = implantId?.let { id -> inventory.firstOrNull { it.id == id } }
    val declaresRestoration = implant?.let {
        val text = listOf(it.name, it.category, it.effect, it.mechanicalEffects.joinToString(" ") { effect -> effect.description }).joinToString(" ")
        val function = text.contains(Regex("(?i)\\b(órgão|orgao|função|funcao)\\b"))
        val restoration = text.contains(Regex("(?i)\\b(restaura|restaurar|restaurador|substitui|substituir|substitutiv[oa])\\b"))
        function && restoration && !text.contains(Regex("(?i)não (remove|zera).{0,24}falhas"))
    } == true
    return withCanonicalBodyState(body.copy(organs = body.organs.mapIndexed { index, organ ->
        if (index != organIndex) organ else organ.copy(
            implantInstanceId = implantId?.let(::ItemInstanceId),
            failures = if (declaresRestoration && organ.implantInstanceId?.value != implantId) 0 else organ.failures,
        )
    }))
}

fun Character.canonicalConditions(): List<ConditionInstance> =
    conditionInstances.ifEmpty { conditions.map { it.toCanonicalInstance() } }

fun Character.withCanonicalConditions(instances: List<ConditionInstance>): Character = copy(
    conditionInstances = instances,
    conditions = instances.map { it.toLegacyEffect() },
)
