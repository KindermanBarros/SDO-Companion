package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.BuiltItem
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.ItemPart
import com.kinderman.sdo.domain.model.ItemBonus
import com.kinderman.sdo.domain.model.ItemQuality
import kotlin.math.roundToInt

object ItemCreationRules {
    const val HERITAGE_BUDGET = 30

    val weaponMaterials = listOf(
        part("sucata", "Sucata", "Material de arma", 0, 5, durability = 0, effect = "Começa em Sucata; ataques com Desvantagem."),
        part("madeira", "Madeira, Osso Simples ou Plástico", "Material de arma", 0, 10, durability = 1, effect = "Dano cortante torna-se impacto quando incompatível."),
        part("ligas_comuns", "Ligas Comuns", "Material de arma", 1, 20, durability = 2),
        part("escamas_exoticas", "Escamas Exóticas", "Material de arma", 2, 50, durability = 2, effect = "Orgânica e não metálica."),
        part("ossos_comuns", "Ossos Trabalhados", "Material de arma", 3, 100, load = -1, durability = 3, effect = "Remove Pesado; Carga mínima 2."),
        part("ligas_incomuns", "Ligas Incomuns", "Material de arma", 3, 130, durability = 5, effect = "+1 dano."),
        part("aco_negro", "Aço Negro", "Material de arma", 5, 300, durability = 6, effect = "+1 Categoria de Dado."),
        part("prata_estelar", "Prata Estelar", "Material de arma", 5, 360, durability = 5, effect = "+1 dano; Delicada; Dilaceração."),
        part("ligas_raras", "Ligas Raras", "Material de arma", null, 0, durability = 6, effect = "Propriedades e preço definidos pela liga; item #."),
        part("ligas_ancestrais", "Ligas Ancestrais", "Material de arma", null, 0, durability = 8, effect = "Propriedades definidas pela liga; item #."),
    )

    val armorMaterials = listOf(
        part("sucata", "Sucata", "Material de armadura", 0, 5, durability = 0, effect = "Começa em Sucata; PG e PL finais pela metade."),
        part("organico", "Madeira, Couro ou Tecido", "Material de armadura", 0, 10, durability = 1, pl = 1, effect = "Corte absorvido torna-se impacto."),
        part("ligas_comuns", "Ligas Comuns", "Material de armadura", 1, 20, durability = 2, pg = 1, pl = 1, effect = "RD 1 contra corte."),
        part("escamas_comuns", "Escamas Comuns", "Material de armadura", 2, 50, durability = 2, pg = 1, pl = 2, effect = "RD 1 contra impacto."),
        part("ligas_incomuns", "Ligas Incomuns", "Material de armadura", 3, 120, durability = 5, pg = 1, pl = 3, agilityLimit = 4, effect = "LA 4."),
        part("aco_negro", "Aço Negro", "Material de armadura", 5, 300, durability = 6, pg = 2, pl = 3, agilityLimit = 3, effect = "RD 4 contra corte e impacto; LA 3."),
        part("prata_estelar", "Prata Estelar", "Material de armadura", 5, 350, durability = 5, pg = 1, pl = 3, agilityLimit = 3, effect = "RD 5; Delicada; LA 3."),
        part("ligas_raras", "Ligas Raras", "Material de armadura", null, 0, durability = 6, effect = "Propriedades, PG, PL, LA e preço definidos pela liga; item #."),
        part("ligas_ancestrais", "Ligas Ancestrais", "Material de armadura", null, 0, durability = 8, effect = "Propriedades definidas pela liga; item #."),
    )

    val weaponBases = listOf(
        weapon("faca", "Faca", "Arma curta", 1, 8, 1, "1d4; Leve; +1 couro ao extrair couro animal."),
        weapon("adaga", "Adaga", "Arma curta", 2, 25, 1, "1d4; Leve; ignora PL da cabeça em Ataque Direcionado."),
        weapon("facao", "Facão", "Arma curta", 3, 50, 1, "1d4; aumenta uma Categoria após acertos consecutivos."),
        weapon("florete", "Florete", "Arma curta", 3, 60, 1, "1d4; pode usar AGI + Reflexos no ataque e dano."),
        weapon("gladio", "Gládio", "Arma curta", 5, 260, 1, "1d4; ativa Esquiva após acertar, 1/rodada."),
        weapon("machado_mao", "Machado de Mão", "Arma média", 2, 25, 2, "1d6; funciona como kit de lenhador."),
        weapon("espada_longa", "Espada Longa Ocidental", "Arma média", 3, 50, 2, "1d6; com duas mãos, +1 Categoria de Dado."),
        weapon("katana", "Katana", "Arma média", 4, 65, 2, "1d6; +2 Passos com Técnica Marcial apropriada."),
        weapon("maca", "Maça", "Arma média", 4, 110, 2, "1d6; Crítico reduz 2 Durabilidades da armadura."),
        weapon("cutelo", "Cutelo Grande", "Arma média", 6, 270, 2, "1d6; +4 dano; ignora PL de Couro."),
        weapon("espada_bastarda", "Espada Bastarda", "Arma grande", 3, 110, 3, "1d8; Pesada."),
        weapon("guarda_dupla", "Guarda Dupla", "Arma grande", 5, 260, 3, "1d8; +2 Esquiva; Ataque em Área; Pesada."),
        weapon("montante", "Montante", "Arma grande", 6, 600, 3, "1d8; cancela Esquiva; Ataque em Área; Pesada."),
        weapon("zanbato", "Zanbato", "Arma grande", 5, 650, 3, "1d8; permite Bloqueio com Técnica apropriada."),
        weapon("clava", "Clava", "Arma grande", 3, 120, 3, "1d8 de impacto; Pesada."),
        weapon("machado_guerra", "Machado de Guerra", "Arma de haste", 5, 130, 3, "1d8; ignora PL de couro, malha e madeira."),
        weapon("foice", "Foice", "Arma de haste", 5, 300, 3, "1d8; +2 Passos com Técnica; Dilaceração em Crítico."),
        weapon("lanca", "Lança", "Arma de haste", 5, 680, 3, "1d8; +2 dano; alcance 2; Anti-Cavalaria."),
        weapon("lanca_curta", "Lança Curta", "Arma de haste", 4, 620, 2, "1d8; +2 Esquiva; Arremesso 4."),
        weapon("glaive", "Glaive", "Arma de haste", 7, 900, 3, "1d8; +4 dano; Anti-Pessoal; Pesada."),
        weapon("arco_curto", "Arco Curto", "Arco ou besta", 3, 110, 2, "1d8; alcance 12 quadrados."),
        weapon("arco_longo", "Arco Longo", "Arco ou besta", 4, 130, 3, "1d8; alcance 25; Recarga."),
        weapon("lancadora", "Lançadora", "Arco ou besta", 5, 250, 2, "1d8; dispara munições especiais."),
        weapon("arco_composto", "Arco Composto", "Arco ou besta", 6, 700, 3, "1d8; alcance 30; +2 dano; Pesado."),
        weapon("besta_leve", "Besta Leve", "Arco ou besta", 3, 55, 2, "1d8; alcance 8; -2 dano; Recarga; Leve."),
        weapon("besta_pesada", "Besta Pesada", "Arco ou besta", 8, 950, 3, "1d8; alcance 30; +4 dano; ignora PL; Recarga; Pesada."),
        weapon("pistola_vapor", "Pistola de Vapor", "Arma de vapor", 3, 60, 1, "1d6; alcance 8; Sobrecarga 2/6; Leve."),
        weapon("rifle_vapor", "Rifle de Assalto a Vapor", "Arma de vapor", 5, 140, 2, "1d6; alcance 8; Sobrecarga 1/6; +1 Passo."),
        weapon("fuzil_vapor", "Fuzil a Vapor", "Arma de vapor", 7, 300, 3, "1d6; alcance 40; ignora PL; +2 dano; Pesado."),
        weapon("estilhacadora", "Estilhaçadora", "Arma de vapor", 7, 600, 3, "Disparo em cone; Sobrecarga; Pesada."),
        weapon("pistola_tesla", "Pistola de Tesla", "Arma de Tesla", 5, 130, 1, "1d6 + 1d6 elétrico; alcance 8; Paralisia 6; Bobina 6."),
        weapon("rifle_tesla", "Rifle de Assalto de Tesla", "Arma de Tesla", 8, 700, 2, "1d6 + 1d6 elétrico; dois alvos; Paralisia 8; Bobina 6."),
        weapon("fuzil_tesla", "Fuzil de Tesla", "Arma de Tesla", 8, 280, 3, "1d6 + 1d6 elétrico; alcance 30; +3 dano; Pesado."),
        weapon("lanca_tesla", "Lança de Tesla", "Arma de Tesla", 7, 300, 3, "1d8 + 1d6 elétrico; alcance 2; +2 dano."),
        weapon("luvas_tesla", "Luvas de Tesla", "Arma de Tesla", 6, 320, 1, "1d4 + 1d6 elétrico; +3 dano desarmado."),
        weapon("lanca_chamas", "Lança-Chamas", "Arma especial", 9, 1_300, 3, "Área 2×3; 1d10 fogo; Queimando; munição 5."),
        weapon("canhao_acido", "Canhão de Ácido", "Arma especial", 12, 3_500, 4, "Área 3×3; 1d10 ácido; Corrosão; munição 3."),
        weapon("besta_repeticao", "Besta de Repetição", "Arma especial", 8, 900, 3, "Três virotes de 1d6+1; alcance 10; munição 9."),
        weapon("lanca_granadas", "Lança-Granadas", "Arma especial", 10, 1_700, 3, "Dispara granada até 8 quadrados; munição 3."),
    )

    val armorBases = listOf(
        armor("elmo", "Elmo", "Armadura", 1, 10, 1, 1, 1, "cabeça"),
        armor("peitoral_comum", "Peitoral Comum", "Armadura", 2, 25, 3, 2, 1, "torso"),
        armor("peitoral_malha", "Peitoral em Malha", "Armadura", 2, 20, 3, 1, 2, "torso", "Não altera corte para impacto."),
        armor("peitoral_segmentado", "Peitoral Segmentado", "Armadura", 3, 30, 4, 2, 2, "torso", "Carga +1 já aplicada."),
        armor("gibao", "Gibão", "Armadura", 1, 25, 2, 1, 1, "torso", "Apenas Couro, Tecido ou fibra."),
        armor("jaqueta_revestida", "Jaqueta Revestida", "Armadura", 2, 30, 2, 2, 0, "torso", "Ignora a LA do Material."),
        armor("perneiras", "Perneiras", "Armadura", 2, 10, 2, 1, 1, "pernas", "LA 4.", agilityLimit = 4),
        armor("escudo_leve", "Escudo Leve", "Escudo", 3, 55, 2, 2, 0, "mão ocupada", "+2 PG enquanto empunhado."),
        armor("escudo_pesado", "Escudo Pesado", "Escudo", 5, 110, 3, 4, 1, "mão ocupada", "+4 PG; Pesado."),
        armor("manopla", "Manopla ou Luva", "Acessório", 1, 10, 1, 0, 1, "mãos"),
        armor("bracadeira", "Braçadeira", "Acessório", 2, 12, 1, 0, 1, "braços", "+1 Proteção de Esquiva."),
        armor("botas", "Botas", "Acessório", 1, 8, 1, 0, 1, "pés"),
        armor("botas_viagem", "Botas de Viagem", "Acessório", 2, 25, 1, 0, 0, "pés", "+2 horas de viagem antes de testar Exaustão."),
        armor("sapatilhas", "Sapatilhas", "Acessório", 2, 10, 1, 0, 0, "pés", "Ignora a LA do Material."),
        armor("bijuteria", "Bijuteria", "Acessório", 2, 25, 1, 0, 1, "cabeça, braço ou mão", "LA 4.", agilityLimit = 4),
        armor("colar_nobre", "Colar Nobre", "Acessório", 2, 110, 1, 0, 0, "cabeça", "+2 Postura contra não nobres."),
        armor("manto_negro", "Manto Negro", "Acessório", 1, 15, 1, 0, 1, "torso"),
        armor("manto_elite", "Manto de Elite", "Acessório", 2, 45, 1, 0, 0, "torso", "+2 social contra classe inferior à elite representada."),
        armor("enganchadora", "Enganchadora", "Acessório", 4, 140, 1, 0, 0, "braço", "Alcance 3; causa 2 dano e fixa."),
        armor("manopla_besteira", "Manopla Besteira", "Acessório", 5, 250, 1, 0, 0, "mão", "Dispara virote leve oculto."),
        armor("enganchadora_vapor", "Enganchadora a Vapor", "Acessório", 5, 320, 1, 0, 0, "braço", "Pode puxar alvo ou usuário."),
    )

    val weaponModifications = listOf(
        part("cruel", "Cruel", "Modificação de arma", 0, 10, effect = "Alvo a 0 PV entra nas Portas da Morte."),
        part("equilibrada", "Equilibrada", "Modificação de arma", 2, 45, effect = "+1 Ataque e +1 dano."),
        part("afiada", "Afiada", "Modificação de arma", 2, 50, effect = "+2 Ataque."),
        part("brutal", "Brutal", "Modificação de arma", 2, 55, effect = "+2 dano."),
        part("raptora", "Raptora", "Modificação de arma", 4, 280, effect = "Aplica Sangramento 1 contra PL 0 ou armadura não metálica."),
        part("acumuladora", "Acumuladora", "Modificação de arma", 10, 3_200, effect = "Acumula até 5 cargas; cada uma causa +1d4 arcano."),
    )

    val armorModifications = listOf(
        part("chamativa", "Chamativa", "Modificação de armadura", 1, 20, effect = "+1 presença quando percebida."),
        part("robusta", "Robusta", "Modificação de armadura", 4, 65, load = 1, pg = 3, effect = "+3 PG; Carga +1; LA -1."),
        part("ajustada", "Ajustada", "Modificação de armadura", 2, 45, load = -1, effect = "Carga -1, mínimo 1; LA +1."),
        part("nobre", "Nobre", "Modificação de armadura", 1, 60, effect = "+2 Postura contra não nobres."),
        part("sob_medida", "Sob Medida", "Modificação de armadura", 4, 130, effect = "Exige Ajustada; LA +2 adicional."),
    )

    val gemComponents = listOf(
        part("gema_menor_aleatoria", "Gema Menor Aleatória", "Gema", 2, 45, effect = "Ocupa 1 Espaço de Gema; efeito sorteado ou definido pelo Historiador."),
        part("gema_aprimoramento_menor", "Gema de Aprimoramento Menor", "Gema", 3, 90, effect = "Ocupa 1 Espaço de Gema; aprimoramento menor."),
        part("gema_aleatoria", "Gema Aleatória", "Gema", 4, 150, effect = "Ocupa 1 Espaço de Gema; efeito sorteado ou definido pelo Historiador."),
        part("gema_aprimoramento_maior", "Gema de Aprimoramento Maior", "Gema", 6, 400, effect = "Ocupa 1 Espaço de Gema; aprimoramento maior."),
    )

    fun build(
        base: ItemPart,
        material: ItemPart,
        modifications: List<ItemPart>,
        gemSlots: Int,
        technologySlots: Int,
        customName: String = "",
        quality: ItemQuality = ItemQuality.COMMON,
        bonuses: List<ItemBonus> = emptyList(),
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
            numericCosts.filterNotNull().sum() + effectiveGemSlots + effectiveTechnologySlots * 2 + bonuses.sumOf { it.creationCost }
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
            bonuses.filter { it.target.isNotBlank() && it.value != 0 }.forEach {
                add("Bônus: ${it.value.withSign()} em ${it.type.label} — ${it.target}.")
            }
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
            bonuses = bonuses.filter { it.target.isNotBlank() && it.value != 0 },
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

    private fun Int.withSign() = if (this > 0) "+$this" else toString()

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

    private fun weapon(id: String, name: String, group: String, cost: Int, price: Int, load: Int, effect: String) =
        part(id, name, group, cost, price, load = load, effect = effect)

    private fun armor(
        id: String, name: String, group: String, cost: Int, price: Int, load: Int,
        pg: Int, pl: Int, region: String, effect: String = "", agilityLimit: Int? = null,
    ) = part(
        id, name, group, cost, price, load = load, region = region, effect = effect,
        pg = pg, pl = pl, agilityLimit = agilityLimit,
    )

    private fun part(
        id: String, name: String, group: String, cost: Int?, price: Int, load: Int = 0,
        durability: Int = 0, region: String = "", effect: String = "", pg: Int = 0, pl: Int = 0,
        agilityLimit: Int? = null,
    ) = ItemPart(id, name, group, cost, cost?.let(::standardPrice) ?: price, load, durability, region, effect, pg, pl, agilityLimit)
}
