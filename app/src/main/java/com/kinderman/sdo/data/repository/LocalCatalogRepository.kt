package com.kinderman.sdo.data.repository

import com.kinderman.sdo.data.local.CatalogDao
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.domain.catalog.BuiltInCatalog
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalCatalogRepository(private val dao: CatalogDao) : CatalogRepository {
    override fun observe(): Flow<List<CatalogEntry>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refreshBundledCatalog() {
        dao.replaceAll(BuiltInCatalog.entries.map { it.toRecord() })
    }
}
