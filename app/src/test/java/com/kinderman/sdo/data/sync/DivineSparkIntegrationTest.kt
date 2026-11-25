package com.kinderman.sdo.data.sync

import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterDivineAscension
import com.kinderman.sdo.domain.model.CharacterDivineRuntime
import com.kinderman.sdo.domain.model.DivineMark
import com.kinderman.sdo.domain.model.DivinePillars
import com.kinderman.sdo.domain.model.DivineTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DivineSparkIntegrationTest {
    @Test
    fun divineFieldsRoundTripThroughRoomMappingAndSparkDocuments() {
        val original = Character(
            id = "sheet",
            ownerId = "owner",
            divineAscension = CharacterDivineAscension(
                tier = DivineTier.GREATER_GOD,
                patronDeityId = "deity.patron",
                pillars = DivinePillars(presenca = 4),
            ),
            divineRuntime = CharacterDivineRuntime(
                marks = listOf(DivineMark(id = "mark", deityId = "deity.patron")),
                tabuViolatedInScene = true,
                pdSpentInCurrentScene = 2,
            ),
        )

        val documents = SparkWireCodec.toSparkDocuments(original.toRecord())
        val restored = SparkWireCodec.fromSparkDocuments(documents).toDomain()

        assertTrue("divineAscensionPayload" in documents.fields)
        assertTrue("divineRuntimePayload" in documents.fields)
        assertEquals(original.divineAscension, restored.divineAscension)
        assertEquals(original.divineRuntime, restored.divineRuntime)
    }

    @Test
    fun legacySparkDocumentsWithoutDivineFieldsRestoreMortalDefaults() {
        val documents = SparkWireCodec.toSparkDocuments(Character(id = "legacy").toRecord())
        val legacy = documents.copy(fields = documents.fields - "divineAscensionPayload" - "divineRuntimePayload")

        val restored = SparkWireCodec.fromSparkDocuments(legacy).toDomain()

        assertEquals(CharacterDivineAscension(), restored.divineAscension)
        assertEquals(CharacterDivineRuntime(), restored.divineRuntime)
    }
}
