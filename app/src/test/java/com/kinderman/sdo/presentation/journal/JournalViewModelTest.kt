package com.kinderman.sdo.presentation.journal

import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignJournalDraft
import com.kinderman.sdo.domain.model.CampaignJournalEntry
import com.kinderman.sdo.domain.model.CampaignState
import com.kinderman.sdo.domain.model.JournalSaveResult
import com.kinderman.sdo.domain.model.JournalSnapshot
import com.kinderman.sdo.domain.model.JournalSyncState
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.repository.CampaignJournalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalViewModelTest {
    private val session = UserSession(
        uid = "player-1",
        email = "player@sdo.test",
        displayName = "Lyra",
        role = UserRole.USER,
    )
    private val campaign = Campaign(
        id = "camp-1",
        name = "Cinzas de Vespera",
        ownerId = "gm-1",
        state = CampaignState.ACTIVE,
    )

    @Test
    fun createEditAndDeleteFlowUpdatesStateCleanly() = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        try {
            val repo = FakeCampaignJournalRepository()
            val viewModel = JournalViewModel(repo, testScope)
            viewModel.bind(session, campaign, isActiveMember = true)
            repo.emitSnapshot(JournalSnapshot(emptyList(), JournalSyncState.LIVE))

            viewModel.startNewEntry()
            viewModel.updateDraft("Sessão 01", "O grupo cruzou a ponte.")
            viewModel.saveDraft()

            assertEquals(JournalMode.LIST, viewModel.state.value.mode)
            assertEquals(1, viewModel.state.value.entries.size)
            val created = viewModel.state.value.entries.first()

            viewModel.openEntry(created.id)
            assertEquals(JournalMode.DETAIL, viewModel.state.value.mode)

            viewModel.startEditEntry(created)
            viewModel.updateDraft("Sessão 01 (revisada)", "O grupo cruzou a ponte sob neblina.")
            viewModel.saveDraft()

            assertEquals("Sessão 01 (revisada)", viewModel.state.value.entries.first().title)
            assertEquals(2L, viewModel.state.value.entries.first().revision)

            val edited = viewModel.state.value.entries.first()
            viewModel.openEntry(edited.id)
            viewModel.requestDelete(edited)
            viewModel.confirmDelete()

            assertTrue(viewModel.state.value.entries.isEmpty())
            assertEquals(JournalMode.LIST, viewModel.state.value.mode)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun uncertainSaveReconcilesOperationWithoutDuplicate() = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        try {
            val repo = FakeCampaignJournalRepository()
            val viewModel = JournalViewModel(repo, testScope)
            viewModel.bind(session, campaign, isActiveMember = true)
            repo.emitSnapshot(JournalSnapshot(emptyList(), JournalSyncState.LIVE))

            viewModel.startNewEntry()
            viewModel.updateDraft("Registro incerto", "Texto importante que caiu na rede.")
            val draft = viewModel.state.value.draft!!

            repo.nextSaveResult = JournalSaveResult.Uncertain(
                entryId = draft.entryId,
                operationId = draft.operationId,
                message = "Conexão oscilou.",
            )
            viewModel.saveDraft()

            assertTrue(viewModel.state.value.uncertainSave)
            assertEquals(JournalMode.EDITOR, viewModel.state.value.mode)

            repo.verifiedEntry = CampaignJournalEntry(
                id = draft.entryId,
                campaignId = campaign.id,
                authorId = session.uid,
                authorName = session.displayName,
                title = draft.title,
                body = draft.body,
                revision = 1L,
                operationId = draft.operationId,
            )
            viewModel.saveDraft()

            assertFalse(viewModel.state.value.uncertainSave)
            assertEquals(JournalMode.LIST, viewModel.state.value.mode)
            assertEquals(1, repo.saveCalls)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun conflictAllowsCompareReloadAndReplace() = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        try {
            val repo = FakeCampaignJournalRepository()
            val initial = CampaignJournalEntry(
                id = "e-1",
                campaignId = campaign.id,
                authorId = session.uid,
                authorName = session.displayName,
                title = "Inicial",
                body = "Versão 1",
                revision = 1L,
            )
            val serverNewer = initial.copy(title = "Servidor", body = "Versão 2", revision = 2L)
            repo.emitSnapshot(JournalSnapshot(listOf(initial), JournalSyncState.LIVE))

            val viewModel = JournalViewModel(repo, testScope)
            viewModel.bind(session, campaign, isActiveMember = true)

            viewModel.startEditEntry(initial)
            viewModel.updateDraft("Rascunho Local", "Texto editado localmente")
            repo.nextSaveResult = JournalSaveResult.Conflict(serverNewer)
            viewModel.saveDraft()

            assertEquals(JournalMode.CONFLICT, viewModel.state.value.mode)
            assertEquals(serverNewer, viewModel.state.value.conflictServerEntry)

            viewModel.reloadConflict()
            assertEquals(JournalMode.EDITOR, viewModel.state.value.mode)
            assertEquals(serverNewer.title, viewModel.state.value.draft?.title)
            assertEquals(serverNewer.body, viewModel.state.value.draft?.body)

            viewModel.startEditEntry(initial)
            viewModel.updateDraft("Rascunho Forçado", "Sobrescrevendo versão")
            repo.nextSaveResult = JournalSaveResult.Conflict(serverNewer)
            viewModel.saveDraft()

            repo.nextSaveResult = null
            viewModel.requestReplaceConflict()
            assertTrue(viewModel.state.value.replaceConfirmVisible)
            viewModel.confirmReplaceConflict()

            assertEquals(JournalMode.LIST, viewModel.state.value.mode)
            assertEquals(3L, viewModel.state.value.entries.first().revision)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun dirtyBackRequiresDiscardConfirmationAndAccessRevokedClearsState() = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        try {
            val repo = FakeCampaignJournalRepository()
            val viewModel = JournalViewModel(repo, testScope)
            viewModel.bind(session, campaign, isActiveMember = true)
            repo.emitSnapshot(JournalSnapshot(emptyList(), JournalSyncState.LIVE))

            viewModel.startNewEntry()
            viewModel.updateDraft("Rascunho", "Ainda digitando")
            var exited = false
            viewModel.handleBack { exited = true }
            assertFalse(exited)
            assertTrue(viewModel.state.value.discardConfirmVisible)

            viewModel.confirmDiscard()
            assertFalse(viewModel.state.value.discardConfirmVisible)
            assertEquals(JournalMode.LIST, viewModel.state.value.mode)

            repo.emitSnapshot(JournalSnapshot(emptyList(), JournalSyncState.ACCESS_REVOKED))
            assertEquals(JournalSyncState.ACCESS_REVOKED, viewModel.state.value.syncState)
            assertNull(viewModel.state.value.draft)

            viewModel.clearContext()
            assertTrue(repo.clearedAccounts.contains(session.uid))
        } finally {
            testScope.cancel()
        }
    }

    private class FakeCampaignJournalRepository : CampaignJournalRepository {
        private val snapshotFlow = MutableStateFlow(JournalSnapshot(emptyList(), JournalSyncState.LOADING))
        private val stored = linkedMapOf<String, CampaignJournalEntry>()
        var nextSaveResult: JournalSaveResult? = null
        var verifiedEntry: CampaignJournalEntry? = null
        var saveCalls: Int = 0
        val clearedAccounts = mutableListOf<String>()

        fun emitSnapshot(snapshot: JournalSnapshot) {
            snapshot.entries.forEach { stored[it.id] = it }
            snapshotFlow.value = snapshot
        }

        override fun observe(session: UserSession, campaign: Campaign): Flow<JournalSnapshot> = snapshotFlow

        override suspend fun save(
            session: UserSession,
            campaign: Campaign,
            draft: CampaignJournalDraft,
            expectedRevision: Long?,
        ): JournalSaveResult {
            saveCalls++
            nextSaveResult?.let { forced ->
                nextSaveResult = null
                return forced
            }
            val nextRevision = (expectedRevision ?: 0L) + 1L
            val entry = CampaignJournalEntry(
                id = draft.entryId,
                campaignId = campaign.id,
                authorId = stored[draft.entryId]?.authorId ?: session.uid,
                authorName = stored[draft.entryId]?.authorName ?: session.displayName,
                title = draft.title,
                body = draft.body,
                createdAt = stored[draft.entryId]?.createdAt ?: 100L,
                updatedAt = 200L,
                revision = nextRevision,
                lastEditorId = session.uid,
                lastEditorName = session.displayName,
                operationId = draft.operationId,
            )
            stored[entry.id] = entry
            snapshotFlow.value = JournalSnapshot(stored.values.toList(), JournalSyncState.LIVE)
            return JournalSaveResult.Confirmed(entry)
        }

        override suspend fun delete(
            session: UserSession,
            campaign: Campaign,
            entry: CampaignJournalEntry,
            operationId: String,
        ): JournalSaveResult {
            stored.remove(entry.id)
            snapshotFlow.value = JournalSnapshot(stored.values.toList(), JournalSyncState.LIVE)
            return JournalSaveResult.Confirmed(entry.copy(deleted = true))
        }

        override suspend fun verifyOperation(
            session: UserSession,
            campaignId: String,
            entryId: String,
            operationId: String,
        ): CampaignJournalEntry? = verifiedEntry

        override fun clearAccountCache(uid: String) {
            clearedAccounts += uid
        }
    }
}
