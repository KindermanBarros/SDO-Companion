package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.AttributeValue
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.KnowledgeMilestoneReward
import com.kinderman.sdo.domain.model.KnowledgeMilestoneRewardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeProgressionTest {
    private val knowledgeEntry = CatalogEntry("knowledge.music", CatalogKind.ACQUIRED_KNOWLEDGE, "Música", "Arte", "")
    private val rewardThree = CatalogEntry("power.rhythm", CatalogKind.POWER, "Ritmo", "", "", sourceKnowledge = "Música")
    private val rewardFive = CatalogEntry("power.harmony", CatalogKind.POWER, "Harmonia", "", "", sourceKnowledge = "Música")

    @Test fun levelJumpWaitsForEveryCrossedMilestoneAndResolvesInOrder() {
        val knowledge = SpecialKnowledge(id = "music", name = "Música", catalogEntryId = knowledgeEntry.id, attribute = "CAR", value = 2)
        val character = Character(attributes = listOf(AttributeValue("Carisma", "CAR", 5)), learnedKnowledges = listOf(knowledge))
        val catalog = listOf(knowledgeEntry, rewardThree, rewardFive)

        val pending = character.requestKnowledgeLevel("music", 5, catalog)
        assertEquals(2, pending.learnedKnowledges.single().value)
        assertEquals(listOf(3, 5), pending.learnedKnowledges.single().pendingMilestoneLevels)

        val afterThree = pending.resolveKnowledgeMilestone("music", rewardThree, catalog)
        assertEquals(2, afterThree.learnedKnowledges.single().value)
        assertEquals(listOf(5), afterThree.learnedKnowledges.single().pendingMilestoneLevels)

        val completed = afterThree.resolveKnowledgeMilestone("music", rewardFive, catalog)
        assertEquals(5, completed.learnedKnowledges.single().value)
        assertEquals(listOf(3, 5), completed.learnedKnowledges.single().milestoneRewards.map { it.level })
        assertTrue(completed.learnedKnowledges.single().pendingMilestoneLevels.isEmpty())
    }

    @Test fun cancelledTransitionDoesNotGrantOrphanReward() {
        val knowledge = SpecialKnowledge(id = "music", name = "Música", catalogEntryId = knowledgeEntry.id, attribute = "CAR", value = 2)
        val character = Character(attributes = listOf(AttributeValue("Carisma", "CAR", 5)), learnedKnowledges = listOf(knowledge))
        val cancelled = character.requestKnowledgeLevel("music", 3, listOf(knowledgeEntry, rewardThree))
            .cancelKnowledgeLevelRequest("music")

        assertEquals(2, cancelled.learnedKnowledges.single().value)
        assertTrue(cancelled.learnedKnowledges.single().milestoneRewards.isEmpty())
        assertTrue(cancelled.powers.isEmpty())
    }

    @Test fun milestoneKindsAreRestrictedByKnowledgeCategory() {
        val acquired = SpecialKnowledge(id = "acquired", name = "Música", catalogEntryId = knowledgeEntry.id)
        val arcaneEntry = CatalogEntry("knowledge.arcane", CatalogKind.ARCANE_KNOWLEDGE, "Música", "Arcano", "")
        val magic = CatalogEntry("magic.music", CatalogKind.MAGIC, "Canção", "", "", sourceKnowledge = "Música")
        val rune = CatalogEntry("rune.music", CatalogKind.RUNE, "Marca", "", "", sourceKnowledge = "Música")
        val catalog = listOf(knowledgeEntry, arcaneEntry, rewardThree, magic, rune)

        assertEquals(setOf(CatalogKind.POWER), eligibleMilestoneRewards(acquired, catalog).map { it.kind }.toSet())
        assertEquals(setOf(CatalogKind.POWER, CatalogKind.MAGIC, CatalogKind.RUNE),
            eligibleMilestoneRewards(acquired.copy(catalogEntryId = arcaneEntry.id), catalog).map { it.kind }.toSet())
    }

    @Test fun milestoneCanCreateAFreeSpecializationInTheSameCategory() {
        val knowledge = SpecialKnowledge(id = "music", name = "Música", catalogEntryId = knowledgeEntry.id, attribute = "CAR", value = 2,
            pendingMilestoneLevels = listOf(3), pendingTargetLevel = 3)
        val result = Character(attributes = listOf(AttributeValue("Carisma", "CAR", 5)), learnedKnowledges = listOf(knowledge))
            .resolveKnowledgeSpecialization("music", "Composição", listOf(knowledgeEntry))
        val specialization = result.learnedKnowledges.single { it.specializationParentId == "music" }
        assertEquals(1, specialization.value)
        assertEquals(KnowledgeMilestoneRewardType.SPECIALIZATION, result.learnedKnowledges.first().milestoneRewards.single().type)
        assertEquals(3, result.learnedKnowledges.first().value)
    }
    @Test fun knowledgeLevelCannotExceedItsPermanentAttribute() {
        val music = SpecialKnowledge(id = "music", name = "Música", attribute = "CAR")
        val character = Character(
            attributes = Character().attributes.map { if (it.acronym == "CAR") it.copy(value = 3) else it },
            learnedKnowledges = listOf(music),
        )
        assertEquals(3, character.withKnowledgeLevel("music", 5, emptyList()).learnedKnowledges.single().value)
    }

    @Test fun runicGrantsEachCanonicalPackageOnceAtItsThreshold() {
        val runic = SpecialKnowledge(id = "runic", name = "Rúnico", attribute = "POD", value = 0)
        val character = Character(
            attributes = Character().attributes.map { if (it.acronym == "POD") it.copy(value = 5) else it },
            arcaneKnowledges = listOf(runic),
        )
        val catalog = BuiltInCatalog.entries.filter { it.kind == CatalogKind.RUNE }

        val levelOne = character.withKnowledgeLevel("runic", 1, catalog)
        assertEquals(setOf("Centelha", "Solo Firme", "Frescor", "Mensagem de Eco"), levelOne.mysticAbilities.map { it.name }.toSet())
        val levelThree = levelOne.withKnowledgeLevel("runic", 3, catalog)
        assertEquals(8, levelThree.mysticAbilities.size)
        val levelFive = levelThree.withKnowledgeLevel("runic", 5, catalog)
        assertEquals(12, levelFive.mysticAbilities.size)
        assertEquals(12, levelFive.withKnowledgeLevel("runic", 5, catalog).mysticAbilities.size)
        assertEquals(listOf(1, 3, 5), levelFive.arcaneKnowledges.single().milestoneLevels)
    }

    @Test fun creationAllocationIsEphemeralAndRequiresAllCanonicalChoices() {
        val choices = List(5) { SpecialKnowledge(name = "K$it", attribute = "INT", value = 0) }
        InitialKnowledgeAllocation(choices, 15).validate()
        assertTrue(runCatching { InitialKnowledgeAllocation(choices.take(4), 15).validate() }.isFailure)
        assertTrue(runCatching { InitialKnowledgeAllocation(choices, 14).validate() }.isFailure)
    }

    @Test fun milestoneRewardIsRecordedOnceWithItsGrantedEntity() {
        val knowledge = SpecialKnowledge(id = "music", name = "Música", attribute = "CAR", value = 3)
        val character = Character(learnedKnowledges = listOf(knowledge))
        val reward = KnowledgeMilestoneReward(3, KnowledgeMilestoneRewardType.POWER, "power.rhythm", "instance-1")

        val updated = character.withKnowledgeMilestoneReward("music", reward)

        assertEquals(reward, updated.learnedKnowledges.single().milestoneRewards.single())
        assertTrue(runCatching { updated.withKnowledgeMilestoneReward("music", reward.copy(grantedEntityId = "instance-2")) }.isFailure)
    }

    @Test fun runeCatalogNeverRequestsAshDoses() {
        val runes = BuiltInCatalog.entries.filter { it.kind == CatalogKind.RUNE }
        assertFalse(runes.any { it.summary.contains("dose", true) || it.mechanicalEffect.contains("dose", true) })
        assertTrue(runes.all { it.toMysticAbility().costType.name == "ARCANE" })
    }
}
