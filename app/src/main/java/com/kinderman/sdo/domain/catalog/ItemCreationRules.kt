package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.BuiltItem
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.ItemPart
import com.kinderman.sdo.domain.model.ItemMaterialPart

object ItemCreationRules {
    const val HERITAGE_BUDGET = 20

    val weaponMaterials = listOf(
        part("sucata", "Sucata", "Material de arma", 0, 5, durability = 0, effect = "Começa em Sucata; ataques com Desvantagem."),
        part("madeira", "Madeira ou Plástico", "Material de arma", 0, 10, durability = 1, effect = "Dano cortante torna-se impacto."),
        part("ligas_comuns", "Ligas Comuns", "Material de arma", 1, 20, durability = 2),
        part("escamas_exoticas", "Escamas Exóticas", "Material de arma", 2, 50, durability = 2, effect = "Orgânica e não metálica."),
        part("ossos_comuns", "Ossos Comuns", "Material de arma", 3, 100, load = -1, durability = 2, effect = "Remove Pesado; Carga mínima 2."),
        part("ligas_incomuns", "Ligas Incomuns", "Material de arma", 3, 130, durability = 5, effect = "+1 dano."),
        part("aco_negro", "Aço Negro", "Material de arma", 4, 300, durability = 6, effect = "+1 Categoria de Dado."),
        part("prata_estelar", "Prata Estelar", "Material de arma", 4, 360, durability = 5, effect = "+1 dano; Delicada; Dilaceração."),
        part("ligas_raras", "Ligas Raras", "Material de arma", null, 1_500, durability = 6, effect = "+1 Categoria de Dado e +1 dano; item #."),
        part("ligas_ancestrais", "Ligas Ancestrais", "Material de arma", null, 0, durability = 8, effect = "Propriedades definidas pela liga; item #."),
    )

    val armorMaterials = listOf(
        part("sucata", "Sucata", "Material de armadura", 0, 5, durability = 0, effect = "Começa em Sucata; PG e PL finais pela metade."),
        part("organico", "Madeira, Couro ou Tecido", "Material de armadura", 0, 10, durability = 1, pl = 1, effect = "Corte absorvido torna-se impacto."),
        part("ligas_comuns", "Ligas Comuns", "Material de armadura", 1, 20, durability = 2, pg = 1, pl = 1, effect = "RD 1 contra corte."),
        part("escamas_comuns", "Escamas Comuns", "Material de armadura", 2, 50, durability = 2, pg = 1, pl = 2, effect = "RD 1 contra impacto."),
        part("ligas_incomuns", "Ligas Incomuns", "Material de armadura", 3, 120, durability = 5, pg = 2, pl = 3, effect = "LA 4."),
        part("aco_negro", "Aço Negro", "Material de armadura", 4, 300, durability = 6, pg = 3, pl = 3, effect = "RD 4 contra corte e impacto; LA 3."),
        part("prata_estelar", "Prata Estelar", "Material de armadura", 4, 350, durability = 5, pg = 2, pl = 3, effect = "RD 5; Delicada; LA 3."),
        part("ligas_raras", "Ligas Raras", "Material de armadura", null, 1_400, durability = 6, pg = 3, pl = 4, effect = "RD 4 contra perfuração, corte e impacto; LA 3; item #."),
        part("ligas_ancestrais", "Ligas Ancestrais", "Material de armadura", null, 0, durability = 8, effect = "Propriedades definidas pela liga; item #."),
    )

    val weaponBases = listOf(
        weapon("faca", "Faca", "Arma curta", 0, 8, 1, "1d4; Leve; +1 couro ao extrair couro animal."),
        weapon("adaga", "Adaga", "Arma curta", 1, 25, 1, "1d4; Leve; ignora PL da cabeça em Ataque Direcionado."),
        weapon("facao", "Facão", "Arma curta", 2, 50, 1, "1d4; aumenta uma Categoria após acertos consecutivos."),
        weapon("florete", "Florete", "Arma curta", 2, 60, 1, "1d4; pode usar AGI + Reflexos no ataque e dano."),
        weapon("gladio", "Gládio", "Arma curta", 4, 260, 1, "1d4; ativa Esquiva após acertar, 1/rodada."),
        weapon("machado_mao", "Machado de Mão", "Arma média", 1, 25, 2, "1d6; funciona como kit de lenhador."),
        weapon("espada_longa", "Espada Longa Ocidental", "Arma média", 2, 50, 2, "1d6; com duas mãos, +1 Categoria de Dado."),
        weapon("katana", "Katana", "Arma média", 2, 65, 2, "1d6; +2 Passos com Técnica Marcial apropriada."),
        weapon("maca", "Maça", "Arma média", 3, 110, 2, "1d6; Crítico reduz 2 Durabilidades da armadura."),
        weapon("cutelo", "Cutelo Grande", "Arma média", 4, 270, 2, "1d6; +4 dano; ignora PL de Couro."),
        weapon("espada_bastarda", "Espada Bastarda", "Arma grande", 3, 110, 3, "1d8; Pesada."),
        weapon("guarda_dupla", "Guarda Dupla", "Arma grande", 4, 260, 3, "1d8; +2 Esquiva; Ataque em Área; Pesada."),
        weapon("montante", "Montante", "Arma grande", 5, 600, 3, "1d8; cancela Esquiva; Ataque em Área; Pesada."),
        weapon("zanbato", "Zanbato", "Arma grande", 5, 650, 3, "1d8; permite Bloqueio com Técnica apropriada."),
        weapon("clava", "Clava", "Arma grande", 3, 120, 3, "1d8 de impacto; Pesada."),
        weapon("machado_guerra", "Machado de Guerra", "Arma de haste", 3, 130, 3, "1d8; ignora PL de couro, malha e madeira."),
        weapon("foice", "Foice", "Arma de haste", 4, 300, 3, "1d8; +2 Passos com Técnica; Dilaceração em Crítico."),
        weapon("lanca", "Lança", "Arma de haste", 5, 680, 3, "1d8; +2 dano; alcance 2; Anti-Cavalaria."),
        weapon("lanca_curta", "Lança Curta", "Arma de haste", 5, 620, 2, "1d8; +2 Esquiva; Arremesso 4."),
        weapon("glaive", "Glaive", "Arma de haste", 6, 900, 3, "1d8; +4 dano; Anti-Pessoal; Pesada."),
        weapon("arco_curto", "Arco Curto", "Arco ou besta", 3, 110, 2, "1d8; alcance 12 quadrados."),
        weapon("arco_longo", "Arco Longo", "Arco ou besta", 3, 130, 3, "1d8; alcance 25; Recarga."),
        weapon("arco_composto", "Arco Composto", "Arco ou besta", 5, 700, 3, "1d8; alcance 30; +2 dano; Pesado."),
        weapon("besta_leve", "Besta Leve", "Arco ou besta", 2, 55, 2, "1d8; alcance 8; -2 dano; Recarga; Leve."),
        weapon("besta_pesada", "Besta Pesada", "Arco ou besta", 6, 950, 3, "1d8; alcance 30; +4 dano; ignora PL; Recarga; Pesada."),
        weapon("pistola_vapor", "Pistola de Vapor", "Arma de vapor", 2, 60, 1, "1d6; alcance 8; Sobrecarga 2/6; Leve."),
        weapon("rifle_vapor", "Rifle de Assalto a Vapor", "Arma de vapor", 3, 140, 2, "1d6; alcance 8; Sobrecarga 1/6; +1 Passo."),
        weapon("fuzil_vapor", "Fuzil a Vapor", "Arma de vapor", 4, 300, 3, "1d6; alcance 40; ignora PL; +2 dano; Pesado."),
        weapon("pistola_tesla", "Pistola de Tesla", "Arma de Tesla", 3, 130, 1, "1d6 + 1d6 elétrico; alcance 8; Paralisia 6; Bobina 6."),
        weapon("rifle_tesla", "Rifle de Assalto de Tesla", "Arma de Tesla", 5, 700, 2, "1d6 + 1d6 elétrico; dois alvos; Paralisia 8; Bobina 6."),
        weapon("fuzil_tesla", "Fuzil de Tesla", "Arma de Tesla", 4, 280, 3, "1d6 + 1d6 elétrico; alcance 30; +3 dano; Pesado."),
        weapon("lanca_tesla", "Lança de Tesla", "Arma de Tesla", 4, 300, 3, "1d8 + 1d6 elétrico; alcance 2; +2 dano."),
        weapon("luvas_tesla", "Luvas de Tesla", "Arma de Tesla", 4, 320, 1, "1d4 + 1d6 elétrico; +3 dano desarmado."),
        weapon("lanca_chamas", "Lança-Chamas", "Arma especial", 7, 1_300, 3, "Área 2×3; 1d10 fogo; Queimando; munição 5."),
        weapon("canhao_acido", "Canhão de Ácido", "Arma especial", 10, 3_500, 4, "Área 3×3; 1d10 ácido; Corrosão; munição 3."),
        weapon("besta_repeticao", "Besta de Repetição", "Arma especial", 6, 900, 3, "Três virotes de 1d6+1; alcance 10; munição 9."),
        weapon("lanca_granadas", "Lança-Granadas", "Arma especial", 8, 1_700, 3, "Dispara granada até 8 quadrados; munição 3."),
    )

    val armorBases = listOf(
        armor("elmo", "Elmo", "Armadura", 0, 10, 1, 1, 1, "cabeça"),
        armor("peitoral_comum", "Peitoral Comum", "Armadura", 1, 25, 3, 2, 1, "torso"),
        armor("peitoral_malha", "Peitoral em Malha", "Armadura", 1, 20, 3, 1, 1, "torso", "Não altera corte para impacto."),
        armor("peitoral_segmentado", "Peitoral Segmentado", "Armadura", 1, 30, 4, 3, 2, "torso", "Carga +1 já aplicada."),
        armor("gibao", "Gibão", "Armadura", 1, 25, 2, 2, 1, "torso", "Apenas Couro, Tecido ou fibra."),
        armor("jaqueta_revestida", "Jaqueta Revestida", "Armadura", 1, 30, 2, 3, 0, "torso", "Ignora a LA do Material."),
        armor("perneiras", "Perneiras", "Armadura", 0, 10, 2, 2, 1, "pernas", "LA 4."),
        armor("escudo_leve", "Escudo Leve", "Escudo", 2, 55, 2, 0, 0, "mão ocupada", "+2 PG enquanto empunhado."),
        armor("escudo_pesado", "Escudo Pesado", "Escudo", 3, 110, 3, 0, 1, "mão ocupada", "+4 PG; Pesado."),
        armor("manopla", "Manopla ou Luva", "Acessório", 0, 10, 1, 0, 1, "mãos"),
        armor("bracadeira", "Braçadeira", "Acessório", 0, 12, 1, 0, 1, "braços", "+1 Proteção de Esquiva."),
        armor("botas", "Botas", "Acessório", 0, 8, 1, 0, 1, "pés"),
        armor("botas_viagem", "Botas de Viagem", "Acessório", 0, 25, 1, 0, 0, "pés", "+2 horas de viagem antes de testar Exaustão."),
        armor("sapatilhas", "Sapatilhas", "Acessório", 0, 10, 1, 0, 0, "pés", "Ignora a LA do Material."),
        armor("bijuteria", "Bijuteria", "Acessório", 1, 25, 1, 0, 1, "cabeça, braço ou mão", "LA 4."),
        armor("colar_nobre", "Colar Nobre", "Acessório", 3, 110, 1, 0, 0, "pescoço", "+2 Postura contra não nobres."),
        armor("manto_negro", "Manto Negro", "Acessório", 0, 15, 1, 0, 1, "torso"),
        armor("manto_elite", "Manto de Elite", "Acessório", 1, 45, 1, 0, 0, "torso", "+2 social contra classe inferior à elite representada."),
        armor("enganchadora", "Enganchadora", "Acessório", 3, 140, 1, 0, 0, "braço", "Alcance 3; causa 2 dano e fixa."),
        armor("enganchadora_vapor", "Enganchadora a Vapor", "Acessório", 4, 320, 1, 0, 0, "braço", "Pode puxar alvo ou usuário."),
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
        part("robusta", "Robusta", "Modificação de armadura", 2, 65, load = 1, pg = 3, effect = "+3 PG; Carga +1; LA -1."),
        part("ajustada", "Ajustada", "Modificação de armadura", 2, 45, load = -1, effect = "Carga -1, mínimo 1; LA +1."),
        part("nobre", "Nobre", "Modificação de armadura", 2, 60, effect = "+2 Postura contra não nobres."),
        part("sob_medida", "Sob Medida", "Modificação de armadura", 3, 130, effect = "Exige Ajustada; LA +2 adicional."),
    )

    fun build(
        base: ItemPart,
        material: ItemPart,
        modifications: List<ItemPart>,
        gemSlots: Int,
        technologySlots: Int,
        customName: String = "",
    ): BuiltItem = build(
        base = base,
        parts = listOf(ItemMaterialPart("Parte principal", material)),
        modifications = modifications,
        gemSlots = gemSlots,
        technologySlots = technologySlots,
        customName = customName,
    )

    fun build(
        base: ItemPart,
        parts: List<ItemMaterialPart>,
        modifications: List<ItemPart>,
        gemSlots: Int,
        technologySlots: Int,
        customName: String = "",
    ): BuiltItem {
        require(parts.isNotEmpty()) { "Um item precisa de pelo menos uma parte material." }
        val materials = parts.map { it.material }
        val numericCosts = listOf(base.creationCost) + materials.map { it.creationCost } + modifications.map { it.creationCost }
        val creationCost = if (numericCosts.any { it == null }) null else numericCosts.filterNotNull().sum() + gemSlots + technologySlots * 2
        val pg = base.pg + materials.sumOf { it.pg } + modifications.sumOf { it.pg }
        val pl = base.pl + materials.sumOf { it.pl } + modifications.sumOf { it.pl }
        val details = buildList {
            add("Composição por partes:")
            parts.forEach { part -> add("• ${part.name}: ${part.material.name}.") }
            add("Qualidade: Comum.")
            if (pg > 0 || pl > 0) add("PG $pg; PL $pl.")
            add(base.effect)
            parts.forEach { part ->
                if (part.material.effect.isNotBlank()) add("${part.name}: ${part.material.effect}")
            }
            modifications.forEach { add("${it.name}: ${it.effect}") }
            if (gemSlots > 0) add("Espaços de Gema: $gemSlots.")
            if (technologySlots > 0) add("Espaços de Tecnologia: $technologySlots.")
            add("Complexidade: ${complexity(creationCost)}.")
            if (creationCost == null) add("Custo da Criação: #; exige permissão do Historiador.")
        }.filter(String::isNotBlank).joinToString("\n")
        return BuiltItem(
            name = customName.ifBlank {
                if (parts.size == 1) "${base.name} de ${parts.first().material.name}" else base.name
            },
            category = base.group,
            creationCost = creationCost,
            price = base.price + materials.sumOf { it.price } + modifications.sumOf { it.price } + gemSlots * 20 + technologySlots * 45,
            load = (base.load + materials.sumOf { it.load } + modifications.sumOf { it.load }).coerceAtLeast(1),
            durability = materials.map { it.durability }.minOrNull() ?: 0,
            region = base.region,
            effect = details,
            pg = pg,
            pl = pl,
        )
    }

    fun complexity(cost: Int?): String = when (cost) {
        null -> "CD 22 ou mais; Progressos e intervalo definidos pelo projeto"
        0 -> "CD 9; 1 Progresso; 1 hora por teste"
        in 1..2 -> "CD 11; 2 Progressos; 2 horas por teste"
        in 3..4 -> "CD 13; 4 Progressos; 4 horas por teste"
        in 5..6 -> "CD 15; 6 Progressos; 1 dia por teste"
        in 7..8 -> "CD 18; 8 Progressos; 2 dias por teste"
        else -> "CD 20; 10 Progressos; 1 semana por teste"
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
        )
        (regular + starters).distinctBy { it.name }
    }

    private fun BuiltItem.catalogEntry(id: String) = CatalogEntry(
        id = id, kind = CatalogKind.ITEM, name = name, group = category, summary = effect,
        source = "Tabelas canônicas de equipamentos", creationCost = creationCost?.toString() ?: "#",
        price = price, load = load, durability = durability.takeIf { it > 0 }?.let { "$it/$it" }.orEmpty(), region = region,
    )

    private fun simple(id: String, name: String, group: String, cost: Int, price: Int, load: Int, region: String, effect: String) =
        CatalogEntry(id, CatalogKind.ITEM, name, group, effect, source = "Criação de Personagem", creationCost = cost.toString(), price = price, load = load, region = region)

    private fun weapon(id: String, name: String, group: String, cost: Int, price: Int, load: Int, effect: String) =
        part(id, name, group, cost, price, load = load, effect = effect)

    private fun armor(id: String, name: String, group: String, cost: Int, price: Int, load: Int, pg: Int, pl: Int, region: String, effect: String = "") =
        part(id, name, group, cost, price, load = load, region = region, effect = effect, pg = pg, pl = pl)

    private fun part(
        id: String, name: String, group: String, cost: Int?, price: Int, load: Int = 0,
        durability: Int = 0, region: String = "", effect: String = "", pg: Int = 0, pl: Int = 0,
    ) = ItemPart(id, name, group, cost, price, load, durability, region, effect, pg, pl)
}
