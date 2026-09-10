package com.kinderman.sdo.data.local

import com.kinderman.sdo.domain.catalog.BuiltInCatalog
import com.kinderman.sdo.domain.model.CatalogKind
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogEntryRecordTest {
    @Test fun structuredAbilityFieldsSurviveRoomMapping() {
        val entry = BuiltInCatalog.entries.first { it.kind == CatalogKind.ASH }

        assertEquals(entry, entry.toRecord().toDomain())
    }
}
