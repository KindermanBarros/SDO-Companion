package com.kinderman.sdo.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kinderman.sdo.BuildConfig
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real authenticated Firebase integration test executed exclusively on the `dev` flavor.
 *
 * Uses `BuildConfig.DEV_TEST_EMAIL` / `BuildConfig.DEV_TEST_PASSWORD` (or anonymous auth when
 * enabled on the dev project) and skips cleanly when the dev `google-services.json` is absent.
 */
@RunWith(AndroidJUnit4::class)
class AuthenticatedDevFirebaseInstrumentedTest {

    @Test
    fun devFlavorAuthenticatesAndReadsWritesDevFirestore() = runBlocking {
        assertTrue("Expected dev flavor BuildConfig.IS_DEV_ENV=true", BuildConfig.IS_DEV_ENV)
        assertTrue(
            "Expected .dev applicationId suffix, got ${BuildConfig.APPLICATION_ID}",
            BuildConfig.APPLICATION_ID.endsWith(".dev"),
        )

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firebaseInitialized = FirebaseApp.getApps(context).isNotEmpty()
        assumeTrue("Skipping authenticated dev test: FirebaseApp not initialized (no dev google-services.json).", firebaseInitialized)

        val auth = FirebaseAuth.getInstance()
        val args = InstrumentationRegistry.getArguments()
        val email = args.getString("sdoDevTestEmail").orEmpty().trim()
        val password = args.getString("sdoDevTestPassword").orEmpty()
        assumeTrue(
            "Skipping authenticated dev test: SDO_DEV_TEST_EMAIL / SDO_DEV_TEST_PASSWORD not configured.",
            email.isNotEmpty() && password.isNotEmpty(),
        )

        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val user = authResult.user
        assertNotNull("Expected signed-in FirebaseUser", user)
        val uid = user!!.uid
        assertFalse("Expected non-blank FirebaseAuth uid", uid.isBlank())

        val token = user.getIdToken(false).await().token
        assertFalse("Expected non-blank FirebaseAuth ID token", token.isNullOrBlank())

        val firestore = FirebaseFirestore.getInstance()
        val userDocRef = firestore.collection("users").document(uid)
        val probeTimestamp = System.currentTimeMillis()
        userDocRef.set(
            mapOf(
                "uid" to uid,
                "email" to (user.email ?: email),
                "lastDevTestAt" to probeTimestamp,
            ),
            SetOptions.merge(),
        ).await()

        val snapshot = userDocRef.get().await()
        assertTrue("Expected user profile document to exist in dev Firestore", snapshot.exists())
        assertEquals(uid, snapshot.getString("uid"))
        assertEquals(probeTimestamp, snapshot.getLong("lastDevTestAt"))

        val ownedCharacters = firestore.collection("characters")
            .whereEqualTo("ownerId", uid)
            .limit(5)
            .get()
            .await()
        assertNotNull(ownedCharacters)
    }
}
