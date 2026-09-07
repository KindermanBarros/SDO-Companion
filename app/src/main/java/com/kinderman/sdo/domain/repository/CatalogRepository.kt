package com.kinderman.sdo.domain.repository

import com.kinderman.sdo.domain.model.CatalogEntry
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun observe(): Flow<List<CatalogEntry>>
    suspend fun refreshBundledCatalog()
}

