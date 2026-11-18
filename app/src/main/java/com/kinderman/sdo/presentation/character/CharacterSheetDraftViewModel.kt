package com.kinderman.sdo.presentation.character

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.applyCharacterEditKeys
import com.kinderman.sdo.domain.model.characterEditKeys
import com.kinderman.sdo.domain.model.serverSyncRevision

/** Keeps the current editing draft and up to 5 local undo steps through Activity recreation. */
internal class CharacterSheetDraftViewModel(initial: Character) : ViewModel() {
    val current = mutableStateOf(initial)
    val undoCount = mutableStateOf(0)
    private var observed = initial
    private val undoHistory = ArrayDeque<Character>()

    /** Records a local edit, retaining at most the 5 most recent prior states. */
    fun recordEdit(previous: Character, next: Character) {
        if (undoHistory.size >= MAX_UNDO_STEPS) {
            undoHistory.removeFirst()
        }
        undoHistory.addLast(previous)
        undoCount.value = undoHistory.size
        current.value = next
    }

    /** Restores the most recent local change, if any, and returns the updated draft. */
    fun undo(): Character? {
        if (undoHistory.isEmpty()) return null
        val previous = undoHistory.removeLast()
        val restored = previous.copy(
            updatedAt = maxOf(System.currentTimeMillis(), current.value.updatedAt + 1),
            dirty = true,
        )
        current.value = restored
        undoCount.value = undoHistory.size
        return restored
    }

    /** Rebase only edits not yet reflected by the repository; unrelated remote fields stay live. */
    fun accept(incoming: Character) {
        if (incoming.serverSyncRevision() < observed.serverSyncRevision()) return
        val local = current.value
        val pendingKeys = characterEditKeys(observed, local)
        current.value = applyCharacterEditKeys(incoming, local, pendingKeys).copy(
            editingBase = incoming,
            dirty = incoming.dirty || pendingKeys.isNotEmpty(),
        )
        observed = incoming
    }

    private companion object {
        const val MAX_UNDO_STEPS = 5
    }
}
