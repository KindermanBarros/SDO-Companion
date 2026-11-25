package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.creation.DivineAscensionCreation
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterDivineAscension
import com.kinderman.sdo.domain.model.DivineAscensionActDraft
import com.kinderman.sdo.domain.model.DivineAscensionDraft
import com.kinderman.sdo.domain.model.DivineHeraldGiftDraft
import com.kinderman.sdo.domain.model.DivinePassiveCapability
import com.kinderman.sdo.domain.model.DivinePillarKind
import com.kinderman.sdo.domain.model.DivineTier
import com.kinderman.sdo.domain.model.DurationKind
import com.kinderman.sdo.domain.model.ExecutionKind
import com.kinderman.sdo.domain.model.RangeBand
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.SdoCardTitle
import com.kinderman.sdo.ui.SdoIconButton
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoMetadataText
import com.kinderman.sdo.ui.SdoOutlinedButton
import com.kinderman.sdo.ui.SdoResponsiveGrid
import com.kinderman.sdo.ui.SdoTextButton
import com.kinderman.sdo.ui.SdoTheme
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.TechPanel

private val ascensionSteps = listOf(
    "Conceito Divino & Tabu",
    "Pilares Divinos",
    "Capacidades, Atos & Dádiva",
    "Revisão & Despertar",
)

private val suggestedPostures = listOf(
    "Preservação",
    "Cobrança",
    "Subversão",
    "Testemunho",
    "Provação",
    "Reforja",
    "Barganha",
    "Ressonância",
)

@Composable
internal fun DivineAscensionWizard(
    character: Character,
    enabled: Boolean,
    canCancelPromotion: Boolean,
    onChange: (Character) -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetTier = DivineAscensionCreation.targetTier(character) ?: DivineTier.LESSER_GOD
    val draft = DivineAscensionCreation.currentDraft(character)
    val step = draft.step.coerceIn(1, DivineAscensionCreation.STEP_COUNT)
    val error = DivineAscensionCreation.validateStep(step, character, draft)
    val expectedPillarTotal = DivineAscensionCreation.requiredTotalPillars(character, targetTier)

    fun saveDraft(updated: DivineAscensionDraft) {
        if (enabled) onChange(DivineAscensionCreation.updateDraft(character, updated))
    }

    Column(modifier.fillMaxSize().testTag("divine-ascension-wizard")) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "ASCENSÃO A ${targetTier.label.uppercase()} // ETAPA $step/${DivineAscensionCreation.STEP_COUNT}",
                    color = DivinePinkAccent,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                if (canCancelPromotion && enabled) {
                    SdoTextButton(
                        onClick = { onChange(DivineAscensionCreation.cancelPromotion(character)) },
                        modifier = Modifier.testTag("divine-ascension-cancel"),
                    ) {
                        Text("CANCELAR PROMOÇÃO", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(
                ascensionSteps[step - 1].uppercase(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(
                progress = { step / DivineAscensionCreation.STEP_COUNT.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = DivinePinkAccent,
            )
            if (step == 2) {
                val spent = draft.pillars.totalPoints
                val remaining = expectedPillarTotal - spent
                SdoMetadataText(
                    "ORÇAMENTO DE PILARES // $spent / $expectedPillarTotal PONTOS (${if (remaining >= 0) "$remaining RESTANTES" else "${-remaining} ACIMA DO LIMITE"})",
                    color = if (remaining == 0) DivinePinkAccent else MaterialTheme.colorScheme.error,
                )
                LinearProgressIndicator(
                    progress = { (spent.toFloat() / expectedPillarTotal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = DivinePinkAccent,
                )
            }
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("divine-ascension-error"),
                )
            }
        }

        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (step) {
                1 -> item("step-concept") {
                    AscensionConceptStep(targetTier, draft, enabled, ::saveDraft)
                }
                2 -> item("step-pillars") {
                    AscensionPillarsStep(character, targetTier, draft, enabled, ::saveDraft)
                }
                3 -> item("step-capabilities") {
                    AscensionCapabilitiesStep(targetTier, draft, enabled, ::saveDraft)
                }
                4 -> item("step-review") {
                    AscensionReviewStep(character, targetTier, draft)
                }
            }
        }

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(14.dp)
                .testTag("divine-ascension-navigation"),
        ) {
            val narrow = maxWidth < 420.dp
            val content: @Composable (Modifier, Modifier) -> Unit = { backMod, forwardMod ->
                CharacterActionButton(
                    label = "Voltar",
                    enabled = enabled && step > 1,
                    style = CharacterActionStyle.SECONDARY,
                    modifier = backMod.testTag("divine-ascension-back"),
                ) {
                    saveDraft(draft.copy(step = step - 1))
                }
                if (step < DivineAscensionCreation.STEP_COUNT) {
                    CharacterActionButton(
                        label = "Continuar",
                        enabled = enabled && error == null,
                        style = CharacterActionStyle.PRIMARY,
                        modifier = forwardMod.testTag("divine-ascension-forward"),
                    ) {
                        saveDraft(draft.copy(step = step + 1))
                    }
                } else {
                    CharacterActionButton(
                        label = "Despertar como ${targetTier.label}",
                        enabled = enabled && DivineAscensionCreation.validateStep(4, character, draft) == null,
                        style = CharacterActionStyle.PRIMARY,
                        modifier = forwardMod.testTag("divine-ascension-finish"),
                    ) {
                        onChange(DivineAscensionCreation.finish(DivineAscensionCreation.updateDraft(character, draft)))
                    }
                }
            }
            if (narrow) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    content(Modifier.fillMaxWidth(), Modifier.fillMaxWidth())
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    content(Modifier.weight(1f), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AscensionConceptStep(
    targetTier: DivineTier,
    draft: DivineAscensionDraft,
    enabled: Boolean,
    onSave: (DivineAscensionDraft) -> Unit,
) {
    TechPanel(accent = DivinePinkAccent) {
        SectionHeader("01", "Conceito Divino, Postura & Tabu")
        Text(
            if (targetTier == DivineTier.LESSER_GOD) {
                "Ao ascender a Deus Menor, sua ficha converte-se para a escala conceitual: defina o conceito governado, seu território sagrado e a filosofia inquebrável que sustenta seus milagres."
            } else {
                "Ao ascender a Deus Maior, consolide seu Domínio pleno no Plano Onírico, seu assento planar e sua Restrição Material irreversível."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HudTextField(
            label = "Domínio (ex.: Bruma, Ferrugem, Escassez, Último Sono)",
            value = draft.dominio,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().testTag("ascension-field-dominio"),
            onValue = { onSave(draft.copy(dominio = it)) },
        )
        HudTextField(
            label = "Santuário / Âncora Planar (território ou refúgio consagrado)",
            value = draft.santuario,
            enabled = enabled,
            multiline = true,
            modifier = Modifier.fillMaxWidth().testTag("ascension-field-santuario"),
            onValue = { onSave(draft.copy(santuario = it)) },
        )
        ChoiceField(
            label = "Postura sugerida",
            value = draft.postura.takeIf { it in suggestedPostures } ?: suggestedPostures.first(),
            options = suggestedPostures,
            enabled = enabled,
        ) { selected ->
            onSave(draft.copy(postura = selected))
        }
        HudTextField(
            label = "Postura diante do Domínio",
            value = draft.postura,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().testTag("ascension-field-postura"),
            onValue = { onSave(draft.copy(postura = it)) },
        )
        HudTextField(
            label = "Linha de Pensamento (filosofia divina)",
            value = draft.linhaDePensamento,
            enabled = enabled,
            multiline = true,
            modifier = Modifier.fillMaxWidth().testTag("ascension-field-thought"),
            onValue = { onSave(draft.copy(linhaDePensamento = it)) },
        )
        HudTextField(
            label = "Tabu Mecânico (violar custa −2 PD e bloqueia Atos na cena)",
            value = draft.tabu,
            enabled = enabled,
            multiline = true,
            modifier = Modifier.fillMaxWidth().testTag("ascension-field-tabu"),
            onValue = { onSave(draft.copy(tabu = it)) },
        )
        if (targetTier == DivineTier.GREATER_GOD) {
            HudTextField(
                label = "Restrição Material (atuação em Nulvara e Intervenção Direta)",
                value = draft.materialRestrictionNote,
                enabled = enabled,
                multiline = true,
                modifier = Modifier.fillMaxWidth().testTag("ascension-field-restriction"),
                onValue = { onSave(draft.copy(materialRestrictionNote = it)) },
            )
        }
    }
}

@Composable
private fun AscensionPillarsStep(
    character: Character,
    targetTier: DivineTier,
    draft: DivineAscensionDraft,
    enabled: Boolean,
    onSave: (DivineAscensionDraft) -> Unit,
) {
    val maxCap = DivineAscensionCreation.maxPerPillar(targetTier)
    val expectedTotal = DivineAscensionCreation.requiredTotalPillars(character, targetTier)
    val descriptions = mapOf(
        DivinePillarKind.ASPECTO to "Feitos corporais, sensoriais e mentais sobre-humanos na manifestação.",
        DivinePillarKind.DOMINIO to "Autoridade direta para Criar, Preservar, Destruir, Alterar ou Saber sobre seu conceito.",
        DivinePillarKind.SANTUARIO to "Poder exercido no próprio território consagrado e desempate em choques divinos.",
        DivinePillarKind.PRESENCA to "Densidade espiritual, resistência e capacidade de vínculos (2 × Presença).",
    )

    TechPanel(accent = DivinePinkAccent) {
        SectionHeader("02", "Alocação dos Quatro Pilares Divinos")
        Text(
            if (targetTier == DivineTier.LESSER_GOD) {
                "Distribua exatamente 8 pontos entre os quatro Pilares (mínimo 0, máximo 4 por Pilar)."
            } else {
                "Some exatamente +4 pontos aos seus Pilares atuais (máximo 5 por Pilar, sem reduzir valores anteriores)."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DivinePillarKind.entries.forEach { pillar ->
            val currentVal = draft.pillars.get(pillar)
            val minVal = DivineAscensionCreation.minForPillar(character, targetTier, pillar)
            val canAdd = enabled && currentVal < maxCap && draft.pillars.totalPoints < expectedTotal
            val canRemove = enabled && currentVal > minVal
            SdoInsetCard(
                accent = DivinePinkAccent.copy(alpha = 0.7f),
                contentPadding = 12.dp,
                verticalSpacing = 6.dp,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        SdoCardTitle(pillar.label.uppercase(), color = DivinePinkAccent)
                        Text(
                            descriptions.getValue(pillar),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (pillar == DivinePillarKind.PRESENCA) {
                            SdoMetadataText(
                                "CAPACIDADE DE VÍNCULOS: ${2 * currentVal} (ÂNCORA = 1, AVATAR = 2)",
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SdoIconButton(
                            onClick = { onSave(draft.copy(pillars = draft.pillars.with(pillar, currentVal - 1))) },
                            enabled = canRemove,
                            modifier = Modifier.testTag("ascension-pillar-dec-${pillar.name}"),
                        ) {
                            Icon(Icons.Default.Remove, "Diminuir ${pillar.label}", tint = DivinePinkAccent)
                        }
                        Text(
                            currentVal.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = DivinePinkAccent,
                            modifier = Modifier.padding(horizontal = 10.dp),
                        )
                        SdoIconButton(
                            onClick = { onSave(draft.copy(pillars = draft.pillars.with(pillar, currentVal + 1))) },
                            enabled = canAdd,
                            modifier = Modifier.testTag("ascension-pillar-inc-${pillar.name}"),
                        ) {
                            Icon(Icons.Default.Add, "Aumentar ${pillar.label}", tint = DivinePinkAccent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AscensionCapabilitiesStep(
    targetTier: DivineTier,
    draft: DivineAscensionDraft,
    enabled: Boolean,
    onSave: (DivineAscensionDraft) -> Unit,
) {
    val maxActCost = if (targetTier == DivineTier.GREATER_GOD) 7 else 5
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TechPanel(accent = DivinePinkAccent) {
            SectionHeader("03A", "Capacidades Passivas (0 PD)")
            Text(
                if (targetTier == DivineTier.LESSER_GOD) {
                    "Defina de 2 a 3 Capacidades Passivas permanentes ligadas ao seu Domínio ou natureza divina. Elas serão registradas na aba Poderes (Passiva · 0 PD)."
                } else {
                    "Revise ou expanda suas Capacidades Passivas permanentes (mínimo 2)."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            draft.passiveCapabilities.forEachIndexed { index, passive ->
                SdoInsetCard(accent = DivinePinkAccent.copy(alpha = 0.55f), contentPadding = 10.dp, verticalSpacing = 6.dp) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SdoMetadataText("PASSIVA ${(index + 1).toString().padStart(2, '0')}", color = DivinePinkAccent, modifier = Modifier.weight(1f))
                        if (draft.passiveCapabilities.size > 2 && enabled) {
                            RemoveButton(true, "Remover passiva") {
                                onSave(draft.copy(passiveCapabilities = draft.passiveCapabilities.filterNot { it.id == passive.id }))
                            }
                        }
                    }
                    HudTextField(
                        label = "Nome da Capacidade Passiva",
                        value = passive.name,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth().testTag("ascension-passive-name-$index"),
                        onValue = { value ->
                            onSave(
                                draft.copy(
                                    passiveCapabilities = draft.passiveCapabilities.mapIndexed { idx, p ->
                                        if (idx == index) p.copy(name = value) else p
                                    },
                                ),
                            )
                        },
                    )
                    HudTextField(
                        label = "Efeito permanente",
                        value = passive.description,
                        enabled = enabled,
                        multiline = true,
                        modifier = Modifier.fillMaxWidth().testTag("ascension-passive-desc-$index"),
                        onValue = { value ->
                            onSave(
                                draft.copy(
                                    passiveCapabilities = draft.passiveCapabilities.mapIndexed { idx, p ->
                                        if (idx == index) p.copy(description = value) else p
                                    },
                                ),
                            )
                        },
                    )
                }
            }
            if (enabled && (targetTier == DivineTier.GREATER_GOD || draft.passiveCapabilities.size < 3)) {
                SdoOutlinedButton(
                    onClick = { onSave(draft.copy(passiveCapabilities = draft.passiveCapabilities + DivinePassiveCapability())) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("+ ADICIONAR CAPACIDADE PASSIVA")
                }
            }
        }

        TechPanel(accent = DivinePinkAccent) {
            SectionHeader("03B", "Atos de Domínio Ativos (PD)")
            Text(
                "Cadastre seus milagres característicos (0 PD Sinais · 1–2 PD Menor · 3–4 PD Maior · 5 PD Mítico${if (targetTier == DivineTier.GREATER_GOD) " · 7 PD Apagamento" else ""}). Todos serão adicionados como Poderes em PD.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            draft.domainActs.forEachIndexed { index, act ->
                SdoInsetCard(accent = DivinePinkAccent.copy(alpha = 0.55f), contentPadding = 10.dp, verticalSpacing = 6.dp) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SdoMetadataText("ATO ${(index + 1).toString().padStart(2, '0')} // ${act.costPd} PD", color = DivinePinkAccent, modifier = Modifier.weight(1f))
                        if (draft.domainActs.size > 1 && enabled) {
                            RemoveButton(true, "Remover Ato de Domínio") {
                                onSave(draft.copy(domainActs = draft.domainActs.filterNot { it.id == act.id }))
                            }
                        }
                    }
                    HudTextField(
                        label = "Nome do Ato de Domínio",
                        value = act.name,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth().testTag("ascension-act-name-$index"),
                        onValue = { value ->
                            onSave(draft.copy(domainActs = draft.domainActs.mapIndexed { idx, a -> if (idx == index) a.copy(name = value) else a }))
                        },
                    )
                    TwoFields(
                        { mod ->
                            ChoiceField("Custo em PD", act.costPd.coerceIn(0, maxActCost), (0..maxActCost).toList(), enabled, mod, display = { "$it PD" }) { cost ->
                                onSave(draft.copy(domainActs = draft.domainActs.mapIndexed { idx, a -> if (idx == index) a.copy(costPd = cost) else a }))
                            }
                        },
                        { mod ->
                            ChoiceField(
                                "Execução",
                                act.executionKind,
                                listOf(ExecutionKind.Action, ExecutionKind.Free, ExecutionKind.Reaction, ExecutionKind.Turn),
                                enabled,
                                mod,
                                display = ExecutionKind::label,
                            ) { exec ->
                                onSave(draft.copy(domainActs = draft.domainActs.mapIndexed { idx, a -> if (idx == index) a.copy(executionKind = exec) else a }))
                            }
                        },
                    )
                    TwoFields(
                        { mod ->
                            ChoiceField("Alcance", act.rangeBand, RangeBand.entries, enabled, mod, display = RangeBand::label) { range ->
                                onSave(draft.copy(domainActs = draft.domainActs.mapIndexed { idx, a -> if (idx == index) a.copy(rangeBand = range) else a }))
                            }
                        },
                        { mod ->
                            ChoiceField("Duração", act.durationKind, DurationKind.entries, enabled, mod, display = DurationKind::label) { dur ->
                                onSave(draft.copy(domainActs = draft.domainActs.mapIndexed { idx, a -> if (idx == index) a.copy(durationKind = dur) else a }))
                            }
                        },
                    )
                    HudTextField(
                        label = "Efeito do Milagre",
                        value = act.description,
                        enabled = enabled,
                        multiline = true,
                        modifier = Modifier.fillMaxWidth().testTag("ascension-act-desc-$index"),
                        onValue = { value ->
                            onSave(draft.copy(domainActs = draft.domainActs.mapIndexed { idx, a -> if (idx == index) a.copy(description = value) else a }))
                        },
                    )
                }
            }
            if (enabled) {
                SdoOutlinedButton(
                    onClick = { onSave(draft.copy(domainActs = draft.domainActs + DivineAscensionActDraft(costPd = 1))) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("+ ADICIONAR ATO DE DOMÍNIO")
                }
            }
        }

        TechPanel(accent = DivinePinkAccent) {
            SectionHeader("03C", "Dádiva de Arauto (Opcional)")
            Text(
                "Se desejar marcar mortais ou companheiros da mesa como seus Arautos/Âncoras, defina a Dádiva concedida e sua contrapartida obrigatória.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val gift = draft.heraldGift
            fun saveGift(updated: DivineHeraldGiftDraft) = onSave(draft.copy(heraldGift = updated))
            HudTextField(
                label = "Nome da Dádiva de Arauto",
                value = gift.name,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                onValue = { saveGift(gift.copy(name = it)) },
            )
            ChoiceField(
                label = "Execução da Dádiva",
                value = gift.executionKind,
                options = listOf(ExecutionKind.Free, ExecutionKind.Reaction, ExecutionKind.Passive, ExecutionKind.Action),
                enabled = enabled,
                display = ExecutionKind::label,
            ) { saveGift(gift.copy(executionKind = it)) }
            HudTextField(
                label = "Efeito concedido ao Arauto",
                value = gift.effect,
                enabled = enabled,
                multiline = true,
                modifier = Modifier.fillMaxWidth(),
                onValue = { saveGift(gift.copy(effect = it)) },
            )
            HudTextField(
                label = "Contrapartida negativa obrigatória",
                value = gift.downside,
                enabled = enabled,
                multiline = true,
                modifier = Modifier.fillMaxWidth(),
                onValue = { saveGift(gift.copy(downside = it)) },
            )
        }
    }
}

@Composable
private fun AscensionReviewStep(
    character: Character,
    targetTier: DivineTier,
    draft: DivineAscensionDraft,
) {
    val baseCeiling = if (targetTier == DivineTier.GREATER_GOD) 10 else 5
    TechPanel(accent = DivinePinkAccent) {
        SectionHeader("04", "Revisão da Ascensão Divina")
        SdoInsetCard(accent = DivinePinkAccent, contentPadding = 12.dp, verticalSpacing = 6.dp) {
            SdoMetadataText("DESPERTAR DIVINO", color = DivinePinkAccent)
            SdoCardTitle("${character.name.ifBlank { "Sem nome" }} → ${targetTier.label.uppercase()}", color = DivinePinkAccent)
            Text("Domínio: ${draft.dominio} // Santuário: ${draft.santuario}")
            Text("Postura: ${draft.postura}")
            Text("Linha de Pensamento: ${draft.linhaDePensamento}")
            Text("Tabu: ${draft.tabu}", color = DivinePinkAccent)
            Text("Destino: ${targetTier.pdPerSession} PD/sessão (reserva base $baseCeiling PD) // Capacidade de Vínculos: ${2 * draft.pillars.presenca}")
        }
        SdoResponsiveGrid(DivinePillarKind.entries, minItemWidth = 132.dp) { pillar, modifier ->
            SdoInsetCard(modifier = modifier, accent = DivinePinkAccent.copy(alpha = 0.6f), contentPadding = 10.dp) {
                SdoMetadataText(pillar.label.uppercase(), color = DivinePinkAccent)
                Text(draft.pillars.get(pillar).toString(), style = MaterialTheme.typography.headlineSmall, color = DivinePinkAccent)
            }
        }
        SdoInsetCard(accent = DivinePinkAccent.copy(alpha = 0.5f), contentPadding = 10.dp, verticalSpacing = 4.dp) {
            SdoMetadataText("CAPACIDADES PASSIVAS & ATOS EM PD", color = DivinePinkAccent)
            draft.passiveCapabilities.filter { it.name.isNotBlank() }.forEach { p ->
                Text("• [Passiva · 0 PD] ${p.name}: ${p.description}", style = MaterialTheme.typography.bodySmall)
            }
            draft.domainActs.filter { it.name.isNotBlank() }.forEach { a ->
                Text("• [Ato · ${a.costPd} PD · ${a.executionKind.label}] ${a.name}: ${a.description}", style = MaterialTheme.typography.bodySmall)
            }
            if (draft.heraldGift.name.isNotBlank()) {
                Text(
                    "• [Dádiva de Arauto · ${draft.heraldGift.executionKind.label}] ${draft.heraldGift.name} — Contrapartida: ${draft.heraldGift.downside}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DivinePinkAccent,
                )
            }
        }
        Text(
            "Ao confirmar, a ficha será convertida para ${targetTier.label}, os Pilares permanentes serão fixados e suas capacidades ativas e passivas serão adicionadas à aba Poderes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun DivineAscensionWizardPreview() {
    SdoTheme {
        DivineAscensionWizard(
            character = DivineAscensionCreation.unlockPromotion(
                Character(id = "char-1", name = "Kaelen", divineAscension = CharacterDivineAscension()),
                DivineTier.LESSER_GOD,
            ),
            enabled = true,
            canCancelPromotion = true,
            onChange = {},
        )
    }
}
