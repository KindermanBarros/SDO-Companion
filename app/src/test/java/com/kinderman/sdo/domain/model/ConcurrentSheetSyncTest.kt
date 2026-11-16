package com.kinderman.sdo.domain.model

import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.firestoreCharacterPatch
import com.kinderman.sdo.data.local.toRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConcurrentSheetSyncTest {
    private fun baseCommittedSheet(): Character {
        val initial = Character(
            id = "sheet-1",
            ownerId = "player-1",
            name = "Ayla",
            race = "Humano",
            occupation = "Batedora",
            age = "24",
            height = "1,68m",
            sex = "F",
            money = 50,
            life = ResourceValue(20, 20),
            sanity = ResourceValue(15, 15),
            inventory = listOf(
                InventoryItem(id = "item-1", name = "Adaga", quantity = 2, load = 1, state = InventoryState.STORED.name),
            ),
            attributes = listOf(
                AttributeValue(
                    name = "Força",
                    acronym = "FOR",
                    value = 3,
                    skills = listOf(
                        SkillValue(name = "Luta", value = 2),
                        SkillValue(name = "Atletismo", value = 1),
                    ),
                ),
            ),
            photoUri = "data:image/jpeg;base64,${"A".repeat(16_000)}",
            updatedAt = 100L,
            lastSyncedAt = 100L,
            lastEditedBy = "player-1",
        )
        return commitCharacterEditKeys(
            remote = Character(id = "sheet-1", updatedAt = 0L),
            local = initial,
            keys = characterEditKeys(Character(id = "sheet-1", updatedAt = 0L), initial),
        )
    }

    @Test
    fun concurrentIndependentFieldsPreserveBothEditsEvenBeyondFiveGroups() {
        val base = baseCommittedSheet()
        val gmRemote = commitCharacterEditKeys(
            remote = base,
            local = base.copy(life = ResourceValue(12, 20), lastEditedBy = "gm-1"),
            keys = setOf("life"),
        )
        val playerLocal = base.copy(
            name = "Ayla Vael",
            race = "Elfa",
            occupation = "Arqueira",
            age = "25",
            height = "1,70m",
            sex = "Feminino",
            money = 75,
            lastEditedBy = "player-1",
            dirty = true,
        ).withFieldEditTimes(base, at = 50L)

        val keys = pendingCharacterEditKeys(playerLocal, gmRemote)
        val effective = resolveConcurrentEditKeys(playerLocal, gmRemote, keys)
        val merged = commitCharacterEditKeys(gmRemote, playerLocal, effective)

        assertEquals(12, merged.life.current)
        assertEquals("Ayla Vael", merged.name)
        assertEquals("Elfa", merged.race)
        assertEquals("Arqueira", merged.occupation)
        assertEquals("25", merged.age)
        assertEquals("1,70m", merged.height)
        assertEquals("Feminino", merged.sex)
        assertEquals(75, merged.money)
    }

    @Test
    fun concurrentEditsToDifferentPropertiesOfSameInventoryItemPreserveBoth() {
        val base = baseCommittedSheet()
        val gmChangedQty = base.copy(
            inventory = base.inventory.map { it.copy(quantity = 5) },
            lastEditedBy = "gm-1",
        )
        val gmKeys = characterEditKeys(base, gmChangedQty)
        val gmRemote = commitCharacterEditKeys(base, gmChangedQty, gmKeys)

        // Player edited item name earlier (smaller timestamp), while GM edited quantity later.
        val playerEditedName = base.copy(
            inventory = base.inventory.map { it.copy(name = "Adaga Rúnica") },
            lastEditedBy = "player-1",
            dirty = true,
        ).withFieldEditTimes(base, at = base.updatedAt + 1L)

        val pending = pendingCharacterEditKeys(playerEditedName, gmRemote)
        val effective = resolveConcurrentEditKeys(playerEditedName, gmRemote, pending)
        val merged = commitCharacterEditKeys(gmRemote, playerEditedName, effective)

        assertEquals(1, merged.inventory.size)
        assertEquals("Adaga Rúnica", merged.inventory.first().name)
        assertEquals(5, merged.inventory.first().quantity)
    }

    @Test
    fun concurrentEditsToDifferentSkillsOfSameAttributePreserveBoth() {
        val base = baseCommittedSheet()
        val gmChangedLuta = base.copy(
            attributes = base.attributes.map { attr ->
                attr.copy(skills = attr.skills.map { if (it.name == "Luta") it.copy(value = 4) else it })
            },
            lastEditedBy = "gm-1",
        )
        val gmRemote = commitCharacterEditKeys(base, gmChangedLuta, characterEditKeys(base, gmChangedLuta))

        val playerChangedAtletismo = base.copy(
            attributes = base.attributes.map { attr ->
                attr.copy(skills = attr.skills.map { if (it.name == "Atletismo") it.copy(value = 3) else it })
            },
            lastEditedBy = "player-1",
            dirty = true,
        ).withFieldEditTimes(base, at = base.updatedAt + 1L)

        val pending = pendingCharacterEditKeys(playerChangedAtletismo, gmRemote)
        val effective = resolveConcurrentEditKeys(playerChangedAtletismo, gmRemote, pending)
        val merged = commitCharacterEditKeys(gmRemote, playerChangedAtletismo, effective)

        val skills = merged.attributes.first().skills.associateBy { it.name }
        assertEquals(4, skills.getValue("Luta").value)
        assertEquals(3, skills.getValue("Atletismo").value)
    }

    @Test
    fun sameFieldCollisionUsesLatestTimestampAndTieFavorsCommittedRemote() {
        val base = baseCommittedSheet()
        val gmRemote = commitCharacterEditKeys(
            remote = base,
            local = base.copy(name = "Nome Remoto", lastEditedBy = "gm-1"),
            keys = setOf("name"),
        )
        val gmNameTime = gmRemote.fieldUpdatedAt["name"] ?: gmRemote.updatedAt

        val olderLocal = base.copy(name = "Nome Antigo", lastEditedBy = "player-1", dirty = true)
            .withFieldEditTimes(base, at = gmNameTime - 10L)
            .let { it.copy(fieldUpdatedAt = it.fieldUpdatedAt + ("name" to gmNameTime - 10L) + ("@pending/name" to gmNameTime - 10L)) }
        assertTrue(resolveConcurrentEditKeys(olderLocal, gmRemote, setOf("name")).isEmpty())

        val tiedLocal = base.copy(name = "Nome Empate", lastEditedBy = "player-1", dirty = true)
            .let { it.copy(fieldUpdatedAt = it.fieldUpdatedAt + ("name" to gmNameTime) + ("@pending/name" to gmNameTime)) }
        assertTrue(resolveConcurrentEditKeys(tiedLocal, gmRemote, setOf("name")).isEmpty())

        val newerLocal = base.copy(name = "Nome Mais Novo", lastEditedBy = "player-1", dirty = true)
            .let { it.copy(fieldUpdatedAt = it.fieldUpdatedAt + ("name" to gmNameTime + 50L) + ("@pending/name" to gmNameTime + 50L)) }
        assertEquals(setOf("name"), resolveConcurrentEditKeys(newerLocal, gmRemote, setOf("name")))
    }

    @Test
    fun remoteDeletionWinsOverStaleLocalPropertyEdit() {
        val base = baseCommittedSheet()
        val gmDeletedItem = base.copy(inventory = emptyList(), lastEditedBy = "gm-1")
        val gmRemote = commitCharacterEditKeys(base, gmDeletedItem, characterEditKeys(base, gmDeletedItem))

        val playerEditedItemName = base.copy(
            inventory = base.inventory.map { it.copy(name = "Adaga Editada Offline") },
            lastEditedBy = "player-1",
            dirty = true,
        ).withFieldEditTimes(base, at = gmRemote.updatedAt + 500L)

        val pending = pendingCharacterEditKeys(playerEditedItemName, gmRemote)
        val effective = resolveConcurrentEditKeys(playerEditedItemName, gmRemote, pending)
        val merged = commitCharacterEditKeys(gmRemote, playerEditedItemName, effective)

        assertTrue(merged.inventory.isEmpty())
    }

    @Test
    fun offlineReplayPreservesBaseRevisionAcrossIntermediateRemoteAdoption() {
        val base = baseCommittedSheet()
        val offlineEdit = base.copy(
            inventory = base.inventory.map { it.copy(name = "Adaga Offline") },
            lastEditedBy = "player-1",
            dirty = true,
        ).withFieldEditTimes(base, at = base.updatedAt + 1L)

        val gmRemote1 = commitCharacterEditKeys(
            remote = base,
            local = base.copy(sanity = ResourceValue(10, 15), lastEditedBy = "gm-1"),
            keys = setOf("sanity"),
        )
        val rebasedLocally = preservePendingCharacterEdits(gmRemote1, offlineEdit)

        val gmRemote2 = commitCharacterEditKeys(
            remote = gmRemote1,
            local = gmRemote1.copy(
                inventory = gmRemote1.inventory.map { it.copy(quantity = 9) },
                lastEditedBy = "gm-1",
            ),
            keys = characterEditKeys(gmRemote1, gmRemote1.copy(inventory = gmRemote1.inventory.map { it.copy(quantity = 9) })),
        )

        val effective = resolveConcurrentEditKeys(rebasedLocally, gmRemote2, rebasedLocally.pendingSyncKeys())
        val finalMerged = commitCharacterEditKeys(gmRemote2, rebasedLocally, effective)

        assertEquals(10, finalMerged.sanity.current)
        assertEquals("Adaga Offline", finalMerged.inventory.first().name)
        assertEquals(9, finalMerged.inventory.first().quantity)
    }

    @Test
    fun concurrentAdditionsOfDifferentInventoryItemsPreserveBoth() {
        val base = baseCommittedSheet()
        val gmAdded = base.copy(
            inventory = base.inventory + InventoryItem(id = "item-gm", name = "Poção", quantity = 1),
            lastEditedBy = "gm-1",
        )
        val gmRemote = commitCharacterEditKeys(base, gmAdded, characterEditKeys(base, gmAdded))

        val playerAdded = base.copy(
            inventory = base.inventory + InventoryItem(id = "item-player", name = "Corda", quantity = 1),
            lastEditedBy = "player-1",
            dirty = true,
        ).withFieldEditTimes(base, at = base.updatedAt + 1L)

        val pending = pendingCharacterEditKeys(playerAdded, gmRemote)
        val effective = resolveConcurrentEditKeys(playerAdded, gmRemote, pending)
        val merged = commitCharacterEditKeys(gmRemote, playerAdded, effective)

        val ids = merged.inventory.map { it.id }.toSet()
        assertEquals(setOf("item-1", "item-gm", "item-player"), ids)
    }

    @Test
    fun firestoreCharacterPatchOmitsUntouchedHeavyFields() {
        val before = baseCommittedSheet().toRecord()
        val sentinelDelete = Any()
        val after = before.copy(
            life = ResourceValue(15, 20),
            updatedAt = before.updatedAt + 100L,
            lastEditedBy = "gm-1",
            fieldUpdatedAt = before.fieldUpdatedAt + ("@revision" to 2L) + ("life" to before.updatedAt + 100L),
        )

        val patch = firestoreCharacterPatch(
            before = before,
            after = after,
            existingDocumentKeys = setOf("dirty", "life", "photoUri"),
            deleteValue = sentinelDelete,
        )

        assertEquals(setOf("life", "updatedAt", "lastEditedBy", "fieldUpdatedAt", "dirty"), patch.keys)
        assertEquals(sentinelDelete, patch["dirty"])
        assertFalse("photoUri" in patch)
        assertFalse("inventory" in patch)
        assertFalse("attributes" in patch)
    }
}
