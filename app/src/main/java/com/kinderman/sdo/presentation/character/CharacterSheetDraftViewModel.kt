package com.kinderman.sdo.presentation.character

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.kinderman.sdo.domain.model.Character

/** Keeps the current editing draft through Activity recreation without putting a sheet in a Bundle. */
internal class CharacterSheetDraftViewModel(initial: Character) : ViewModel() {
    val current = mutableStateOf(initial)
}
