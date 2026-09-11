package com.kinderman.sdo.domain.creation

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemAcquisitionSource
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.PowerSourceType
import com.kinderman.sdo.domain.model.CharacterCreationStatus
import com.kinderman.sdo.domain.model.KnowledgeMilestoneReward
import com.kinderman.sdo.domain.model.KnowledgeMilestoneRewardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CharacterCreationTest {
    @Test fun `racial attribute bonus does not spend one of the ten creation points`() {
        val character = Character(raceAttribute = "CAR", attributes = Character().attributes.map {
            it.copy(value = if (it.acronym == "CAR") 2 else if (it.acronym == "FOR") 8 else 0)
        })
        assertEquals(10, CharacterCreation.attributePointsSpent(character))
        assertEquals(3, character.permanentAttributeValue("CAR"))
        assertNull(CharacterCreation.stepError(2, character))
    }

    @Test fun `heritage budget must be spent completely`() {
        assertNotNull(CharacterCreation.stepError(7, Character()))
    }

    @Test fun `valid review can finish the character`() {
        val base = Character()
        val milestone = { index: Int -> KnowledgeMilestoneReward(3, KnowledgeMilestoneRewardType.POWER, "reward-$index", "power-$index") }
        val character = base.copy(
            name = "Iria",
            race = "Humanos",
            attributes = base.attributes.map { attribute -> attribute.copy(value = if (attribute.acronym in setOf("FOR", "INT")) 5 else 0) },
            learnedKnowledges = List(5) { index -> SpecialKnowledge(
                name = "K$index", attribute = "INT", value = 3,
                milestoneLevels = listOf(3), milestoneRewards = listOf(milestone(index)),
            ) },
            pathName = "Caminho",
            pathPillars = listOf("Um", "Dois", "Três"),
            powers = listOf(Power(sourceType = PowerSourceType.PATH), Power(sourceType = PowerSourceType.PATH)),
            inventory = listOf(InventoryItem(acquisitionSource = ItemAcquisitionSource.HERITAGE, heritageCost = 30)),
            creationStep = CharacterCreation.STEP_COUNT,
        )

        assertEquals(CharacterCreationStatus.COMPLETED, CharacterCreation.finish(character).creationStatus)
    }

    @Test fun `five special knowledges start free and fifteen points may be split across any knowledge`() {
        val specials = (1..5).map { SpecialKnowledge(name = "Especial $it", attribute = "INT", value = 0) }
        val base = Character(learnedKnowledges = specials)
        assertEquals(0, CharacterCreation.knowledgePointsSpent(base))

        val withBasicPoints = base.copy(attributes = base.attributes.mapIndexed { index, attribute ->
            if (index < 3) attribute.copy(value = 5, skills = attribute.skills.mapIndexed { skillIndex, skill ->
                if (skillIndex == 0) skill.copy(value = 5) else skill
            }) else attribute
        })
        assertEquals(15, CharacterCreation.knowledgePointsSpent(withBasicPoints))
        assertNull(CharacterCreation.stepError(4, withBasicPoints))
    }

    @Test fun `specialization granted by a milestone does not replace an initial choice or spend its free level`() {
        val initial = List(5) { SpecialKnowledge(name = "Inicial $it", attribute = "INT") }
        val specialization = SpecialKnowledge(name = "Especialização", attribute = "INT", value = 1, specializationParentId = initial.first().id)
        val character = Character(learnedKnowledges = initial + specialization)

        assertEquals(5, CharacterCreation.initialSpecialKnowledges(character).size)
        assertEquals(0, CharacterCreation.knowledgePointsSpent(character))
        assertNull(CharacterCreation.stepError(3, character))
    }

    @Test fun `creation requires exactly five special knowledges`() {
        assertNotNull(CharacterCreation.stepError(3, Character(learnedKnowledges = List(4) { SpecialKnowledge(value = 0) })))
        assertNull(CharacterCreation.stepError(3, Character(learnedKnowledges = List(5) { SpecialKnowledge(name = "K$it", attribute = "INT", value = 0) })))
    }

    @Test fun `creation rejects duplicate or incomplete special knowledges`() {
        val duplicated = Character(learnedKnowledges = List(5) { SpecialKnowledge(name = "Mesmo", attribute = "INT") })
        val codes = CharacterCreation.validateStep(3, duplicated).map { it.code }
        assertEquals(true, CharacterCreationErrorCode.SPECIAL_KNOWLEDGE_DUPLICATE in codes)
    }

    @Test fun `knowledge cannot exceed permanent related attribute`() {
        val character = Character(
            attributes = Character().attributes.map { if (it.acronym == "CAR") it.copy(value = 2) else it },
            raceAttribute = "CAR",
            learnedKnowledges = listOf(SpecialKnowledge(name = "Música", attribute = "CAR", value = 4)),
        )
        assertEquals(CharacterCreationErrorCode.KNOWLEDGE_LIMIT, CharacterCreation.validateStep(4, character).first { it.code == CharacterCreationErrorCode.KNOWLEDGE_LIMIT }.code)
    }

    @Test fun `level three and five require persisted milestone rewards`() {
        val knowledge = SpecialKnowledge(name = "Música", attribute = "CAR", value = 3, milestoneLevels = listOf(3))
        val character = Character(
            attributes = Character().attributes.map { if (it.acronym == "CAR") it.copy(value = 3) else it },
            learnedKnowledges = listOf(knowledge),
        )
        assertEquals(true, CharacterCreation.validateStep(4, character).any { it.code == CharacterCreationErrorCode.MILESTONE_REWARD_MISSING })
    }
}
