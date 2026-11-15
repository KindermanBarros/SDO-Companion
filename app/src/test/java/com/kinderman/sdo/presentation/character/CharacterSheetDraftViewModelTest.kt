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
    fun remoteResourceChangeDoesNotErasePendingNote() {
        val note = com.kinderman.sdo.domain.model.PersonalNote(id = "note", text = "Before")
        val initial = Character(personalNotes = listOf(note))
        val draft = CharacterSheetDraftViewModel(initial)
        draft.current.value = initial.copy(personalNotes = listOf(note.copy(text = "Typing")))
        val remote = initial.copy(life = initial.life.copy(current = 7), fieldUpdatedAt = mapOf("@revision" to 2))

        draft.accept(remote)

        assertEquals("Typing", draft.current.value.personalNotes.single().text)
        assertEquals(7, draft.current.value.life.current)
    }

    @Test
    fun acknowledgedEditDoesNotMaskLaterRemoteChange() {
        val initial = Character(name = "Before")
        val draft = CharacterSheetDraftViewModel(initial)
        val edited = initial.copy(name = "Edited", fieldUpdatedAt = mapOf("@revision" to 1))
        draft.current.value = edited
        draft.accept(edited)
        draft.accept(edited.copy(name = "Remote", fieldUpdatedAt = mapOf("@revision" to 2)))

        assertEquals("Remote", draft.current.value.name)
    }

    @Test
    fun olderConfirmedRevisionCannotRollBackDraft() {
        val initial = Character(name = "Current", fieldUpdatedAt = mapOf("@revision" to 3))
        val draft = CharacterSheetDraftViewModel(initial)
        draft.accept(initial.copy(name = "Old", fieldUpdatedAt = mapOf("@revision" to 2)))
        assertEquals("Current", draft.current.value.name)
    }

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
