package com.kinderman.sdo.data.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.firestore
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kinderman.sdo.SdoApplication
import com.kinderman.sdo.data.local.AccountSheetRecord
import com.kinderman.sdo.data.local.AppDatabase
import com.kinderman.sdo.data.local.CampaignMemberRecord
import com.kinderman.sdo.data.local.CampaignRecord
import com.kinderman.sdo.data.local.CharacterConverters
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.SessionOperationRecord
import com.kinderman.sdo.data.local.SheetConflictRecord
import com.kinderman.sdo.data.local.SheetIntentRecord
import com.kinderman.sdo.data.local.SheetOutboxRecord
import com.kinderman.sdo.data.local.SheetRouteRecord
import com.kinderman.sdo.data.local.normalizedForSparkV2
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.data.repository.FirestoreCollections
import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.AccountSheetSnapshot
import com.kinderman.sdo.domain.model.AttributeValue
import com.kinderman.sdo.domain.model.CampaignMemberState
import com.kinderman.sdo.domain.model.CampaignRole
import com.kinderman.sdo.domain.model.CampaignState
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterLock
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.ConditionInstance
import com.kinderman.sdo.domain.model.ConflictResolutionStatus
import com.kinderman.sdo.domain.model.CURRENT_CHARACTER_SCHEMA_VERSION
import com.kinderman.sdo.domain.model.DomainError
import com.kinderman.sdo.domain.model.IdempotencyMatchResult
import com.kinderman.sdo.domain.model.ItemState
import com.kinderman.sdo.domain.model.OperationReceipt
import com.kinderman.sdo.domain.model.OperationType
import com.kinderman.sdo.domain.model.ProgressionRecord
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.SessionResource
import com.kinderman.sdo.domain.model.SheetConflictItem
import com.kinderman.sdo.domain.model.SheetIntent
import com.kinderman.sdo.domain.model.SheetIntentStatus
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.SheetSyncStatus
import com.kinderman.sdo.domain.model.SparkTargetSpec
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.TargetKind
import com.kinderman.sdo.domain.model.TargetOp
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.allCanonicalAbilitiesSafely
import com.kinderman.sdo.domain.model.applyCharacterEditKeys
import com.kinderman.sdo.domain.model.applyPendingIntentsOnConfirmed
import com.kinderman.sdo.domain.model.applySessionCommand
import com.kinderman.sdo.domain.model.buildConflictResolutionIntent
import com.kinderman.sdo.domain.model.characterEditKeys
import com.kinderman.sdo.domain.model.computeCanonicalPayloadHash
import com.kinderman.sdo.domain.model.decodeWireElementId
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.domain.model.resourceMaximum
import com.kinderman.sdo.domain.model.resourceValue
import com.kinderman.sdo.domain.model.toSyncStatus
import com.kinderman.sdo.domain.model.validateAtomicSparkActionBudget
import com.kinderman.sdo.domain.policy.CharacterAccessPolicy
import com.kinderman.sdo.domain.repository.RemoteSyncEvent
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json


class RoomSparkLocalStore(private val db: AppDatabase) : SparkLocalStore {
    private val accountSheetDao = db.accountSheetDao()
    private val intentDao = db.sheetIntentDao()
    private val conflictDao = db.sheetConflictDao()
    private val characterDao = db.characterDao()
    private val campaignDao = db.campaignDao()
    private val operationsDao = db.operationsDao()
    private val legacyDao = db.sheetSyncDao()

    override suspend fun <T> withTransaction(block: suspend () -> T): T = db.withTransaction(block)

    override suspend fun getAccountSheet(accountId: String, sheetId: String): AccountSheetRecord? =
        accountSheetDao.get(accountId, sheetId)

    override suspend fun listAccountSheets(accountId: String): List<AccountSheetRecord> =
        db.openHelper.readableDatabase.query(
            "SELECT accountId, sheetId, sheetRevision, fieldRevisionsJson, confirmedJson, localPhotoUri, updatedAt " +
                "FROM account_sheets WHERE accountId = ?",
            arrayOf(accountId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        AccountSheetRecord(
                            accountId = cursor.getString(0),
                            sheetId = cursor.getString(1),
                            sheetRevision = cursor.getLong(2),
                            fieldRevisionsJson = cursor.getString(3),
                            confirmedJson = cursor.getString(4),
                            localPhotoUri = cursor.getString(5),
                            updatedAt = cursor.getLong(6),
                        ),
                    )
                }
            }
        }

    override fun observeAccountSheets(accountId: String): Flow<List<AccountSheetRecord>> =
        accountSheetDao.observeAll(accountId)

    override suspend fun upsertAccountSheet(record: AccountSheetRecord) = accountSheetDao.upsert(record)

    override suspend fun deleteAccountSheet(accountId: String, sheetId: String) =
        accountSheetDao.delete(accountId, sheetId)

    override suspend fun listIntentsForSheet(accountId: String, sheetId: String): List<SheetIntentRecord> =
        intentDao.listForSheet(accountId, sheetId)

    override suspend fun listActiveIntents(accountId: String): List<SheetIntentRecord> =
        intentDao.listActive(accountId)

    override fun observeIntents(accountId: String): Flow<List<SheetIntentRecord>> =
        intentDao.observeForAccount(accountId)

    override suspend fun getIntent(accountId: String, operationId: String): SheetIntentRecord? =
        intentDao.get(accountId, operationId)

    override suspend fun upsertIntent(intent: SheetIntentRecord) = intentDao.upsert(intent)

    override suspend fun upsertIntents(intents: List<SheetIntentRecord>) {
        if (intents.isNotEmpty()) intentDao.upsertAll(intents)
    }

    override suspend fun deleteIntent(accountId: String, operationId: String) =
        intentDao.delete(accountId, operationId)

    override fun observeOpenConflicts(accountId: String): Flow<List<SheetConflictRecord>> =
        conflictDao.observeOpen(accountId)

    override suspend fun getConflict(accountId: String, id: String): SheetConflictRecord? =
        conflictDao.get(accountId, id)

    override suspend fun upsertConflict(conflict: SheetConflictRecord) = conflictDao.upsert(conflict)

    override suspend fun upsertConflicts(conflicts: List<SheetConflictRecord>) {
        if (conflicts.isNotEmpty()) conflictDao.upsertAll(conflicts)
    }

    override suspend fun deleteConflict(accountId: String, id: String) = conflictDao.delete(accountId, id)

    override suspend fun getCharacter(sheetId: String): CharacterRecord? = characterDao.get(sheetId)

    override suspend fun allCharacters(): List<CharacterRecord> = characterDao.all()

    override suspend fun upsertCharacter(record: CharacterRecord) = characterDao.upsert(record)

    override suspend fun purgeCharacter(sheetId: String) = characterDao.purge(sheetId)

    override suspend fun getCampaign(campaignId: String): CampaignRecord? = campaignDao.campaign(campaignId)

    override suspend fun listOwnedActiveCampaignIds(uid: String): Set<String> =
        campaignDao.ownedCampaigns(uid)
            .filter { it.state == CampaignState.ACTIVE.name }
            .mapTo(linkedSetOf(), CampaignRecord::id)

    override suspend fun getMember(campaignId: String, uid: String): CampaignMemberRecord? =
        campaignDao.member(campaignId, uid)

    override suspend fun upsertMember(member: CampaignMemberRecord) = campaignDao.upsertMember(member)

    override suspend fun getOperationByIdempotencyKey(key: String): SessionOperationRecord? =
        operationsDao.operationByIdempotencyKey(key)

    override suspend fun upsertOperation(operation: SessionOperationRecord) = operationsDao.upsertOperation(operation)

    override suspend fun listLegacyOutbox(accountId: String): List<SheetOutboxRecord> =
        legacyDao.pending(accountId)

    override suspend fun markLegacyOutboxStatus(operationId: String, status: String) {
        val existing = legacyDao.operation(operationId) ?: return
        legacyDao.upsertOperation(existing.copy(status = status))
    }

    override suspend fun listLegacyRoutes(accountId: String): List<SheetRouteRecord> =
        db.openHelper.readableDatabase.query(
            "SELECT accountId, sheetId, status, accessEpoch, revision, confirmedJson FROM sheet_routes WHERE accountId = ?",
            arrayOf(accountId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        SheetRouteRecord(
                            accountId = cursor.getString(0),
                            sheetId = cursor.getString(1),
                            status = cursor.getString(2),
                            accessEpoch = cursor.getLong(3),
                            revision = cursor.getLong(4),
                            confirmedJson = cursor.getString(5),
                        ),
                    )
                }
            }
        }

    override suspend fun removeLegacyRoute(accountId: String, sheetId: String) =
        legacyDao.removeRoute(accountId, sheetId)
}

