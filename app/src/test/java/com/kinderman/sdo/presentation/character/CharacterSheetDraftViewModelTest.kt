package com.kinderman.sdo.presentation.character

import com.kinderman.sdo.domain.model.Character
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterSheetDraftViewModelTest {
    @Test
    fun editingDraftRemainsOwnedByTheRetainedViewModel() {
        val initial = Character(name = "Antes")
        val retainedStore = ViewModelStore()
        val factory = viewModelFactory { initializer { CharacterSheetDraftViewModel(initial) } }
        val draft = ViewModelProvider(retainedStore, factory)[CharacterSheetDraftViewModel::class.java]
        draft.current.value = initial.copy(name = "Edição pendente")

        val recreatedOwner = ViewModelProvider(retainedStore, factory)
        val restored = recreatedOwner[CharacterSheetDraftViewModel::class.java]
        assertEquals("Edição pendente", restored.current.value.name)
        assertEquals(initial.id, restored.current.value.id)
        retainedStore.clear()
    }
}
