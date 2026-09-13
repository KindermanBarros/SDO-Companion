package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind

object BuiltInCatalog {
    const val VERSION = 4
    private fun rows(kind: CatalogKind, source: String, values: String): List<CatalogEntry> =
        values.trimIndent().lineSequence().filter(String::isNotBlank).map { row ->
            val p = row.split('|')
            CatalogEntry(id = "${kind.name.lowercase()}.${p[0]}", kind = kind, name = p[1],
                group = p[2], summary = p[3], mechanicalEffect = p[3], source = source,
                version = VERSION, ruleReference = "03 - Regras/Caminhos")
        }.toList()

    private val paths = rows(CatalogKind.PATH, "Catálogos canônicos de Caminhos", """
        engrenagens|Caminho das Engrenagens|Solidão dos Oprimidos|Reparar, criação e perseverança.
        personagem|Caminho do Personagem|Solidão dos Oprimidos|Caminho pessoal de Robert Julian Dobber.
        necrocamminus|Necrocamminus|Solidão dos Oprimidos|Morte, travessia e transformação.
        herdeiro_ruinas|Herdeiro das Ruínas|Solidão dos Oprimidos|Relíquias, legado e exploração.
        la_befana|Caminho da La Befana|Solidão dos Oprimidos|Culinária, cuidado e tradição.
        festival_eterno|Caminho do Festival Eterno|Silêncio dos Oráculos|Impressionar, motivar e alegrar.
        mao_justica|Caminho da Mão da Justiça|Silêncio dos Oráculos|Lei, proteção e julgamento.
        redencao|Caminho da Redenção|Silêncio dos Oráculos|Culpa, reparação e esperança.
        conhecimento_juvenil|Caminho do Conhecimento Juvenil|Silêncio dos Oráculos|Curiosidade, estudo e descoberta.
        apostador|Caminho do Apostador|Silêncio dos Oráculos|Risco, leitura e oportunidade.
        pedra_rocha|Caminho da Pedra e da Rocha|Silêncio dos Oráculos|Resistência, construção e impacto.
        podio|Caminho do Pódio|Silêncio dos Oráculos|Competição, vitória e reconhecimento.
        desmanche|Caminho do Desmanche|Silêncio dos Oráculos|Sucata, desmontagem e sobrevivência.
        peao|Caminho do Peão|Sons do Orgulho|Servidão, doutrina e obediência.
        governante|Caminho do Governante|Sons do Orgulho|Autoridade, estratégia e responsabilidade.
        guardia_segredos|Guardiã dos Segredos|Sons do Orgulho|Sigilo, memória e proteção.
        dispariedade|Caminho da Dispariedade|Sons do Orgulho|Contraste, ruptura e identidade.
        sangue_carmesim|Caminho do Sangue Carmesim|Sons do Orgulho|Sangue, sacrifício e legado.
    """)

    val entries: List<CatalogEntry> = paths + CanonicalCatalogData.entries.filter {
        it.kind in setOf(CatalogKind.POWER, CatalogKind.MAGIC, CatalogKind.ASH, CatalogKind.RUNE)
    } + CombatPowerCatalog.entries + ItemCreationRules.catalog
}
