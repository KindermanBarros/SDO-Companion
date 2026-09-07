package com.kinderman.sdo.data

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth get() = FirebaseAuth.getInstance()
    val configured get() = runCatching { FirebaseApp.getInstance() }.isSuccess
    val session = callbackFlow {
        if (!configured) { trySend(null); close(); return@callbackFlow }
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    suspend fun loginWithGoogle(context: Activity) = run {
        val resourceId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        require(resourceId != 0) { "Configuração do Google Login não encontrada." }
        val option = GetSignInWithGoogleOption.Builder(
            context.getString(resourceId)
        )
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await().user
            ?.also { user ->
                val profile = FirebaseFirestore.getInstance().collection("users").document(user.uid)
                if (!profile.get().await().exists()) {
                    profile.set(mapOf("role" to "PLAYER", "email" to user.email, "displayName" to user.displayName), SetOptions.merge()).await()
                }
            }
    }
    suspend fun isMaster(uid: String) = FirebaseFirestore.getInstance().collection("users").document(uid).get().await().getString("role") == "MASTER"
    fun logout() = runCatching { auth.signOut() }
}

class CharacterRepository(private val dao: CharacterDao) {
    fun observe(uid: String, isMaster: Boolean): Flow<List<CharacterEntity>> = dao.observe(uid, isMaster)
    fun observeOne(id: String) = dao.observeOne(id)
    suspend fun save(character: CharacterEntity) = dao.upsert(character.copy(updatedAt = System.currentTimeMillis(), dirty = true))
    suspend fun sync(uid: String, isMaster: Boolean) {
        val store = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return
        dao.dirty().filter { it.ownerId == uid }.forEach { c ->
            store.collection("characters").document(c.id).set(c.copy(dirty = false)).await()
            dao.markSynced(c.id)
        }
        val query = if (isMaster) store.collection("characters") else store.collection("characters").whereEqualTo("ownerId", uid)
        query.get().await().documents.mapNotNull { it.toObject(CharacterEntity::class.java) }.forEach { dao.upsert(it.copy(dirty = false)) }
    }
}
