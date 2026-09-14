package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.creation.CharacterCreation
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.PowerSourceType
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.SdoInsetCard

private val creationSteps = listOf(
    "Conceito e raça", "Atributos", "5 Conhecimentos Especiais", "15 Pontos de Conhecimento",
    "Preparação do Caminho", "Poderes", "Equipamento inicial", "Revisão",
)

@Composable
internal fun CharacterCreationWizard(character: Character, catalog: List<CatalogEntry>, enabled: Boolean, onChange: (Character) -> Unit, modifier: Modifier = Modifier) {
    val step = character.creationStep.coerceIn(1, CharacterCreation.STEP_COUNT)
    val error = CharacterCreation.flowError(step, character)
    Column(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("ETAPA $step/${CharacterCreation.STEP_COUNT} // ${creationSteps[step - 1].uppercase()}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = { step / CharacterCreation.STEP_COUNT.toFloat() }, modifier = Modifier.fillMaxWidth())
            when (step) {
                2 -> CreationBudgetProgress("ATRIBUTOS", CharacterCreation.attributePointsSpent(character), 10)
                3 -> CreationBudgetProgress("CONHECIMENTOS ESPECIAIS", CharacterCreation.initialSpecialKnowledges(character).size, 5, "NÍVEL 0")
                4 -> CreationBudgetProgress("PONTOS DE CONHECIMENTO", CharacterCreation.knowledgePointsSpent(character), 15)
                7 -> CreationBudgetProgress("PONTOS DE HERANÇA", CharacterCreation.heritageSpent(character), 30, "PH")
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("creation-error")) }
        }
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        when (step) {
            1 -> item { IdentitySection(character, catalog, enabled, onChange) }
            2 -> item { AttributeSection(character, enabled, onChange, showBasicKnowledges = false) }
            3 -> item { PhaseOneKnowledgeSection(character, catalog, enabled, onChange, selectionLimit = 5, lockLevels = true) }
            4 -> {
                item { AttributeSection(character, enabled, onChange, showAttributes = false) }
                item { PhaseOneKnowledgeSection(character, catalog, enabled, onChange, allowEntryChanges = false) }
            }
            5 -> item { PhaseOnePathSection(character, catalog.filter { it.kind == CatalogKind.PATH }, enabled, onChange) }
            6 -> item {
                if (character.powers.any { it.sourceType == PowerSourceType.PATH }) {
                    CreationPowersOverview(character)
                } else {
                    PhaseOnePowerSection(
                        character = character,
                        catalog = catalog.filter { it.kind == CatalogKind.POWER },
                        enabled = enabled,
                        onChange = onChange,
                        selectionLimit = 2,
                    )
                }
            }
            7 -> item { PhaseOneInventoryWithBonusSection(character, catalog.filter { it.kind == CatalogKind.ITEM || it.kind == CatalogKind.ASH }, enabled, onChange) }
            8 -> item { CreationReview(character) }
        }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding().padding(14.dp).testTag("creation-navigation")) {
            val narrow = maxWidth < 420.dp
            val layoutModifier = Modifier.fillMaxWidth()
            val content: @Composable (Modifier, Modifier) -> Unit = { backModifier, forwardModifier ->
                    CharacterActionButton("Voltar", step > 1 && (!character.isHeritageReselection || step > 7), CharacterActionStyle.SECONDARY, backModifier.testTag("creation-back")) {
                        onChange(character.copy(creationStep = step - 1))
                    }
                    if (step < CharacterCreation.STEP_COUNT) {
                        CharacterActionButton("Continuar", error == null, CharacterActionStyle.PRIMARY, forwardModifier.testTag("creation-forward")) {
                            onChange(character.copy(creationStep = step + 1))
                        }
                    } else {
                        CharacterActionButton(if (character.isHeritageReselection) "Concluir nova Herança" else "Finalizar personagem", CharacterCreation.validate(character).isEmpty(), CharacterActionStyle.PRIMARY, forwardModifier.testTag("creation-forward")) {
                            onChange(CharacterCreation.finish(character))
                        }
                    }
            }
            if (narrow) Column(layoutModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content(Modifier.fillMaxWidth(), Modifier.fillMaxWidth())
            } else Row(layoutModifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                content(Modifier.weight(1f), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CreationBudgetProgress(label: String, selected: Int, total: Int, suffix: String = "") {
    val normalizedSelected = selected.coerceAtLeast(0)
    val remaining = (total - normalizedSelected).coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("$label // $normalizedSelected / $total${suffix.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()}", style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(
            progress = { (normalizedSelected.toFloat() / total).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("$remaining RESTANTES", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CreationPowersOverview(character: Character) {
    TechPanel {
        Text("PODERES DO PERSONAGEM", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Text("Os poderes desta etapa são definidos pelo Caminho e pela Raça. Esta tela serve apenas para conferência.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        if (character.powers.isEmpty()) {
            Text("Nenhum poder foi atribuído ainda.", color = MaterialTheme.colorScheme.error)
        } else character.powers.forEach { power ->
            SdoInsetCard {
                Text(power.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
                Text(power.effect.ifBlank { "Sem efeito descrito." }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CreationReview(character: Character) {
    TechPanel {
        Text(character.name.uppercase(), style = MaterialTheme.typography.titleLarge)
        Text("${character.race} // ${character.occupation} // NÍVEL ${character.level}")
        Text("ATRIBUTOS // ${CharacterCreation.attributePointsSpent(character)} / 10")
        Text("CONHECIMENTOS ESPECIAIS // ${CharacterCreation.initialSpecialKnowledges(character).size} / 5")
        Text("PONTOS DE CONHECIMENTO // ${CharacterCreation.knowledgePointsSpent(character)} / 15")
        Text("PONTOS DE HERANÇA // ${CharacterCreation.heritageSpent(character)} / 30")
        Text("CAMINHO // ${character.pathName.ifBlank { "PENDENTE" }}")
        Text("Ao finalizar, os orçamentos de criação desaparecem e a ficha passa ao modo normal.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
