package com.kinderman.sdo.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kinderman.sdo.data.local.AppDatabase
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.repository.FirestoreCampaignJournalRepository
import com.kinderman.sdo.data.repository.SyncedCampaignRepository
import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.AbilityCost
import com.kinderman.sdo.domain.model.AbilityKind
import com.kinderman.sdo.domain.model.CURRENT_CHARACTER_SCHEMA_VERSION
import com.kinderman.sdo.domain.model.CampaignJournalDraft
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.JournalSaveResult
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.SessionResource
import com.kinderman.sdo.domain.model.SheetIntentStatus
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SparkEmulatorTwoAccountInstrumentedTest {
    private lateinit var playerDb: AppDatabase
    private lateinit var gmDb: AppDatabase
    private lateinit var playerScope: CoroutineScope
    private lateinit var gmScope: CoroutineScope

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        playerDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        gmDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        playerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        gmScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun tearDown() {
        playerScope.cancel()
        gmScope.cancel()
        playerDb.close()
        gmDb.close()
    }

    @Test
    fun twoAccountsSyncTransactionsAndJournalAgainstFirebaseEmulator() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val noOpScheduler = object : SparkWorkScheduler {
            override fun schedule(accountId: String) = Unit
        }
        val playerEngine = SparkFirestoreSyncEngine(
            localStore = RoomSparkLocalStore(playerDb),
            remoteGateway = FirestoreSparkRemoteGateway(),
            scope = playerScope,
            workScheduler = noOpScheduler,
        )
        val gmEngine = SparkFirestoreSyncEngine(
            localStore = RoomSparkLocalStore(gmDb),
            remoteGateway = FirestoreSparkRemoteGateway(),
            scope = gmScope,
            workScheduler = noOpScheduler,
        )
        val playerCampaignRepo = SyncedCampaignRepository(playerDb.campaignDao(), playerDb.characterDao(), playerEngine)
        val gmCampaignRepo = SyncedCampaignRepository(gmDb.campaignDao(), gmDb.characterDao(), gmEngine)
        val journalRepo = FirestoreCampaignJournalRepository()

        val suffix = UUID.randomUUID().toString().take(8)
        val playerUid = signInOrCreate("player-$suffix@sdo.local", "test-pass-1234")
        val playerSession = UserSession(playerUid, "player-$suffix@sdo.local", "Player Test", UserRole.USER)

        val gmUid = signInOrCreate("gm-$suffix@sdo.local", "test-pass-1234")
        val gmSession = UserSession(gmUid, "gm-$suffix@sdo.local", "GM Test", UserRole.USER)

        // 1. Player creates and saves a sheet with inv_, abil_, and lcond_ items.
        signInExisting("player-$suffix@sdo.local", "test-pass-1234")
        val created = playerEngine.create(playerSession)
        val edited = created.copy(
            name = "Lyra-$suffix",
            level = 3,
            life = ResourceValue(current = 20, maximum = 25),
            inventory = listOf(
                InventoryItem(
                    id = "inv-1-$suffix",
                    name = "Espada Longa",
                    quantity = 1,
                    enhancementSlots = 2,
                ),
            ),
            abilities = listOf(
                Ability(
                    id = "abil-1-$suffix",
                    name = "Corte Arcano",
                    kind = AbilityKind.POWER,
                    cost = AbilityCost.PowerCost(amount = 2),
                    effect = "Ataque corpo a corpo.",
                ),
            ),
            conditions = listOf(
                ConditionEffect(
                    id = "cond-1-$suffix",
                    name = "Sangramento",
                    intensity = "1",
                    duration = "2",
                    origin = "Combate",
                    summary = "Perde 1 PV por turno.",
                ),
            ),
            canonicalSchemaVersion = CURRENT_CHARACTER_SCHEMA_VERSION,
        )
        playerEngine.save(playerSession, edited)
        assertTrue("Player initial drain must succeed", playerEngine.drain(playerUid))
        val playerIntentsAfterCreate = playerDb.sheetIntentDao().listForSheet(playerUid, created.id)
        assertTrue(
            "Expected all intents CONFIRMED on create/save, got: ${playerIntentsAfterCreate.filter { it.status != SheetIntentStatus.CONFIRMED.name }}",
            playerIntentsAfterCreate.all { it.status == SheetIntentStatus.CONFIRMED.name },
        )

        // 2. GM creates a campaign & invite and syncs to Firestore.
        signInExisting("gm-$suffix@sdo.local", "test-pass-1234")
        val campaign = gmCampaignRepo.create(gmSession, "Mesa-$suffix", "Teste emulador")
        val invite = gmCampaignRepo.createInvite(gmSession, campaign)
        gmCampaignRepo.sync(gmSession)

        // 3. Player joins by invite code and links their character.
        signInExisting("player-$suffix@sdo.local", "test-pass-1234")
        val joinedCampaign = playerCampaignRepo.joinByCode(playerSession, invite.code)
        val currentLocalChar = playerDb.characterDao().get(created.id)!!.toDomain()
        assertEquals("Local abilities must be 1", 1, currentLocalChar.abilities.size)
        playerCampaignRepo.linkCharacter(playerSession, currentLocalChar, joinedCampaign)

        // 4. GM syncs from cloud and must receive the Player's full character.
        signInExisting("gm-$suffix@sdo.local", "test-pass-1234")
        gmCampaignRepo.sync(gmSession)
        gmEngine.sync(gmSession)
        val gmSyncedChar = gmDb.characterDao().get(created.id)?.toDomain()
        assertEquals("Lyra-$suffix", gmSyncedChar?.name)
        assertEquals(1, gmSyncedChar?.inventory?.size)
        assertEquals(1, gmSyncedChar?.abilities?.size)
        assertEquals(1, gmSyncedChar?.conditions?.size)
        assertEquals(20, gmSyncedChar?.life?.current)

        // 5. GM applies a session resource delta (-5 life) and drains.
        val applied = gmEngine.applySessionCommand(
            gmSession,
            gmSyncedChar!!,
            SessionCommand(
                type = SessionOperationType.RESOURCE,
                resource = SessionResource.LIFE,
                amount = -5,
                label = "Dano",
                idempotencyKey = "op-gm-dmg-$suffix",
            ),
        )
        assertTrue(applied)
        assertTrue("GM drain must succeed", gmEngine.drain(gmUid))

        val gmAfterDrain = gmDb.characterDao().get(created.id)?.toDomain()
        assertTrue("Expected GM life to decrease from 20, got ${gmAfterDrain?.life?.current}", (gmAfterDrain?.life?.current ?: 20) < 20)

        // 6. Player syncs from cloud and must receive the GM's life change.
        signInExisting("player-$suffix@sdo.local", "test-pass-1234")
        playerEngine.sync(playerSession)
        val playerRefreshed = playerDb.characterDao().get(created.id)?.toDomain()
        assertEquals(gmAfterDrain?.life?.current, playerRefreshed?.life?.current)

        // 7. Player creates & deletes a Campaign Journal entry without PERMISSION_DENIED.
        val draft = CampaignJournalDraft(
            entryId = "journal-$suffix",
            title = "Diario $suffix",
            body = "Sessao 1 no emulador.",
            operationId = "jop-$suffix",
        )
        val saveResult = journalRepo.save(playerSession, joinedCampaign, draft, expectedRevision = null)
        assertTrue("Expected Confirmed, got $saveResult", saveResult is JournalSaveResult.Confirmed)
        val savedEntry = (saveResult as JournalSaveResult.Confirmed).entry
        val deleteResult = journalRepo.delete(playerSession, joinedCampaign, savedEntry, "jop-del-$suffix")
        assertTrue("Expected Confirmed on delete, got $deleteResult", deleteResult is JournalSaveResult.Confirmed)
    }

    private suspend fun signInOrCreate(email: String, password: String): String {
        val auth = FirebaseAuth.getInstance()
        return try {
            auth.createUserWithEmailAndPassword(email, password).await().user!!.uid
        } catch (_: Exception) {
            auth.signInWithEmailAndPassword(email, password).await().user!!.uid
        }
    }

    private suspend fun signInExisting(email: String, password: String) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
    }

    companion object {
        private var configured = false

        @JvmStatic
        @BeforeClass
        fun configureEmulators() {
            if (configured) return
            val context = ApplicationProvider.getApplicationContext<Context>()
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance().useEmulator("127.0.0.1", 9099)
            FirebaseFirestore.getInstance().useEmulator("127.0.0.1", 8080)
            configured = true
        }
    }
}
