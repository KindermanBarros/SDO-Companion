package com.kinderman.sdo.data

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    suspend fun login(email: String, password: String) = auth.signInWithEmailAndPassword(email, password).await().user
    suspend fun register(email: String, password: String) = auth.createUserWithEmailAndPassword(email, password).await().user.also { user ->
        user?.let { FirebaseFirestore.getInstance().collection("users").document(it.uid).set(mapOf("role" to "PLAYER", "email" to email)).await() }
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
