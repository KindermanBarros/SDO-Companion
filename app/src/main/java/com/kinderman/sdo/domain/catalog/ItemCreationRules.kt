package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.BuiltItem
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.ItemPart
import com.kinderman.sdo.domain.model.ItemQuality
import kotlin.math.roundToInt

object ItemCreationRules {
    const val HERITAGE_BUDGET = 30

    val weaponMaterials = GeneratedItemParts.weaponMaterials
    val armorMaterials = GeneratedItemParts.armorMaterials
    val weaponBases = GeneratedItemParts.weaponBases
    val armorBases = GeneratedItemParts.armorBases
    val weaponModifications = GeneratedItemParts.weaponModifications
    val armorModifications = GeneratedItemParts.armorModifications

    fun compatibleModifications(base: ItemPart, weapon: Boolean): List<ItemPart> =
        (if (weapon) weaponModifications else armorModifications)
            .filter { GeneratedItemParts.modificationSupports(it.id, base) }

    val gemComponents = GeneratedGemCatalog.entries.map { it.part }

    fun build(
        base: ItemPart,
        material: ItemPart,
        modifications: List<ItemPart>,
        gemSlots: Int,
        technologySlots: Int,
        customName: String = "",
        quality: ItemQuality = ItemQuality.COMMON,
        components: List<ItemPart> = emptyList(),
        priceOverride: Int? = null,
    ): BuiltItem {
        val installedComponents = if (quality == ItemQuality.MUNDANE) emptyList() else components
        require(installedComponents.size <= gemSlots) { "Cada Gema selecionada precisa de um Espaço de Gema." }
        val effectiveGemSlots = if (quality == ItemQuality.MUNDANE) 0 else gemSlots
        val effectiveTechnologySlots = if (quality == ItemQuality.MUNDANE) 0 else technologySlots
        val numericCosts = listOf(base.creationCost, material.creationCost) +
            modifications.map { it.creationCost } + installedComponents.map { it.creationCost }
        val componentCost = if (numericCosts.any { it == null }) null else
            numericCosts.filterNotNull().sum() + effectiveGemSlots + effectiveTechnologySlots * 2
        val creationCost = componentCost?.let { (it + quality.creationAdjustment).coerceAtLeast(0) }
        val armor = base.group == "Armadura" || base.group == "Escudo" || base.group == "Acessório"
        val qualityPg = if (armor) when (quality) {
            ItemQuality.IMPROVED, ItemQuality.ICONIC -> 1
            ItemQuality.MASTERPIECE, ItemQuality.ARTIFACT, ItemQuality.ANCIENT -> 2
            else -> 0
        } else 0
        val qualityPl = if (armor) when (quality) {
            ItemQuality.ICONIC -> 1
            ItemQuality.MASTERPIECE, ItemQuality.ARTIFACT, ItemQuality.ANCIENT -> 2
            else -> 0
        } else 0
        val rawPg = base.pg + material.pg + modifications.sumOf { it.pg } + qualityPg
        val rawPl = if (quality == ItemQuality.MUNDANE) 0 else base.pl + material.pl + modifications.sumOf { it.pl } + qualityPl
        val pg = if (armor && material.id == "sucata") rawPg / 2 else rawPg
        val pl = if (armor && material.id == "sucata") rawPl / 2 else rawPl
        val baseAgilityLimit = if (base.id in setOf("jaqueta_revestida", "sapatilhas")) base.agilityLimit else
            listOfNotNull(base.agilityLimit, material.agilityLimit).minOrNull()
        val agilityAdjustment = modifications.sumOf { modification -> when (modification.id) {
            "robusta" -> -1
            "ajustada" -> 1
            "sob_medida" -> 2
            else -> 0
        } }
        val details = buildList {
            add("Material predominante: ${material.name}.")
            add("Partes e camadas seguem o molde de ${base.name}; ligas da mesma família usam um único material predominante.")
            add("Qualidade: ${quality.label}.")
            if (pg > 0 || pl > 0) add("PG $pg; PL $pl.")
            add(base.effect)
            if (material.effect.isNotBlank()) add(material.effect)
            modifications.forEach { add("${it.name}: ${it.effect}") }
            installedComponents.forEach { add("${it.name}: ${it.effect}") }
            if (effectiveGemSlots > 0) add("Espaços de Gema: $effectiveGemSlots.")
            if (effectiveTechnologySlots > 0) add("Espaços de Tecnologia: $effectiveTechnologySlots.")
            qualityEffect(quality, armor)?.let(::add)
            add("Complexidade: ${complexity(creationCost)}.")
            if (creationCost == null) add("Custo da Criação: #; exige permissão do Historiador.")
        }.filter(String::isNotBlank).joinToString("\n")
        val referencePrice = componentCost?.let(::standardPrice)?.let { (it * quality.priceMultiplier).roundToInt() } ?: 0
        return BuiltItem(
            name = customName.ifBlank { "${base.name} de ${material.name}" },
            category = base.group,
            creationCost = creationCost,
            price = priceOverride?.coerceAtLeast(0) ?: referencePrice,
            load = (base.load + material.load + modifications.sumOf { it.load }).coerceAtLeast(1),
            durability = material.durability,
            region = base.region,
            effect = details,
            pg = pg,
            pl = pl,
            agilityLimit = baseAgilityLimit?.let { (it + agilityAdjustment).coerceAtLeast(0) },
            quality = quality,
            baseId = base.id,
            materialId = material.id,
            modificationIds = modifications.map { it.id },
            gemIds = installedComponents.map { it.id },
            mechanicalEffects = installedComponents.mapNotNull { GeneratedGemCatalog.effect(it.id) },
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

    private fun qualityEffect(quality: ItemQuality, armor: Boolean): String? = when (quality) {
        ItemQuality.MUNDANE -> if (armor) "Mundana: não concede PL nem possui espaços." else "Mundana: -1 Categoria de Dado e sem espaços."
        ItemQuality.COMMON -> null
        ItemQuality.IMPROVED -> if (armor) "Aprimorada: +1 PG." else "Aprimorada: +1 Ataque e +1 dano."
        ItemQuality.ICONIC -> if (armor) "Icônica: +1 PG, +1 PL e +1 espaço de melhoria." else "Icônica: +1 Ataque, +1 dano e +1 espaço de melhoria."
        ItemQuality.MASTERPIECE -> if (armor) "Obra-Prima: +2 PG, +2 PL e +2 espaços de melhoria." else "Obra-Prima: +2 Ataque, +2 dano e +2 espaços de melhoria."
        ItemQuality.ARTIFACT -> "Artefato: imune ao desgaste normal e possui um Poder de Item."
        ItemQuality.ANCIENT -> "Anciã: habilidade única; exige Maestria 7 para criação."
    }

    val catalog: List<CatalogEntry> by lazy {
        val commonWeapon = weaponMaterials.first { it.id == "ligas_comuns" }
        val commonArmor = armorMaterials.first { it.id == "ligas_comuns" }
        val built = weaponBases.map { build(it, commonWeapon, emptyList(), 0, 0) } +
            armorBases.map { base ->
                val material = if (base.id == "gibao") armorMaterials.first { it.id == "organico" } else commonArmor
                build(base, material, emptyList(), 0, 0)
            }
        val regular = built.mapIndexed { index, item -> item.catalogEntry("item.regular_$index") }
        val starters = listOf(
            build(armorBases.first { it.id == "jaqueta_revestida" }, armorMaterials.first { it.id == "aco_negro" }, emptyList(), 0, 0).catalogEntry("item.jaqueta_aco_negro"),
            build(weaponBases.first { it.id == "arco_curto" }, weaponMaterials.first { it.id == "madeira" }, emptyList(), 0, 0).catalogEntry("item.arco_curto_madeira"),
            simple("item.mochila_viajante", "Mochila de Viajante", "Recipiente", 2, 45, 1, "", "Capacidade de Carga +10."),
            simple("item.kit_sutura", "Kit de Sutura", "Consumível", 3, 100, 1, "", "Ferramentas para sutura e tratamento de campo."),
            simple("item.granada_fumaca", "Granada de Fumaça", "Consumível", 2, 45, 1, "", "Cria uma área de fumaça que bloqueia visão."),
            simple("item.morfina", "Morfina", "Consumível", 2, 45, 1, "", "Analgésico de uso médico."),
            simple("item.alcool", "Álcool", "Consumível", 1, 20, 1, "", "Dose de bebida alcoólica."),
            simple("item.tabaco", "Tabaco", "Consumível", 1, 20, 1, "", "Porção de tabaco."),
            simple("item.rede", "Rede", "Consumível", 2, 45, 1, "", "Imobiliza ou restringe um alvo conforme o ataque."),
            simple("item.cogumelo_dourado", "Cogumelo Dourado", "Consumível", 3, 90, 1, "", "Reagente medicinal raro."),
            simple("item.sussurros", "Sussurros", "Consumível", 3, 90, 1, "", "Composto de efeito mental."),
            simple("item.granada_veneno", "Granada de Veneno", "Consumível", 3, 90, 1, "", "Cria uma área venenosa."),
            simple("item.granada_incendiaria", "Granada Incendiária", "Consumível", 3, 90, 1, "", "Explode e aplica fogo."),
            simple("item.granada_sucata", "Granada de Sucata", "Consumível", 3, 90, 1, "", "Explode em estilhaços improvisados."),
            simple("item.granada_fogo_palido", "Granada de Fogo Pálido", "Consumível", 4, 150, 1, "", "Espalha fogo pálido."),
            simple("item.primeiros_socorros", "Kit de Primeiros Socorros", "Consumível", 5, 250, 1, "", "Tratamento completo de campo."),
            simple("item.estaca_voltaica", "Estaca Voltaica", "Consumível", 6, 400, 1, "", "Descarga elétrica concentrada."),
            simple("item.destilado_divino", "Destilado Divino", "Consumível", 6, 400, 1, "", "Reagente sagrado refinado."),
            simple("item.veneno_basilisco", "Veneno de Basilisco", "Consumível", 6, 400, 1, "", "Veneno de alta potência."),
            simple("item.macula_morte", "Mácula da Morte", "Consumível", 7, 600, 1, "", "Substância necromântica perigosa."),
            simple("item.mochila_pequena", "Mochila Pequena", "Recipiente", 1, 20, 1, "", "Capacidade de Carga +5."),
            simple("item.mochila_mercador", "Mochila de Mercador", "Recipiente", 3, 90, 2, "", "Capacidade de Carga +15."),
            simple("item.saco_frutas", "Saco de Frutas", "Recipiente", 4, 150, 2, "", "Recipiente amplo para provisões."),
            simple("item.mochila_mensageiro", "Mochila de Mensageiro", "Recipiente", 5, 250, 1, "", "Capacidade e acesso rápido a documentos."),
            simple("item.ferramenta_improvisada", "Ferramenta Improvisada", "Ferramenta", 0, 5, 1, "", "Permite uma tarefa simples com penalidade narrativa."),
            simple("item.ferramenta_comum", "Ferramenta Comum", "Ferramenta", 2, 45, 1, "", "Ferramenta adequada para um ofício."),
            simple("item.ferramenta_profissional", "Ferramenta Profissional", "Ferramenta", 4, 150, 2, "", "Conjunto profissional para um ofício."),
            simple("item.cinza_bruta", "Dose de Cinza Bruta", "Consumível arcano", 1, 20, 0, "", "Uma dose de cinza bruta."),
            simple("item.cinza_refinada", "Dose de Cinza Refinada", "Consumível arcano", 2, 45, 0, "", "Uma dose de cinza refinada."),
            simple("item.cinza_pura", "Dose de Cinza Pura", "Consumível arcano", 4, 150, 0, "", "Uma dose de cinza pura."),
            simple("item.gema_menor_aleatoria", "Gema Menor Aleatória", "Componente arcano", 2, 45, 0, "", "Para instalar em um Espaço de Gema."),
            simple("item.gema_aprimoramento_menor", "Gema de Aprimoramento Menor", "Componente arcano", 3, 90, 0, "", "Para instalar em um Espaço de Gema."),
            simple("item.gema_aleatoria", "Gema Aleatória", "Componente arcano", 4, 150, 0, "", "Para instalar em um Espaço de Gema."),
            simple("item.gema_aprimoramento_maior", "Gema de Aprimoramento Maior", "Componente arcano", 6, 400, 0, "", "Para instalar em um Espaço de Gema."),
        )
        (regular + starters).distinctBy { it.name }
    }

    private fun BuiltItem.catalogEntry(id: String) = CatalogEntry(
        id = id, kind = CatalogKind.ITEM, name = name, group = category, summary = effect,
        source = "Tabelas canônicas de equipamentos", creationCost = creationCost?.toString() ?: "#",
        price = price, load = load, durability = durability.takeIf { it > 0 }?.let { "$it/$it" }.orEmpty(), region = region,
        version = BuiltInCatalog.VERSION,
        ruleReference = "03 - Regras/Balanceamento de Criação e Equipamentos.md",
    )

    private fun simple(id: String, name: String, group: String, cost: Int, price: Int, load: Int, region: String, effect: String) =
        CatalogEntry(
            id, CatalogKind.ITEM, name, group, effect,
            source = "Criação de Personagem", version = BuiltInCatalog.VERSION,
            creationCost = cost.toString(), price = price, load = load, region = region,
            ruleReference = "03 - Regras/Criação de Personagem/Criação de Personagem.md#10-pontos-de-herança",
        )


}
