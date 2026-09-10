package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.ItemPart

data class GlossaryEntry(
    val term: String,
    val group: String,
    val definition: String,
    val section: String = "Termos",
    val referenceId: String = "",
)

object EquipmentGlossary {
    private val rules = listOf(
        GlossaryEntry("P.G. — Proteção Geral", "Proteções", "É a dificuldade para acertar o personagem. A P.G. concedida por equipamentos equipados é somada à P.G. do personagem."),
        GlossaryEntry("P.L. — Proteção Local", "Proteções", "Reduz o dano depois que a região atingida é determinada. Em ataque mirado, use a P.L. do membro escolhido; ela não substitui a dificuldade do ataque."),
        GlossaryEntry("LA — Limitação de Agilidade", "Proteções", "Limita quanto da Agilidade pode ser aproveitado enquanto o equipamento é usado. Efeitos que ignoram ou alteram LA prevalecem sobre o valor do material."),
        GlossaryEntry("Leve", "Propriedades", "Propriedade de equipamento leve. Só concede os benefícios escritos no item ou na técnica que interage com Leve; não muda o dado automaticamente."),
        GlossaryEntry("Pesado", "Propriedades", "Propriedade de equipamento pesado. Suas exigências e interações são aplicadas pelo texto do item, material ou técnica; não é sinônimo de Carga."),
        GlossaryEntry("Delicado", "Propriedades", "Propriedade de equipamento sensível. A perda de Durabilidade ocorre quando a regra ou o efeito que interage com Delicado determinar."),
        GlossaryEntry("Dilaceração", "Propriedades", "Propriedade de dano dilacerante. Ela só é ativada na situação indicada pelo item ou efeito, como um Crítico."),
        GlossaryEntry("Anti-Pessoal", "Propriedades", "Propriedade voltada contra alvos individuais. A vantagem concreta é a descrita pelo item ou pela regra que referencia Anti-Pessoal."),
        GlossaryEntry("Anti-Cavalaria", "Propriedades", "Propriedade voltada contra montarias, cavaleiros e investidas. A vantagem concreta é a descrita pelo item ou pela regra que referencia Anti-Cavalaria."),
        GlossaryEntry("Sobrecarga", "Tecnologia", "Marcador de armas a vapor. A notação X/Y registra o gatilho X e a capacidade ou escala Y; a resolução completa segue a regra de Sobrecarga do equipamento."),
        GlossaryEntry("Bobina", "Tecnologia", "Reserva operacional de uma arma de Tesla. O número indicado registra sua capacidade; gastos e recarga seguem o procedimento da arma."),
        GlossaryEntry("Passo de dado", "Dados", "É a quantidade de dados lançados. Em 1d8, o passo é 1; aumentar um passo transforma 1d8 em 2d8."),
        GlossaryEntry("Categoria de dado", "Dados", "É o tipo ou número de faces do dado. Em 1d8, a categoria é d8; aumentar uma categoria segue a escala de dados adotada pela regra."),
        GlossaryEntry("Qualidade", "Equipamentos", "Classifica acabamento e poder em Mundana, Comum, Aprimorada, Icônica, Obra-Prima, Artefato ou Anciã. A qualidade altera PH, preço, proteções, espaços e efeitos conforme a categoria."),
        GlossaryEntry("Durabilidade", "Equipamentos", "É registrada como atual/máxima e deriva do único material predominante. Qualidade e Durabilidade são informações diferentes."),
        GlossaryEntry("Material predominante", "Equipamentos", "Cada arma ou armadura possui um único material predominante. Partes, camadas e ligas sob a mesma família não repetem custo, PG, PL ou Durabilidade."),
    )

    val entries: List<GlossaryEntry> by lazy {
        rules +
            materialEntries(ItemCreationRules.weaponMaterials) +
            materialEntries(ItemCreationRules.armorMaterials) +
            itemEntries(ItemCreationRules.weaponBases + ItemCreationRules.armorBases) +
            modificationEntries(ItemCreationRules.weaponModifications + ItemCreationRules.armorModifications)
    }

    private fun materialEntries(parts: List<ItemPart>) = parts.map { part ->
        GlossaryEntry(
            term = part.name,
            group = part.group,
            definition = describe(part, includeDurability = true), section = "Materiais", referenceId = part.id,
        )
    }

    private fun itemEntries(parts: List<ItemPart>) = parts.map { part ->
        GlossaryEntry(
            term = part.name,
            group = part.group,
            definition = describe(part, includeDurability = false), section = "Armas", referenceId = part.id,
        )
    }

    private fun modificationEntries(parts: List<ItemPart>) = parts.map { part ->
        GlossaryEntry(part.name, part.group, part.effect.ifBlank { "Sem efeito adicional." }, "Termos", part.id)
    }

    private fun describe(part: ItemPart, includeDurability: Boolean): String = buildList {
        add("Criação ${part.creationCost ?: "#"} PH; referência ${part.price} E$.")
        if (part.load != 0) add("Carga ${part.load}.")
        if (includeDurability) add("Durabilidade ${part.durability}.")
        if (part.pg != 0 || part.pl != 0) add("P.G. ${part.pg}; P.L. ${part.pl}.")
        if (part.region.isNotBlank()) add("Região: ${part.region}.")
        if (part.effect.isNotBlank()) add(part.effect)
    }.joinToString(" ")
}
