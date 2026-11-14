package com.kinderman.sdo.presentation

import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignAlertSettings
import com.kinderman.sdo.domain.model.CampaignDelivery
import com.kinderman.sdo.domain.model.CampaignInvite
import com.kinderman.sdo.domain.model.CampaignInvitePreview
import com.kinderman.sdo.domain.model.CampaignLibraryEntry
import com.kinderman.sdo.domain.model.CampaignMember
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperation
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.repository.CampaignRepository
import com.kinderman.sdo.domain.repository.CatalogRepository
import com.kinderman.sdo.domain.repository.CharacterRepository
import com.kinderman.sdo.domain.repository.ConnectivityMonitor
import com.kinderman.sdo.domain.repository.OperationsRepository
import com.kinderman.sdo.domain.repository.OwnerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppViewModelSyncPolicyTest {
    private val session = UserSession("user-1", "user@sdo.test", "Jogador", UserRole.USER)
    private lateinit var testScope: CoroutineScope
    private lateinit var connectivity: FakeConnectivityMonitor
    private lateinit var characterRepo: FakeCharacterRepository
    private lateinit var campaignRepo: FakeCampaignRepository
    private lateinit var operationsRepo: FakeOperationsRepository
    private lateinit var viewModel: AppViewModel

    @Before
    fun setUp() {
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        connectivity = FakeConnectivityMonitor(initialOnline = true)
        characterRepo = FakeCharacterRepository()
        campaignRepo = FakeCampaignRepository()
        operationsRepo = FakeOperationsRepository()
        viewModel = AppViewModel(
            repository = characterRepo,
            ownerRepository = FakeOwnerRepository(),
            catalogRepository = FakeCatalogRepository(),
            campaignRepository = campaignRepo,
            operationsRepository = operationsRepo,
            connectivityMonitor = connectivity,
            externalScope = testScope,
        )
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun offlineAuthenticatedSessionBlocksMutationsWithoutQueuingEdits() = runBlocking {
        viewModel.setSession(session)
        connectivity.onlineState.value = false

        assertTrue(viewModel.isOfflineReadOnly.first { it })

        val character = Character(id = "char-1", ownerId = session.uid, name = "Aria")
        viewModel.save(character)
        viewModel.autosave(character)
        viewModel.createCampaign("Campanha Offline", "")
        viewModel.applySessionCommand(character, SessionCommand(type = SessionOperationType.MONEY, amount = 5))
        viewModel.delete(character)

        assertEquals(0, characterRepo.saveCount)
        assertEquals(0, characterRepo.deleteCount)
        assertEquals(0, campaignRepo.createCount)
        assertEquals(0, operationsRepo.applyCount)
        assertEquals(AppViewModel.OFFLINE_READ_ONLY_MESSAGE, viewModel.message.value)
    }

    @Test
    fun reconnectingRestoresEditingAndTriggersSync() = runBlocking {
        connectivity.onlineState.value = false
        viewModel.setSession(session)
        assertTrue(viewModel.isOfflineReadOnly.first { it })
        assertEquals(0, characterRepo.syncCount)

        connectivity.onlineState.value = true
        assertFalse(viewModel.isOfflineReadOnly.first { !it })
        assertTrue(characterRepo.syncCount >= 1)

        viewModel.save(Character(id = "char-1", ownerId = session.uid, name = "Aria"))
        assertEquals(1, characterRepo.saveCount)
    }

    @Test
    fun remoteChangeSignalTriggersRealtimeSyncWhenOnline() = runBlocking {
        viewModel.setSession(session)
        val initialSyncs = characterRepo.syncCount

        characterRepo.remoteChanges.emit(Unit)
        delay(450)

        assertTrue(characterRepo.syncCount > initialSyncs)
    }

    @Test
    fun demoSessionRemainsEditableOffline() = runBlocking {
        connectivity.onlineState.value = false
        viewModel.setDemoSession()

        assertFalse(viewModel.isOfflineReadOnly.value)
        viewModel.save(Character(id = "char-demo", ownerId = AppViewModel.DEMO_USER_ID, name = "Demo"))

        assertEquals(1, characterRepo.saveCount)
    }

    private class FakeConnectivityMonitor(initialOnline: Boolean) : ConnectivityMonitor {
        val onlineState = MutableStateFlow(initialOnline)
        override val isOnline = onlineState
    }

    private class FakeCharacterRepository : CharacterRepository {
        val remoteChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
        var saveCount = 0
        var deleteCount = 0
        var syncCount = 0

        override fun observe(session: UserSession): Flow<List<Character>> = flowOf(emptyList())
        override fun observeOne(id: String): Flow<Character?> = flowOf(null)
        override fun observeRemoteChanges(session: UserSession, campaignIds: Set<String>): Flow<Unit> = remoteChanges
        override suspend fun create(session: UserSession, ownerId: String, campaignId: String): Character =
            Character(ownerId = ownerId, campaignId = campaignId)

        override suspend fun save(session: UserSession, character: Character) {
            saveCount++
        }

        override suspend fun setPlayerLocked(session: UserSession, character: Character, locked: Boolean) = Unit
        override suspend fun setHistorianLocked(session: UserSession, character: Character, locked: Boolean) = Unit
        override suspend fun transferOwnership(session: UserSession, character: Character, owner: UserProfile) = Unit
        override suspend fun delete(session: UserSession, character: Character) {
            deleteCount++
        }

        override suspend fun sync(session: UserSession): List<CharacterSyncConflict> {
            syncCount++
            return emptyList()
        }

        override suspend fun resolveConflict(
            session: UserSession,
            conflict: CharacterSyncConflict,
            remoteFieldIds: Set<String>,
        ) = Unit
    }

    private class FakeCampaignRepository : CampaignRepository {
        var createCount = 0
        override fun observe(session: UserSession): Flow<List<Campaign>> = flowOf(emptyList())
        override fun observeMembers(campaignId: String): Flow<List<CampaignMember>> = flowOf(emptyList())
        override fun observeMemberships(session: UserSession): Flow<List<CampaignMember>> = flowOf(emptyList())
        override fun observeInvites(campaignId: String): Flow<List<CampaignInvite>> = flowOf(emptyList())
        override fun observeAllInvites(): Flow<List<CampaignInvite>> = flowOf(emptyList())
        override suspend fun create(session: UserSession, name: String, description: String): Campaign {
            createCount++
            return Campaign(name = name, description = description, ownerId = session.uid)
        }

        override suspend fun update(session: UserSession, campaign: Campaign) = Unit
        override suspend fun archive(session: UserSession, campaign: Campaign, archived: Boolean) = Unit
        override suspend fun delete(session: UserSession, campaign: Campaign) = Unit
        override suspend fun leave(session: UserSession, campaign: Campaign) = Unit
        override suspend fun removeMember(session: UserSession, campaign: Campaign, userId: String) = Unit
        override suspend fun createInvite(session: UserSession, campaign: Campaign): CampaignInvite =
            CampaignInvite(code = "ABC123")

        override suspend fun previewInvite(session: UserSession, code: String): CampaignInvitePreview? = null
        override suspend fun joinByCode(session: UserSession, code: String): Campaign = Campaign()
        override suspend fun linkCharacter(session: UserSession, character: Character, campaign: Campaign): Character = character
        override suspend fun unlinkCharacter(session: UserSession, character: Character): Character = character
        override suspend fun sync(session: UserSession) = Unit
    }

    private class FakeOperationsRepository : OperationsRepository {
        var applyCount = 0
        override fun observeAudit(campaignIds: Set<String>): Flow<List<SessionOperation>> = flowOf(emptyList())
        override fun observeLibrary(campaignIds: Set<String>): Flow<List<CampaignLibraryEntry>> = flowOf(emptyList())
        override fun observeDeliveries(session: UserSession, campaignIds: Set<String>): Flow<List<CampaignDelivery>> = flowOf(emptyList())
        override fun observeAlertSettings(campaignIds: Set<String>): Flow<List<CampaignAlertSettings>> = flowOf(emptyList())
        override suspend fun apply(session: UserSession, character: Character, command: SessionCommand): Boolean {
            applyCount++
            return true
        }

        override suspend fun saveLibrary(session: UserSession, entry: CampaignLibraryEntry) = Unit
        override suspend fun duplicateLibrary(session: UserSession, entry: CampaignLibraryEntry) = Unit
        override suspend fun archiveLibrary(session: UserSession, entry: CampaignLibraryEntry, archived: Boolean) = Unit
        override suspend fun deliver(
            session: UserSession,
            entry: CampaignLibraryEntry,
            recipients: List<Character>,
            knowledgeMappings: Map<String, String>,
        ) = Unit

        override suspend fun respondToDelivery(session: UserSession, delivery: CampaignDelivery, accept: Boolean) = Unit
        override suspend fun saveAlertSettings(settings: CampaignAlertSettings) = Unit
        override suspend fun sync(
            session: UserSession,
            readableCampaignIds: Set<String>,
            writableCampaignIds: Set<String>,
        ) = Unit
    }

    private class FakeOwnerRepository : OwnerRepository {
        override fun observe(): Flow<List<UserProfile>> = flowOf(emptyList())
        override suspend fun sync(session: UserSession) = Unit
    }

    private class FakeCatalogRepository : CatalogRepository {
        override fun observe(): Flow<List<CatalogEntry>> = flowOf(emptyList())
        override suspend fun refreshBundledCatalog() = Unit
    }
}
