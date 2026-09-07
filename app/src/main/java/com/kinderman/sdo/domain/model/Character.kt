package com.kinderman.sdo.domain.model

import java.util.UUID

enum class UserRole { PLAYER, MASTER }

enum class CharacterLock { NONE, PLAYER, HISTORIAN }

data class UserSession(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: UserRole,
) {
    val isMaster: Boolean get() = role == UserRole.MASTER
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
)

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
)

data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val state: String = "M",
    val name: String = "",
    val load: Int = 0,
    val durability: String = "",
    val region: String = "",
    val effect: String = "",
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
)

data class ConditionEffect(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val intensity: String = "",
    val duration: String = "",
    val origin: String = "",
)

data class PersonalNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val text: String = "",
)

data class Character(
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String = "",
    val campaignId: String = "default",
    val name: String = "Novo personagem",
    val race: String = "",
    val subRace: String = "",
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
    val organs: List<OrganStatus> = defaultOrgans(),
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
    val maximumLoad: Int get() = 2 + (attributes.firstOrNull { it.acronym == "FOR" }?.value ?: 0) + containerCapacity

    val lifeBase: Int get() = 10 + skillValue("VIG", "Vitalidade")
    val sanityBase: Int get() = 10 + skillValue("INT", "Sanidade")
    val arcaneBase: Int get() = attributeValue("POD") + skillValue("POD", "Arcano")
    val energyBase: Int get() = attributeValue("VIG") + skillValue("VIG", "Energia")

    val lifeMaximum: Int get() = (lifeBase + life.adjustment).coerceAtLeast(0)
    val sanityMaximum: Int get() = (sanityBase + sanity.adjustment).coerceAtLeast(0)
    val arcaneMaximum: Int get() = (arcaneBase + arcane.adjustment).coerceAtLeast(0)
    val energyMaximum: Int get() = (energyBase + energy.adjustment).coerceAtLeast(0)

    fun protectionBase(name: String): Int = when (name) {
        "Geral" -> 10
        "Esquiva" -> protectionTotal("Geral") + attributeValue("AGI") + skillValue("AGI", "Reflexos")
        "Postura" -> 10 + attributeValue("CAR") + skillValue("CAR", "Lábia")
        "Mental" -> 10 + attributeValue("INT") + skillValue("INT", "Sanidade")
        "Arcana" -> 10 + attributeValue("POD") + skillValue("POD", "Arcano")
        else -> 0
    }

    fun protectionTotal(name: String): Int =
        (protectionBase(name) + (protectionAdjustments[name] ?: 0)).coerceAtLeast(0)

    fun calculatedProtections(): Map<String, Int> = defaultProtectionNames.associateWith(::protectionTotal)

    private fun attributeValue(acronym: String): Int =
        attributes.firstOrNull { it.acronym == acronym }?.value ?: 0

    private fun skillValue(attributeAcronym: String, skillName: String): Int =
        attributes
            .firstOrNull { it.acronym == attributeAcronym }
            ?.skills
            ?.firstOrNull { it.name == skillName }
            ?.value
            ?: 0
}

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
    "Cabeça", "Braço esquerdo", "Braço direito", "Torso", "Mão esquerda",
    "Mão direita", "Perna esquerda", "Perna direita", "Pé esquerdo", "Pé direito",
).mapIndexed { index, name -> BodyRegion(roll = index + 1, name = name) }

fun defaultOrgans() = listOf(
    "Cérebro", "Coração ou núcleo", "Pulmões ou sistema respiratório", "Fígado ou filtro", "Outro",
).map { OrganStatus(name = it) }
