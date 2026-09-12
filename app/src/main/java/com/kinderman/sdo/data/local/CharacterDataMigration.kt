package com.kinderman.sdo.data.local

import com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION
import com.kinderman.sdo.domain.model.CANONICAL_SCHEMA_VERSION
import com.kinderman.sdo.domain.model.NeedsReview

const val CURRENT_CREATION_RULES_VERSION = 2

/** Content-aware migration shared by Room reads and Firestore synchronization. */
fun CharacterRecord.requiresStructuredMigration(): Boolean =
    canonicalSchemaVersion < CANONICAL_SCHEMA_VERSION ||
        itemSchemaVersion < CURRENT_ITEM_DATA_VERSION ||
        creationRulesVersion < CURRENT_CREATION_RULES_VERSION ||
        inventory.any { it.dataVersion < CURRENT_ITEM_DATA_VERSION }

fun CharacterRecord.migratedStructuredRecord(markDirty: Boolean): CharacterRecord {
    if (!requiresStructuredMigration()) return this
    val migrationSource = if (itemSchemaVersion < CURRENT_ITEM_DATA_VERSION) {
        copy(inventory = inventory.map { item -> item.copy(dataVersion = minOf(item.dataVersion, itemSchemaVersion)) })
    } else this
    val reviews = (migrationSource.toDomain().migrationReviews + migrationSource.legacyReviewQueue()).distinct()
    return migrationSource.toDomain().copy(
        canonicalSchemaVersion = CANONICAL_SCHEMA_VERSION,
        migrationReviews = reviews,
        itemSchemaVersion = CURRENT_ITEM_DATA_VERSION,
        creationRulesVersion = maxOf(creationRulesVersion, CURRENT_CREATION_RULES_VERSION),
    ).toRecord().copy(
        canonicalSchemaVersion = CANONICAL_SCHEMA_VERSION,
        itemSchemaVersion = CURRENT_ITEM_DATA_VERSION,
        creationRulesVersion = maxOf(creationRulesVersion, CURRENT_CREATION_RULES_VERSION),
        dirty = dirty || markDirty,
        lastSyncedAt = lastSyncedAt,
    )
}

/** Collects ambiguous legacy mechanics verbatim. It never parses prose or invents references. */
private fun CharacterRecord.legacyReviewQueue(): List<NeedsReview> = buildList {
    powers.forEach { power ->
        listOf(
            "power.effect" to power.effect,
            "power.limit" to power.limit,
            "power.activationCondition" to power.activationCondition,
            "power.deactivationCondition" to power.deactivationCondition,
        ).filter { it.second.isNotBlank() }.forEach { (field, value) ->
            add(NeedsReview("$field:${power.id}", value, "Converter para operação/referência tipada."))
        }
    }
    mysticAbilities.filter { it.effect.isNotBlank() }.forEach { ability ->
        add(NeedsReview("ability.effect:${ability.id}", ability.effect, "Revisar operações executáveis sem interpretar a descrição."))
    }
    conditions.forEach { condition ->
        if (condition.intensity.isNotBlank()) add(NeedsReview("condition.intensity:${condition.id}", condition.intensity, "Selecionar intensidade tipada."))
        if (condition.duration.isNotBlank()) add(NeedsReview("condition.duration:${condition.id}", condition.duration, "Selecionar duração tipada."))
    }
    bodyRegions.forEach { region ->
        if (region.damage.isNotBlank()) add(NeedsReview("body.damage:${region.name}", region.damage, "Registrar InjuryEvent tipado."))
        if (region.implants.isNotBlank()) add(NeedsReview("body.implants:${region.name}", region.implants, "Vincular ItemInstanceId de implante."))
    }
    organs.forEach { organ ->
        if (organ.implant.isNotBlank()) add(NeedsReview("organ.implant:${organ.id}", organ.implant, "Vincular ItemInstanceId de implante."))
        if (organ.effect.isNotBlank()) add(NeedsReview("organ.effect:${organ.id}", organ.effect, "Converter para condição ou modificador tipado."))
    }
    inventory.filter { !it.canonical && it.catalogEntryId.isBlank() }.forEach { item ->
        add(NeedsReview("inventory.catalog:${item.id}", item.name, "Criar entrada de catálogo de personagem ou campanha."))
    }
}
