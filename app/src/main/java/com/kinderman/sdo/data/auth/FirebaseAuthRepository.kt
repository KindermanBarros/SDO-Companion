package com.kinderman.sdo.data.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object MasterAccount {
    const val EMAIL = "kindbarros@gmail.com"

    fun roleFor(email: String?, storedRole: String?): UserRole = when {
        email.equals(EMAIL, ignoreCase = true) -> UserRole.MASTER
        storedRole == UserRole.MASTER.name -> UserRole.MASTER
        else -> UserRole.PLAYER
    }
}

class FirebaseAuthRepository : AuthRepository {
    private val auth get() = Firebase.auth
    override val configured: Boolean get() = runCatching { FirebaseApp.getInstance() }.isSuccess

    override val session = callbackFlow {
        if (!configured) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                trySend(null)
            } else {
                launch {
                    val session = loadSession(user.uid, user.email, user.displayName)
                    if (auth.currentUser?.uid == user.uid) trySend(session)
                }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun loginWithGoogle(activity: Activity): UserSession? {
        val resourceId = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        require(resourceId != 0) { "Configuração do Google Login não encontrada." }
        val option = GetSignInWithGoogleOption.Builder(activity.getString(resourceId)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val user = auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await().user
            ?: return null

        val profile = Firebase.firestore.collection("users").document(user.uid)
        val storedRole = profile.get().await().getString("role")
        val role = MasterAccount.roleFor(user.email, storedRole)
        profile.set(
            mapOf(
                "role" to role.name,
                "email" to user.email,
                "displayName" to user.displayName,
            ),
            SetOptions.merge(),
        ).await()
        return UserSession(user.uid, user.email.orEmpty(), user.displayName.orEmpty(), role)
    }

    override fun logout() = auth.signOut()

    private suspend fun loadSession(uid: String, email: String?, displayName: String?): UserSession {
        // On process restore FirebaseAuth can expose the cached user before refreshing its ID
        // token. Wait for a usable token before allowing Firestore synchronization to start.
        auth.currentUser?.takeIf { it.uid == uid }?.getIdToken(false)?.await()
        val storedRole = runCatching {
            Firebase.firestore.collection("users").document(uid).get().await().getString("role")
        }.getOrNull()
        return UserSession(uid, email.orEmpty(), displayName.orEmpty(), MasterAccount.roleFor(email, storedRole))
    }
}
