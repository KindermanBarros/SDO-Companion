package com.kinderman.sdo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CatalogDao {
    @Query("SELECT * FROM catalog_entries ORDER BY kind, groupName, name")
    abstract fun observeAll(): Flow<List<CatalogEntryRecord>>

    @Upsert
    abstract suspend fun upsertAll(entries: List<CatalogEntryRecord>)

    @Query("DELETE FROM catalog_entries")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceAll(entries: List<CatalogEntryRecord>) {
        deleteAll()
        upsertAll(entries)
    }
}
