package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterSyncConflictTest {
    @Test
    fun listsOnlyFieldsWhoseContentChanged() {
        val local = Character(
            id = "character-1",
            name = "Versão local",
            money = 12,
            powers = listOf(Power(name = "Pulso local")),
        )
        val remote = local.copy(
            name = "Versão online",
            money = 30,
            powers = listOf(Power(name = "Pulso online")),
            updatedAt = local.updatedAt + 1_000,
            dirty = false,
        )

        assertEquals(
            setOf("name", "money", "powers"),
            characterConflictFields(local, remote).mapTo(mutableSetOf()) { it.id },
        )
    }

    @Test
    fun mergesEachFieldUsingThePlayersChoice() {
        val localPower = Power(name = "Poder local")
        val remotePower = Power(name = "Poder online")
        val local = Character(
            id = "character-1",
            name = "Nome local",
            race = "Raça local",
            money = 12,
            powers = listOf(localPower),
        )
        val remote = local.copy(
            name = "Nome online",
            race = "Raça online",
            money = 30,
            powers = listOf(remotePower),
        )

        val merged = mergeCharacterConflict(
            local = local,
            remote = remote,
            remoteFieldIds = setOf("name", "powers"),
        )

        assertEquals("Nome online", merged.name)
        assertEquals("Raça local", merged.race)
        assertEquals(12, merged.money)
        assertEquals(listOf(remotePower), merged.powers)
    }

    @Test
    fun choosingEveryOnlineFieldProducesTheRemoteContent() {
        val local = Character(
            id = "character-1",
            name = "Local",
            story = "História local",
            personalNotes = listOf(PersonalNote(title = "Nota local")),
        )
        val remote = local.copy(
            name = "Online",
            story = "História online",
            personalNotes = listOf(PersonalNote(title = "Nota online")),
            updatedAt = local.updatedAt + 1_000,
            dirty = false,
        )
        val allRemote = characterConflictFields(local, remote).mapTo(mutableSetOf()) { it.id }

        val merged = mergeCharacterConflict(local, remote, allRemote)

        assertTrue(characterConflictFields(merged, remote).isEmpty())
    }
}
