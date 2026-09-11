package com.kinderman.sdo.domain.creation

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.SpecialKnowledge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CharacterCreationTest {
    @Test fun `five special knowledges start free and fifteen points may be split across any knowledge`() {
        val specials = (1..5).map { SpecialKnowledge(name = "Especial $it", value = 1) }
        val base = Character(learnedKnowledges = specials)
        assertEquals(0, CharacterCreation.knowledgePointsSpent(base))

        val withBasicPoints = base.copy(attributes = base.attributes.mapIndexed { index, attribute ->
            if (index == 0) attribute.copy(skills = attribute.skills.mapIndexed { skillIndex, skill ->
                if (skillIndex == 0) skill.copy(value = 5) else skill
            }) else attribute
        })
        val distributed = withBasicPoints.copy(learnedKnowledges = withBasicPoints.learnedKnowledges.mapIndexed { index, knowledge ->
            if (index < 2) knowledge.copy(value = 5) else if (index == 2) knowledge.copy(value = 3) else knowledge
        })
        assertEquals(15, CharacterCreation.knowledgePointsSpent(distributed))
        assertNull(CharacterCreation.stepError(4, distributed))
    }

    @Test fun `creation requires exactly five special knowledges`() {
        assertNotNull(CharacterCreation.stepError(3, Character(learnedKnowledges = List(4) { SpecialKnowledge(value = 1) })))
        assertNull(CharacterCreation.stepError(3, Character(learnedKnowledges = List(5) { SpecialKnowledge(value = 1) })))
    }
}
