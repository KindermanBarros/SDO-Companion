package com.kinderman.sdo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import com.kinderman.sdo.data.local.CampaignDao
import com.kinderman.sdo.data.local.CampaignRecord
import com.kinderman.sdo.data.local.DeityGmCacheDao
import com.kinderman.sdo.data.local.DeityGmCacheRecord
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.domain.catalog.BuiltInCatalog
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.DeityDetailView
import com.kinderman.sdo.domain.model.DeityGmContent
import com.kinderman.sdo.domain.model.DeityPublicProfile
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.domain.model.toDeityPublicProfile
import com.kinderman.sdo.domain.policy.DeityAccessPolicy
import com.kinderman.sdo.domain.repository.CatalogRepository
import com.kinderman.sdo.domain.repository.ConnectivityMonitor
import com.kinderman.sdo.domain.repository.DeityRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface DeityGmRemoteDataSource {
    suspend fun fetchGmContent(
        session: UserSession,
        deityId: String,
        campaignId: String?,
    ): DeityGmContent?
}

@Singleton
class FirestoreDeityGmRemoteDataSource @Inject constructor() : DeityGmRemoteDataSource {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun fetchGmContent(
        session: UserSession,
        deityId: String,
        campaignId: String?,
    ): DeityGmContent? = withContext(Dispatchers.IO) {
        if (session.uid == "demo-player" || deityId.isBlank()) return@withContext null
        val store = runCatching { Firebase.firestore }.getOrNull() ?: return@withContext null
        val normalizedCampaignId = normalizeCampaignId(campaignId.orEmpty())

        if (!session.isAdmin && normalizedCampaignId.isBlank()) return@withContext null
        val context = store.collection(FirestoreCollections.GM_ACTIVE_CONTEXT).document(session.uid)
        try {
            if (normalizedCampaignId.isNotBlank()) {
                val campaignDoc = runCatching {
                    store.collection(FirestoreCollections.CAMPAIGNS)
                        .document(normalizedCampaignId)
                        .collection(FirestoreCollections.CAMPAIGN_DEITY_SECRETS_SUBCOLLECTION)
                        .document(deityId)
                        .get(Source.SERVER)
                        .await()
                }.getOrNull()
                val overrideJson = campaignDoc?.takeIf { it.exists() }?.getString("payloadJson")
                if (!overrideJson.isNullOrBlank()) {
                    return@withContext runCatching { json.decodeFromString<DeityGmContent>(overrideJson) }.getOrNull()
                }
            }

            if (!session.isAdmin) {
                context.set(
                    mapOf(
                        "uid" to session.uid,
                        "activeCampaignId" to normalizedCampaignId,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }

            val globalDoc = runCatching {
                store.collection(FirestoreCollections.DEITY_GM_CATALOG)
                    .document(deityId)
                    .get(Source.SERVER)
                    .await()
            }.getOrNull()
            val payloadJson = globalDoc?.takeIf { it.exists() }?.getString("payloadJson") ?: return@withContext null
            runCatching { json.decodeFromString<DeityGmContent>(payloadJson) }.getOrNull()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        } finally {
            if (!session.isAdmin) context.delete()
        }
    }
}

@Singleton
class SecuredDeityRepository @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val campaignDao: CampaignDao,
    private val gmCacheDao: DeityGmCacheDao,
    private val remoteDataSource: DeityGmRemoteDataSource,
    private val connectivityMonitor: ConnectivityMonitor,
) : DeityRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override fun observePublicDeities(): Flow<List<DeityPublicProfile>> =
        catalogRepository.observe()
            .map { entries ->
                val deityEntries = entries.filter { it.kind == CatalogKind.DEITY }
                    .ifEmpty { BuiltInCatalog.entries.filter { it.kind == CatalogKind.DEITY } }
                deityEntries
                    .distinctBy(CatalogEntry::id)
                    .map(CatalogEntry::toDeityPublicProfile)
                    .sortedWith(compareBy(DeityPublicProfile::navigationOrder, DeityPublicProfile::name))
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    override suspend fun getDeityDetail(
        session: UserSession?,
        deityId: String,
        campaignId: String?,
    ): DeityDetailView? = withContext(Dispatchers.IO) {
        val publicEntry = catalogRepository.observe().first()
            .firstOrNull { it.kind == CatalogKind.DEITY && it.id == deityId }
            ?: BuiltInCatalog.entries.firstOrNull { it.kind == CatalogKind.DEITY && it.id == deityId }
            ?: return@withContext null
        val publicProfile = publicEntry.toDeityPublicProfile()

        val campaignsById = campaignDao.allCampaigns()
            .map(CampaignRecord::toDomain)
            .associateBy { it.id }

        if (!DeityAccessPolicy.canReadGmContent(session, campaignId, campaignsById)) {
            return@withContext DeityDetailView(publicProfile = publicProfile, gmContent = null)
        }

        val verifiedSession = checkNotNull(session)
        val normalizedCampaignId = normalizeCampaignId(campaignId.orEmpty())
        val cacheScope = if (verifiedSession.isAdmin && normalizedCampaignId.isBlank()) {
            "ADMIN"
        } else {
            normalizedCampaignId
        }

        val online = connectivityMonitor.isOnline.value
        val remote = if (online) {
            remoteDataSource.fetchGmContent(verifiedSession, deityId, normalizedCampaignId)?.also { fetched ->
                gmCacheDao.upsert(
                    DeityGmCacheRecord(
                        viewerUid = verifiedSession.uid,
                        campaignScopeId = cacheScope,
                        deityId = deityId,
                        payloadJson = json.encodeToString(DeityGmContent.serializer(), fetched),
                        cachedAt = System.currentTimeMillis(),
                    ),
                )
            }
        } else {
            null
        }

        val resolvedGm = if (online) remote else gmCacheDao.find(verifiedSession.uid, cacheScope, deityId)
            ?.payloadJson
            ?.let { raw -> runCatching { json.decodeFromString<DeityGmContent>(raw) }.getOrNull() }

        DeityDetailView(publicProfile = publicProfile, gmContent = resolvedGm)
    }

    override suspend fun clearUnauthorizedCaches(activeUid: String?) = withContext(Dispatchers.IO) {
        if (activeUid.isNullOrBlank()) {
            gmCacheDao.deleteAll()
        } else {
            gmCacheDao.deleteOtherViewers(activeUid)
        }
    }
}
