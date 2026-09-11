package com.kinderman.sdo.domain.creation

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemAcquisitionSource
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.PowerSourceType
import com.kinderman.sdo.domain.model.CharacterCreationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CharacterCreationTest {
    @Test fun `racial attribute bonus does not spend one of the ten creation points`() {
        val character = Character(raceAttribute = "CAR", attributes = Character().attributes.map {
            it.copy(value = if (it.acronym == "CAR") 3 else if (it.acronym == "FOR") 8 else 0)
        })
        assertEquals(10, CharacterCreation.attributePointsSpent(character))
        assertNull(CharacterCreation.stepError(2, character))
    }

    @Test fun `heritage budget must be spent completely`() {
        assertNotNull(CharacterCreation.stepError(7, Character()))
    }

    @Test fun `valid review can finish the character`() {
        val base = Character()
        val character = base.copy(
            name = "Iria",
            race = "Humanos",
            attributes = base.attributes.mapIndexed { index, attribute -> attribute.copy(value = if (index == 0) 10 else 0) },
            learnedKnowledges = List(5) { index -> SpecialKnowledge(name = "K$index", value = if (index < 3) 5 else 0) },
            pathName = "Caminho",
            pathPillars = listOf("Um", "Dois", "Três"),
            powers = listOf(Power(sourceType = PowerSourceType.PATH), Power(sourceType = PowerSourceType.PATH)),
            inventory = listOf(InventoryItem(acquisitionSource = ItemAcquisitionSource.HERITAGE, heritageCost = 30)),
            creationStep = CharacterCreation.STEP_COUNT,
        )

        assertEquals(CharacterCreationStatus.COMPLETED, CharacterCreation.finish(character).creationStatus)
    }

    @Test fun `five special knowledges start free and fifteen points may be split across any knowledge`() {
        val specials = (1..5).map { SpecialKnowledge(name = "Especial $it", value = 0) }
        val base = Character(learnedKnowledges = specials)
        assertEquals(0, CharacterCreation.knowledgePointsSpent(base))

        val withBasicPoints = base.copy(attributes = base.attributes.mapIndexed { index, attribute ->
            if (index == 0) attribute.copy(skills = attribute.skills.mapIndexed { skillIndex, skill ->
                if (skillIndex == 0) skill.copy(value = 5) else skill
            }) else attribute
        })
        val distributed = withBasicPoints.copy(learnedKnowledges = withBasicPoints.learnedKnowledges.mapIndexed { index, knowledge ->
            if (index < 2) knowledge.copy(value = 5) else knowledge
        })
        assertEquals(15, CharacterCreation.knowledgePointsSpent(distributed))
        assertNull(CharacterCreation.stepError(4, distributed))
    }

    @Test fun `creation requires exactly five special knowledges`() {
        assertNotNull(CharacterCreation.stepError(3, Character(learnedKnowledges = List(4) { SpecialKnowledge(value = 0) })))
        assertNotNull(CharacterCreation.stepError(3, Character(learnedKnowledges = List(5) { SpecialKnowledge(value = 1) })))
        assertNull(CharacterCreation.stepError(3, Character(learnedKnowledges = List(5) { SpecialKnowledge(value = 0) })))
    }
}
