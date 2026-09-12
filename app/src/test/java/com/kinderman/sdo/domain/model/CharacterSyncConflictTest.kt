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
            setOf("name", "money", "abilities"),
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
            remoteFieldIds = setOf("name", "abilities"),
        )

        assertEquals("Nome online", merged.name)
        assertEquals("Raça local", merged.race)
        assertEquals(12, merged.money)
        assertEquals(listOf(remotePower), merged.powers)
    }

    @Test
    fun typedOnlyChangesAreConflictsAndRemoteSelectionKeepsTypedAuthority() {
        val local = Character(
            id = "character-typed",
            bodyState = BodyState(),
            conditionInstances = emptyList(),
            activeModifiers = emptyList(),
        )
        val remote = local.copy(
            bodyState = BodyState().withInjury(
                BodyRegionSlot.Torso,
                InjuryEvent(failuresAdded = 1, source = Source.narrative(NarrativeSourceId("test"))),
            ),
            conditionInstances = listOf(ConditionInstance(
                instanceId = ConditionInstanceId("condition-typed"),
                payload = ConditionPayload(kind = ConditionKind.Abalado, intensity = 2),
                source = Source.narrative(NarrativeSourceId("test")),
            )),
            activeModifiers = listOf(ActiveModifier(
                modifierId = "modifier-typed",
                target = BonusTarget.AttributeTarget(Attribute.FOR),
                amount = 1,
                source = Source.narrative(NarrativeSourceId("test")),
            )),
        )

        val fields = characterConflictFields(local, remote).mapTo(mutableSetOf()) { it.id }
        val merged = mergeCharacterConflict(local, remote, fields)

        assertTrue(setOf("bodyState", "conditionInstances", "activeModifiers").all(fields::contains))
        assertEquals(remote.bodyState, merged.bodyState)
        assertEquals(remote.conditionInstances, merged.conditionInstances)
        assertEquals(remote.activeModifiers, merged.activeModifiers)
        assertTrue(characterConflictFields(merged, remote).isEmpty())
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
