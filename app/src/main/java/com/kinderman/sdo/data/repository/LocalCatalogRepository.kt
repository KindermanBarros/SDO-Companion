package com.kinderman.sdo.data.repository

import com.kinderman.sdo.data.local.CatalogDao
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.domain.catalog.BuiltInCatalog
import com.kinderman.sdo.domain.catalog.KnowledgeCatalog
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers

class LocalCatalogRepository(private val dao: CatalogDao) : CatalogRepository {
    override fun observe(): Flow<List<CatalogEntry>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged().flowOn(Dispatchers.Default)

    override suspend fun refreshBundledCatalog() {
        val bundled = (BuiltInCatalog.entries + KnowledgeCatalog.entries)
            .distinctBy(CatalogEntry::id)
        dao.replaceAll(bundled.map { it.toRecord() })
    }
}
