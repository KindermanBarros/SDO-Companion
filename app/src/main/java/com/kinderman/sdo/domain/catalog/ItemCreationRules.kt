package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.BuiltItem
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemAcquisitionSource
import com.kinderman.sdo.domain.model.ItemPart
import com.kinderman.sdo.domain.model.ItemQuality
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectCondition
import com.kinderman.sdo.domain.model.ItemEffectType
import kotlin.math.roundToInt
import java.util.Locale

object ItemCreationRules {
    const val HERITAGE_BUDGET = 30

    private fun structuredMaterials(weapon: Boolean): List<ItemPart> =
        GeneratedStructuredItemCatalog.materials.map { material ->
            val effect = if (weapon) material.weapon else material.armor
            ItemPart(
                id = material.id,
                name = material.name,
                group = when (material.tier) {
                    MaterialTier.COMMON -> "Material comum"
                    MaterialTier.UNCOMMON -> "Material incomum"
                    MaterialTier.RARE -> "Material raro"
                    MaterialTier.ANCESTRAL -> "Material ancestral"
                },
                creationCost = material.ph,
                price = material.money,
                durability = effect.durability,
                agilityLimit = effect.agilityLimit,
                materialTier = material.tier.name,
                characterCreationVisible = material.characterCreationVisible,
                traitIds = effect.traitIds,
                damageBonus = effect.damageBonus,
                damageReduction = effect.damageReduction,
                pg = effect.pg,
                pl = effect.pl,
                categoryDieShift = effect.categoryDieShift,
            )
        }

    private fun mergeMaterials(legacy: List<ItemPart>, structured: List<ItemPart>): List<ItemPart> =
        (legacy.filterNot { old -> structured.any { it.id == old.id } } + structured)
            .sortedWith(compareBy(ItemPart::materialTier, ItemPart::name))

    val weaponMaterials = mergeMaterials(CanonicalItemCatalog.weaponMaterials, structuredMaterials(weapon = true))
    val armorMaterials = mergeMaterials(CanonicalItemCatalog.armorMaterials, structuredMaterials(weapon = false))
    val weaponBases = CanonicalItemCatalog.weaponBases
    val armorBases = CanonicalItemCatalog.armorBases
    val weaponModifications = CanonicalItemCatalog.modifications
        .filter { "Arma" in it.compatibleItemTypes }.map { it.part.copy(group = "Modificação de arma") }
    val armorModifications = CanonicalItemCatalog.modifications
        .filter { definition -> definition.compatibleItemTypes.any { it in setOf("Armadura", "Acessório", "Escudo") } }
        .map { it.part.copy(group = "Modificação de armadura") }

    fun compatibleModifications(base: ItemPart, weapon: Boolean): List<ItemPart> =
        (if (weapon) weaponModifications else armorModifications)
            .filter { modification ->
                CanonicalItemCatalog.modifications.first { it.part.id == modification.id }.supports(base)
            }

    val gemComponents = CanonicalItemCatalog.gems.map { it.part }
    val technologyComponents = CanonicalItemCatalog.technologies.map { it.part }

    fun traitName(id: String): String = GeneratedStructuredItemCatalog.traits.firstOrNull { it.id == id }?.name ?: id

    fun traitDescription(id: String): String = GeneratedStructuredItemCatalog.traits.firstOrNull { it.id == id }?.description.orEmpty()

    fun qualitiesFor(initialCreation: Boolean): List<ItemQuality> =
        ItemQuality.entries.filterNot { initialCreation && it == ItemQuality.ANCIENT }

    fun materialsFor(materials: List<ItemPart>, initialCreation: Boolean): List<ItemPart> =
        materials.filter { material ->
            !initialCreation || (material.creationCost != null && material.characterCreationVisible && material.materialTier != MaterialTier.ANCESTRAL.name)
        }

    fun mixMaterials(primary: ItemPart, secondary: ItemPart?): ItemPart {
        if (secondary == null) return primary
        require(primary.id != secondary.id) { "Uma liga precisa de dois materiais diferentes." }
        val creationCost = listOf(primary.creationCost, secondary.creationCost)
            .takeUnless { costs -> costs.any { it == null } }
            ?.filterNotNull()?.sum()
        val tierOrder = listOf("", MaterialTier.COMMON.name, MaterialTier.UNCOMMON.name, MaterialTier.RARE.name, MaterialTier.ANCESTRAL.name)
        val tier = listOf(primary.materialTier, secondary.materialTier).maxBy { tierOrder.indexOf(it) }
        return primary.copy(
            id = "${primary.id}+${secondary.id}",
            name = "${primary.name} + ${secondary.name}",
            group = "Liga ${tier.lowercase(Locale.ROOT)}",
            creationCost = creationCost,
            price = primary.price + secondary.price,
            load = (primary.load + secondary.load + 1) / 2,
            durability = (primary.durability + secondary.durability + 1) / 2,
            agilityLimit = listOfNotNull(primary.agilityLimit, secondary.agilityLimit).minOrNull(),
            materialTier = tier,
            characterCreationVisible = primary.characterCreationVisible && secondary.characterCreationVisible && tier != MaterialTier.ANCESTRAL.name,
            traitIds = (primary.traitIds + secondary.traitIds).distinct(),
            damageBonus = (primary.damageBonus + secondary.damageBonus + 1) / 2,
            damageReduction = (primary.damageReduction + secondary.damageReduction + 1) / 2,
            pg = (primary.pg + secondary.pg + 1) / 2,
            pl = (primary.pl + secondary.pl + 1) / 2,
            categoryDieShift = (primary.categoryDieShift + secondary.categoryDieShift + 1) / 2,
        )
    }

    fun build(
        base: ItemPart,
        material: ItemPart,
        modifications: List<ItemPart>,
        gemSlots: Int,
        technologySlots: Int,
        customName: String = "",
        quality: ItemQuality = ItemQuality.COMMON,
        components: List<ItemPart> = emptyList(),
        technologies: List<ItemPart> = emptyList(),
        priceOverride: Int? = null,
        secondaryMaterial: ItemPart? = null,
    ): BuiltItem {
        val effectiveMaterial = mixMaterials(material, secondaryMaterial)
        val installedComponents = if (quality == ItemQuality.MUNDANE) emptyList() else components
        require(installedComponents.size <= gemSlots) { "Cada Gema selecionada precisa de um Espaço de Gema." }
        require(technologies.size <= technologySlots) { "Cada melhoria selecionada precisa de um Espaço de Tecnologia." }
        val effectiveGemSlots = if (quality == ItemQuality.MUNDANE) 0 else gemSlots
        val effectiveTechnologySlots = if (quality == ItemQuality.MUNDANE) 0 else technologySlots
        val numericCosts = listOf(base.creationCost, effectiveMaterial.creationCost) +
            modifications.map { it.creationCost } + installedComponents.map { it.creationCost } + technologies.map { it.creationCost }
        val componentCost = if (numericCosts.any { it == null }) null else
            numericCosts.filterNotNull().sum() + effectiveGemSlots + effectiveTechnologySlots * 2
        val creationCost = componentCost?.let { (it + quality.creationAdjustment).coerceAtLeast(0) }
        val armor = base.group == "Armadura" || base.group == "Escudo" || base.group == "Acessório"
        val protective = base.group == "Armadura" || base.group == "Escudo"
        val qualityPg = if (protective) when (quality) {
            ItemQuality.IMPROVED, ItemQuality.ICONIC -> 1
            ItemQuality.MASTERPIECE, ItemQuality.ARTIFACT, ItemQuality.ANCIENT -> 2
            else -> 0
        } else 0
        val qualityPl = if (armor) when (quality) {
            ItemQuality.ICONIC -> 1
            ItemQuality.MASTERPIECE, ItemQuality.ARTIFACT, ItemQuality.ANCIENT -> 2
            else -> 0
        } else 0
        val rawPg = base.pg + (if (protective) effectiveMaterial.pg else 0) + modifications.sumOf { it.pg } + qualityPg
        val rawPl = if (quality == ItemQuality.MUNDANE) 0 else base.pl + effectiveMaterial.pl + modifications.sumOf { it.pl } + qualityPl
        val pg = if (armor && effectiveMaterial.id == "sucata") rawPg / 2 else rawPg
        val pl = if (armor && effectiveMaterial.id == "sucata") rawPl / 2 else rawPl
        val baseAgilityLimit = if (base.id in setOf("jaqueta_revestida", "sapatilhas")) base.agilityLimit else
            listOfNotNull(base.agilityLimit, effectiveMaterial.agilityLimit).minOrNull()
        val agilityAdjustment = modifications.sumOf { modification -> when (modification.id) {
            "robusta" -> -1
            "ajustada" -> 1
            "sob_medida" -> 2
            else -> 0
        } }
        val details = buildList {
            add(base.effect)
            if (effectiveMaterial.effect.isNotBlank()) add(effectiveMaterial.effect)
            modifications.forEach { add(it.effect) }
            installedComponents.forEach { add(it.effect) }
            technologies.forEach { add(it.effect) }
            qualityEffect(quality, armor)?.let(::add)
        }.filter(String::isNotBlank).joinToString("\n")
        val componentPrice = base.price + effectiveMaterial.price + modifications.sumOf { it.price } +
            installedComponents.sumOf { it.price } + technologies.sumOf { it.price }
        val referencePrice = (componentPrice * quality.priceMultiplier).roundToInt().coerceAtLeast(0)
        return BuiltItem(
            name = customName.ifBlank { "${base.name} de ${effectiveMaterial.name}" },
            category = base.group,
            creationCost = creationCost,
            price = priceOverride?.coerceAtLeast(0) ?: referencePrice,
            load = (base.load + effectiveMaterial.load + modifications.sumOf { it.load }).coerceAtLeast(1),
            durability = effectiveMaterial.durability,
            region = base.region,
            effect = details,
            pg = pg,
            pl = pl,
            agilityLimit = baseAgilityLimit?.let { (it + agilityAdjustment).coerceAtLeast(0) },
            quality = quality,
            baseId = base.id,
            materialId = material.id,
            secondaryMaterialId = secondaryMaterial?.id.orEmpty(),
            modificationIds = modifications.map { it.id },
            gemIds = installedComponents.map { it.id },
            technologyIds = technologies.map { it.id },
            gemSlots = effectiveGemSlots,
            technologySlots = effectiveTechnologySlots,
            mechanicalEffects = (componentEffects(modifications.map(ItemPart::id), installedComponents.map(ItemPart::id), technologies.map(ItemPart::id)) +
                listOfNotNull(effectiveMaterial.damageBonus.takeIf { it != 0 }?.let { bonus ->
                    ItemEffect(
                        id = "material:${effectiveMaterial.id}:damage",
                        type = ItemEffectType.PHYSICAL_DAMAGE,
                        value = bonus,
                        condition = ItemEffectCondition.WIELDED,
                        description = "Bônus de dano concedido pelo material.",
                    )
                }, effectiveMaterial.damageReduction.takeIf { protective && it != 0 }?.let { reduction ->
                    ItemEffect(
                        id = "material:${effectiveMaterial.id}:reduction",
                        type = ItemEffectType.DAMAGE_REDUCTION,
                        value = reduction,
                        condition = ItemEffectCondition.EQUIPPED,
                        description = "Redução de dano concedida pelo material.",
                    )
                }, effectiveMaterial.categoryDieShift.takeIf { !armor && it != 0 }?.let { shift ->
                    ItemEffect(
                        id = "material:${effectiveMaterial.id}:die-category",
                        type = ItemEffectType.DIE_CATEGORY,
                        value = shift,
                        condition = ItemEffectCondition.WIELDED,
                        description = "Alteração da categoria de dado concedida pelo material.",
                    )
                }) +
                equipmentEffects(base.id, pg, pl, baseAgilityLimit?.let { (it + agilityAdjustment).coerceAtLeast(0) }, quality, armor, isShield = base.group == "Escudo"))
                .distinctBy(ItemEffect::id),
        )
    }

    fun complexity(cost: Int?): String = when (cost) {
        null -> "CD 25+; projeto definido pelo Historiador"
        in 0..2 -> "CD 9; 2 Progressos; 1 hora por teste"
        in 3..5 -> "CD 12; 3 Progressos; 2 horas por teste"
        in 6..8 -> "CD 15; 5 Progressos; 4 horas por teste"
        in 9..12 -> "CD 18; 7 Progressos; 1 dia por teste"
        in 13..17 -> "CD 20; 9 Progressos; 2 dias por teste"
        in 18..22 -> "CD 22; 12 Progressos; 1 semana por teste"
        else -> "CD 25; 15 Progressos; 2 semanas por teste"
    }

    fun standardPrice(cost: Int): Int = when (cost.coerceIn(0, 30)) {
        0 -> 5; 1 -> 20; 2 -> 45; 3 -> 90; 4 -> 150; 5 -> 250
        6 -> 400; 7 -> 600; 8 -> 850; 9 -> 1_250; 10 -> 1_700
        11 -> 2_200; 12 -> 2_800; 13 -> 3_500; 14 -> 4_200; 15 -> 5_000
        16 -> 6_000; 17 -> 7_000; 18 -> 8_200; 19 -> 9_500; 20 -> 11_000
        21 -> 13_000; 22 -> 15_500; 23 -> 18_000; 24 -> 21_500; 25 -> 25_000
        26 -> 30_000; 27 -> 36_000; 28 -> 43_000; 29 -> 51_000
        else -> 60_000
    }

    fun qualityEffect(quality: ItemQuality, armor: Boolean): String? = when (quality) {
        ItemQuality.MUNDANE -> if (armor) "Mundana: não concede PL nem possui espaços." else "Mundana: -1 Categoria de Dado e sem espaços."
        ItemQuality.COMMON -> null
        ItemQuality.IMPROVED -> if (armor) "Aprimorada: +1 PG." else "Aprimorada: +1 Ataque e +1 dano."
        ItemQuality.ICONIC -> if (armor) "Icônica: +1 PG, +1 PL e +1 espaço de melhoria." else "Icônica: +1 Ataque, +1 dano e +1 espaço de melhoria."
        ItemQuality.MASTERPIECE -> if (armor) "Obra-Prima: +2 PG, +2 PL e +2 espaços de melhoria." else "Obra-Prima: +2 Ataque, +2 dano e +2 espaços de melhoria."
        ItemQuality.ARTIFACT -> "Artefato: imune ao desgaste normal e possui um Poder de Item."
        ItemQuality.ANCIENT -> "Anciã: habilidade única; exige Maestria 7 para criação."
    }

    private data class CatalogBundle(
        val entries: List<CatalogEntry>,
        val inventoryTemplates: Map<String, BuiltItem>,
    )

    private val catalogBundle: CatalogBundle by lazy {
        val commonWeapon = weaponMaterials.first { it.id == "ligas_comuns" }
        val commonArmor = armorMaterials.first { it.id == "ligas_comuns" }
        val built = weaponBases.map { build(it, commonWeapon, emptyList(), 0, 0) } +
            armorBases.map { base ->
                val material = if (base.id == "gibao") armorMaterials.first { it.id == "organico" } else commonArmor
                build(base, material, emptyList(), 0, 0)
            }
        val regular = built.mapIndexed { index, item -> item.catalogEntry("item.regular_$index") }
        val starterItems = CanonicalItemCatalog.catalogItems.map { definition ->
            definition.part.toBuiltItem()
        }
        val starters = CanonicalItemCatalog.catalogItems.zip(starterItems).map { (definition, item) ->
            item.catalogEntry(definition.part.id, definition.source, definition.ruleReference)
        }
        val entries = (regular + starters).distinctBy { it.name }
        CatalogBundle(
            entries = entries,
            inventoryTemplates = (regular.zip(built) + starters.zip(starterItems)).associate { (entry, item) -> entry.id to item },
        )
    }

    val catalog: List<CatalogEntry> get() = catalogBundle.entries

    internal fun inventoryTemplate(catalogEntryId: String): BuiltItem? =
        catalogBundle.inventoryTemplates[catalogEntryId]

    internal fun componentEffects(modificationIds: List<String>, gemIds: List<String>, technologyIds: List<String> = emptyList()) =
        (CanonicalItemCatalog.modifications.filter { it.part.id in modificationIds }.map { it.effect } +
            CanonicalItemCatalog.gems.filter { it.part.id in gemIds }.map { it.effect } +
            CanonicalItemCatalog.technologies.filter { it.part.id in technologyIds }.map { it.effect })
            .distinctBy { it.id }

    private fun equipmentEffects(baseId: String, pg: Int, pl: Int, agilityLimit: Int?, quality: ItemQuality, armor: Boolean, isShield: Boolean = false) = buildList {
        if (pg != 0) {
            val condition = if (isShield) ItemEffectCondition.WIELDED else ItemEffectCondition.EQUIPPED
            add(ItemEffect("$baseId:pg", ItemEffectType.PG, pg, condition = condition, description = "Proteção geral do item."))
        }
        if (pl != 0) add(ItemEffect("$baseId:pl", ItemEffectType.PL, pl, condition = ItemEffectCondition.EQUIPPED, description = "Proteção local do item."))
        agilityLimit?.let { add(ItemEffect("$baseId:la", ItemEffectType.AGILITY_LIMIT, it, condition = ItemEffectCondition.EQUIPPED, description = "Limite de Agilidade do item.")) }
        if (!armor) {
            val bonus = when (quality) {
                ItemQuality.IMPROVED, ItemQuality.ICONIC -> 1
                ItemQuality.MASTERPIECE, ItemQuality.ARTIFACT, ItemQuality.ANCIENT -> 2
                else -> 0
            }
            if (bonus > 0) {
                add(ItemEffect("$baseId:quality:attack", ItemEffectType.ATTACK, bonus, condition = ItemEffectCondition.WIELDED, description = "Ataque concedido pela qualidade ${quality.label}."))
                add(ItemEffect("$baseId:quality:damage", ItemEffectType.PHYSICAL_DAMAGE, bonus, condition = ItemEffectCondition.WIELDED, description = "Dano concedido pela qualidade ${quality.label}."))
            }
        }
    }

    internal fun inventoryTemplate(catalogEntryId: String, name: String): Pair<CatalogEntry, BuiltItem>? {
        val entry = catalogBundle.entries.firstOrNull { it.id == catalogEntryId }
            ?: catalogBundle.entries.firstOrNull { it.name.normalizedItemName() == name.normalizedItemName() }
            ?: return null
        return entry to (catalogBundle.inventoryTemplates[entry.id] ?: return null)
    }

    private fun BuiltItem.catalogEntry(
        id: String,
        source: String = "Tabelas canônicas de equipamentos",
        ruleReference: String = "03 - Regras/Balanceamento de Criação e Equipamentos.md",
    ) = CatalogEntry(
        id = id, kind = CatalogKind.ITEM, name = name, group = category, summary = effect,
        source = source, creationCost = creationCost?.toString() ?: "#",
        price = price, load = load, durability = durability.takeIf { it > 0 }?.let { "$it/$it" }.orEmpty(), region = region,
        version = BuiltInCatalog.VERSION,
        ruleReference = ruleReference,
    )

    private fun ItemPart.toBuiltItem() = BuiltItem(
        name = name,
        category = group,
        creationCost = creationCost,
        price = price,
        load = load,
        backpackCapacity = backpackCapacity,
        durability = durability,
        region = region,
        effect = effect,
        pg = pg,
        pl = pl,
        agilityLimit = agilityLimit,
    )
}

private fun String.normalizedItemName(): String = trim().lowercase(Locale.ROOT)

fun CatalogEntry.toInventoryItem(initialCreation: Boolean = false): InventoryItem {
    val template = ItemCreationRules.inventoryTemplate(id)
    if (template != null) {
        return template.toInventoryItem(initialCreation).copy(
            catalogEntryId = id,
            catalogVersion = version,
            canonical = true,
        )
    }
    return InventoryItem(
        name = name,
        load = load,
        backpackCapacity = backpackCapacity,
        durabilityCurrent = durability.toDurabilityPair().first,
        durabilityMax = durability.toDurabilityPair().second,
        region = region,
        effect = listOfNotNull(
            "Categoria: $group".takeIf { group.isNotBlank() },
            summary.takeIf(String::isNotBlank),
        ).joinToString("\n"),
        category = group,
        acquisitionSource = if (initialCreation) ItemAcquisitionSource.HERITAGE else ItemAcquisitionSource.NARRATIVE,
        heritageCost = creationCost.toIntOrNull().takeIf { initialCreation },
        purchasePrice = null,
        catalogEntryId = id,
        catalogVersion = version,
        canonical = true,
        dataVersion = com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION,
    )
}

private fun String.toDurabilityPair(): Pair<Int, Int> {
    val values = split('/')
    val maximum = values.getOrNull(1)?.toIntOrNull() ?: values.firstOrNull()?.toIntOrNull() ?: 1
    return (values.firstOrNull()?.toIntOrNull() ?: maximum).coerceIn(0, maximum) to maximum
}
