package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CatalogKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInCatalogTest {
    @Test fun containsAllPublishedExamplesAndPaths() {
        assertEquals(100, BuiltInCatalog.entries.count { it.kind == CatalogKind.POWER })
        assertEquals(50, BuiltInCatalog.entries.count { it.kind == CatalogKind.POWER && it.source == "50 Exemplos de Poderes Mágicos" })
        assertEquals(50, BuiltInCatalog.entries.count { it.kind == CatalogKind.POWER && it.source == "50 Exemplos de Poderes de Profissão e Conhecimento" })
        assertEquals(50, BuiltInCatalog.entries.count { it.kind == CatalogKind.MAGIC })
        assertEquals(50, BuiltInCatalog.entries.count { it.kind == CatalogKind.ASH })
        assertEquals(50, BuiltInCatalog.entries.count { it.kind == CatalogKind.RUNE })
        assertTrue(BuiltInCatalog.entries.count { it.kind == CatalogKind.PATH } >= 18)
        assertTrue(BuiltInCatalog.entries.count { it.kind == CatalogKind.ITEM } >= 60)
    }

    @Test fun idsAreStableAndUnique() {
        val ids = BuiltInCatalog.entries.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        assertTrue(ids.all { it.matches(Regex("[a-z]+\\.[a-z0-9_]+")) })
    }
}
