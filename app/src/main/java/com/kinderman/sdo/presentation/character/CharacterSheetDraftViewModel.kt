package com.kinderman.sdo.presentation.character

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.applyCharacterEditKeys
import com.kinderman.sdo.domain.model.characterEditKeys
import com.kinderman.sdo.domain.model.serverSyncRevision

/** Keeps the current editing draft through Activity recreation without putting a sheet in a Bundle. */
internal class CharacterSheetDraftViewModel(initial: Character) : ViewModel() {
    val current = mutableStateOf(initial)
    private var observed = initial

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
}
