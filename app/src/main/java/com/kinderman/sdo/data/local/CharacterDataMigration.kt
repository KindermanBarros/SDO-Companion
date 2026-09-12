package com.kinderman.sdo.data.local

import com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION
import com.kinderman.sdo.domain.model.CANONICAL_SCHEMA_VERSION
import com.kinderman.sdo.domain.model.NeedsReview
import com.kinderman.sdo.domain.model.AuditableChoice
import com.kinderman.sdo.domain.model.CatalogEntryId
import com.kinderman.sdo.domain.model.CatalogReference
import com.kinderman.sdo.domain.model.CatalogScope
import com.kinderman.sdo.domain.model.CharacterProgression
import com.kinderman.sdo.domain.model.ChoiceKind
import com.kinderman.sdo.domain.model.ConsumableState
import com.kinderman.sdo.domain.model.EntityId
import com.kinderman.sdo.domain.model.ItemCatalogId
import com.kinderman.sdo.domain.model.ItemInstanceId
import com.kinderman.sdo.domain.model.ItemState
import com.kinderman.sdo.domain.model.NarrativeSourceId
import com.kinderman.sdo.domain.model.ScopedItemDefinition
import com.kinderman.sdo.domain.model.SourceRef
import com.kinderman.sdo.domain.model.StackState

const val CURRENT_CREATION_RULES_VERSION = 2

/** Content-aware migration shared by Room reads and Firestore synchronization. */
fun CharacterRecord.requiresStructuredMigration(): Boolean =
    canonicalSchemaVersion < CANONICAL_SCHEMA_VERSION ||
        canonicalAbilitiesPayload.isBlank() ||
        canonicalBodyPayload.isBlank() ||
        canonicalConditionsPayload.isBlank() ||
        activeModifiersPayload.isBlank() ||
        canonicalItemsPayload.isBlank() ||
        scopedItemCatalogPayload.isBlank() ||
        canonicalProgressionPayload.isBlank() ||
        itemSchemaVersion < CURRENT_ITEM_DATA_VERSION ||
        creationRulesVersion < CURRENT_CREATION_RULES_VERSION ||
        inventory.any { it.dataVersion < CURRENT_ITEM_DATA_VERSION }

fun CharacterRecord.migratedStructuredRecord(markDirty: Boolean): CharacterRecord {
    if (canonicalSchemaVersion > CANONICAL_SCHEMA_VERSION) {
        throw com.kinderman.sdo.domain.model.DomainError.UnsupportedSchemaVersion(
            "A ficha usa schema canônico $canonicalSchemaVersion; este app suporta até $CANONICAL_SCHEMA_VERSION.",
        )
    }
    if (!requiresStructuredMigration()) return this
    val migrationSource = if (itemSchemaVersion < CURRENT_ITEM_DATA_VERSION) {
        copy(inventory = inventory.map { item -> item.copy(dataVersion = minOf(item.dataVersion, itemSchemaVersion)) })
    } else this
    val domain = migrationSource.toDomain()
    val (itemStates, scopedCatalog) = migrationSource.canonicalInventory()
    val reviews = (domain.migrationReviews + migrationSource.legacyReviewQueue()).distinct()
    return domain.copy(
        canonicalSchemaVersion = CANONICAL_SCHEMA_VERSION,
        migrationReviews = reviews,
        itemSchemaVersion = CURRENT_ITEM_DATA_VERSION,
        creationRulesVersion = maxOf(creationRulesVersion, CURRENT_CREATION_RULES_VERSION),
        itemStates = itemStates,
        customItemCatalog = scopedCatalog,
        progression = migrationSource.canonicalProgression(),
    ).toRecord().copy(
        canonicalSchemaVersion = CANONICAL_SCHEMA_VERSION,
        itemSchemaVersion = CURRENT_ITEM_DATA_VERSION,
        creationRulesVersion = maxOf(creationRulesVersion, CURRENT_CREATION_RULES_VERSION),
        dirty = dirty || markDirty,
        lastSyncedAt = lastSyncedAt,
    )
}

private fun CharacterRecord.canonicalInventory(): Pair<List<ItemState>, List<ScopedItemDefinition>> {
    val converters = CharacterConverters()
    if (canonicalItemsPayload.isNotBlank() && scopedItemCatalogPayload.isNotBlank() &&
        converters.isCanonicalItemsPayloadValid(canonicalItemsPayload) &&
        converters.isScopedItemCatalogPayloadValid(scopedItemCatalogPayload)) {
        return converters.stringToCanonicalItems(canonicalItemsPayload) to
            converters.stringToScopedItemCatalog(scopedItemCatalogPayload)
    }
    val definitions = mutableListOf<ScopedItemDefinition>()
    val states = inventory.map { item ->
        val custom = item.catalogEntryId.isBlank()
        val definitionId = if (custom) "character:$id:${item.id}" else item.catalogEntryId
        val reference = CatalogReference(ItemCatalogId(definitionId), item.catalogVersion.coerceAtLeast(1))
        if (custom) definitions += ScopedItemDefinition(
            reference = reference,
            scope = CatalogScope.Character,
            ownerScopeId = id,
            name = item.name.ifBlank { "Item sem nome" },
            description = item.effect,
        )
        ItemState(
            id = ItemInstanceId(item.id),
            definition = reference,
            scope = if (custom) CatalogScope.Character else CatalogScope.Canonical,
            stack = StackState(item.quantity.coerceAtLeast(0)),
            consumable = item.linkedAshId.takeIf(String::isNotBlank)?.let {
                ConsumableState(doses = item.quantity.coerceAtLeast(0))
            },
        )
    }
    return states to definitions.distinctBy { it.reference }
}

private fun CharacterRecord.canonicalProgression(): CharacterProgression {
    if (canonicalProgressionPayload.isNotBlank() && CharacterConverters().isProgressionPayloadValid(canonicalProgressionPayload)) {
        return CharacterConverters().stringToCanonicalProgression(canonicalProgressionPayload)
    }
    return CharacterProgression(progressionHistory.map { record ->
        AuditableChoice(
            id = EntityId("progression:${record.id}"),
            kind = ChoiceKind.PROGRESSION,
            option = CatalogReference(CatalogEntryId("progression:${record.previousLevel}-${record.newLevel}"), 1),
            source = SourceRef.Narrative(NarrativeSourceId("legacy-progression:${record.id}")),
            grantedEntityIds = record.rewards.mapIndexed { index, reward ->
                EntityId("progression:${record.id}:$index:${reward.type}:${reward.targetId}:${reward.catalogEntryId}")
            },
            chosenAt = record.appliedAt,
        )
    }.distinctBy { it.id })
}

/** Collects ambiguous legacy mechanics verbatim. It never parses prose or invents references. */
private fun CharacterRecord.legacyReviewQueue(): List<NeedsReview> = buildList {
    val converters = CharacterConverters()
    listOf(
        "canonicalAbilitiesPayload" to converters.isCanonicalAbilitiesPayloadValid(canonicalAbilitiesPayload),
        "canonicalBodyPayload" to converters.isCanonicalBodyPayloadValid(canonicalBodyPayload),
        "canonicalConditionsPayload" to converters.isCanonicalConditionsPayloadValid(canonicalConditionsPayload),
        "activeModifiersPayload" to converters.isActiveModifiersPayloadValid(activeModifiersPayload),
        "canonicalItemsPayload" to converters.isCanonicalItemsPayloadValid(canonicalItemsPayload),
        "scopedItemCatalogPayload" to converters.isScopedItemCatalogPayloadValid(scopedItemCatalogPayload),
        "canonicalProgressionPayload" to converters.isProgressionPayloadValid(canonicalProgressionPayload),
    ).filterNot { it.second }.forEach { (field, _) ->
        val value = when (field) {
            "canonicalAbilitiesPayload" -> canonicalAbilitiesPayload
            "canonicalBodyPayload" -> canonicalBodyPayload
            "canonicalConditionsPayload" -> canonicalConditionsPayload
            "activeModifiersPayload" -> activeModifiersPayload
            "canonicalItemsPayload" -> canonicalItemsPayload
            "scopedItemCatalogPayload" -> scopedItemCatalogPayload
            else -> canonicalProgressionPayload
        }
        add(NeedsReview(field, value, "Payload canônico inválido; revisar o registro estruturado sem inferência textual."))
    }
    powers.forEach { power ->
        listOf(
            "power.effect" to power.effect,
            "power.limit" to power.limit,
            "power.activationCondition" to power.activationCondition,
            "power.deactivationCondition" to power.deactivationCondition,
        ).filter { it.second.isNotBlank() }.forEach { (field, value) ->
            add(NeedsReview("$field:${power.id}", value, "Converter para operação/referência tipada."))
        }
        val missingReference = when (power.canonicalSource) {
            com.kinderman.sdo.domain.model.AbilitySource.KNOWLEDGE -> power.knowledgeId.isBlank() || power.knowledgeLevel == null
            com.kinderman.sdo.domain.model.AbilitySource.ITEM -> power.linkedItemId.isBlank()
            else -> power.sourceId.isBlank()
        }
        if (missingReference) add(NeedsReview(
            "power.sourceRef:${power.id}",
            power.origin,
            "Selecionar SourceRef tipado; nomes e descrições não são convertidos automaticamente.",
        ))
    }
    mysticAbilities.forEach { ability ->
        if (ability.effect.isNotBlank()) add(NeedsReview("ability.effect:${ability.id}", ability.effect, "Revisar operações executáveis sem interpretar a descrição."))
        val missingReference = !ability.type.equals("Cinza", true) && when (ability.canonicalSource) {
            com.kinderman.sdo.domain.model.AbilitySource.KNOWLEDGE -> ability.knowledgeId.isBlank() || ability.knowledgeLevel == null
            com.kinderman.sdo.domain.model.AbilitySource.ITEM -> ability.linkedInventoryItemId.isBlank()
            else -> ability.source.isBlank()
        }
        if (missingReference) add(NeedsReview(
            "ability.sourceRef:${ability.id}",
            ability.source,
            "Selecionar SourceRef tipado; a migração não inventa referências ausentes.",
        ))
    }
    conditions.forEach { condition ->
        if (com.kinderman.sdo.domain.model.ConditionKind.fromName(condition.name) == null) add(NeedsReview(
            "condition.kind:${condition.id}", condition.name, "Selecionar ConditionKind tipado.",
        ))
        if (condition.intensity.isNotBlank()) add(NeedsReview("condition.intensity:${condition.id}", condition.intensity, "Selecionar intensidade tipada."))
        if (condition.duration.isNotBlank()) add(NeedsReview("condition.duration:${condition.id}", condition.duration, "Selecionar duração tipada."))
    }
    bodyRegions.forEach { region ->
        if (com.kinderman.sdo.domain.model.BodyRegionSlot.fromName(region.name) == null) add(NeedsReview(
            "body.region:${region.roll}", region.name, "Selecionar BodyRegionSlot tipado; a migração não infere regiões por texto.",
        ))
        if (region.damage.isNotBlank()) add(NeedsReview("body.damage:${region.name}", region.damage, "Registrar InjuryEvent tipado."))
        if (region.implants.isNotBlank()) add(NeedsReview("body.implants:${region.name}", region.implants, "Vincular ItemInstanceId de implante."))
    }
    organs.forEach { organ ->
        if (organ.implant.isNotBlank()) add(NeedsReview("organ.implant:${organ.id}", organ.implant, "Vincular ItemInstanceId de implante."))
        if (organ.effect.isNotBlank()) add(NeedsReview("organ.effect:${organ.id}", organ.effect, "Converter para condição ou modificador tipado."))
    }
    if (creationCompletedAt != null && canonicalProgressionPayload.isBlank()) {
        listOf("race" to race, "occupation" to occupation, "path" to pathName)
            .filter { it.second.isNotBlank() }
            .forEach { (kind, value) -> add(NeedsReview(
                "creation.choice:$kind", value,
                "Selecionar a revisão de catálogo para registrar esta escolha de criação sem inferência por nome.",
            )) }
    }
    (learnedKnowledges + arcaneKnowledges + battleTechniques).filter {
        it.category.equals("Especialização", true) && it.specializationParentId.isBlank()
    }.forEach { knowledge -> add(NeedsReview(
        "knowledge.parent:${knowledge.id}", knowledge.name,
        "Selecionar o parentKnowledgeId tipado obrigatório da especialização.",
    )) }
    inventory.filter { !it.canonical && it.catalogEntryId.isBlank() }.forEach { item ->
        // A deterministic character-scoped entry is safe; only unresolved mechanics require review.
        if (item.mechanicalEffects.isNotEmpty() || item.effect.isNotBlank()) add(NeedsReview(
            "inventory.mechanics:${item.id}", item.effect,
            "Vincular operações estruturadas à entrada de catálogo criada para o personagem.",
        ))
    }
    inventory.forEach { item ->
        if (item.materialId.isNotBlank() || item.modificationIds.isNotEmpty() || item.gemIds.isNotEmpty()) add(NeedsReview(
            "inventory.installations:${item.id}",
            (listOf(item.materialId) + item.modificationIds + item.gemIds).filter(String::isNotBlank).joinToString(","),
            "Selecionar revisões e InstallationId para material, modificações e gemas.",
        ))
    }
}
