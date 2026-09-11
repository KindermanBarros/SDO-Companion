package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.creation.CharacterCreation
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.ui.TechPanel

private val creationSteps = listOf(
    "Conceito e raça", "Atributos", "5 Conhecimentos Especiais", "15 Pontos de Conhecimento",
    "Raça e origem", "Recursos", "Proteções", "Preparação do Caminho", "Caminho e Poderes",
    "Equipamento inicial", "Inventário e corpo", "Traços", "Revisão",
)

@Composable
internal fun CharacterCreationWizard(character: Character, catalog: List<CatalogEntry>, enabled: Boolean, onChange: (Character) -> Unit) {
    val step = character.creationStep.coerceIn(1, CharacterCreation.STEP_COUNT)
    val error = CharacterCreation.stepError(step, character)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("creation-header") {
            TechPanel {
                Text("CRIAÇÃO DE PERSONAGEM", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                Text("ETAPA $step DE ${CharacterCreation.STEP_COUNT} // ${creationSteps[step - 1].uppercase()}", style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(progress = { step / CharacterCreation.STEP_COUNT.toFloat() }, modifier = Modifier.fillMaxWidth())
                when (step) {
                    2 -> Text("PONTOS DE ATRIBUTO // ${CharacterCreation.attributePointsSpent(character)} / 10")
                    3 -> Text("CONHECIMENTOS ESPECIAIS // ${CharacterCreation.specialKnowledges(character).size} / 5")
                    4 -> Text("PONTOS DISTRIBUÍDOS // ${CharacterCreation.knowledgePointsSpent(character)} / 15")
                    10 -> Text("PONTOS DE HERANÇA // ${CharacterCreation.heritageSpent(character)} / 30")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        when (step) {
            1 -> item { IdentitySection(character, catalog, enabled, onChange) }
            2 -> item { AttributeSection(character, enabled, onChange, showBasicKnowledges = false) }
            3 -> item { PhaseOneKnowledgeSection(character, catalog, enabled, onChange, selectionLimit = 5, lockLevels = true) }
            4 -> {
                item { AttributeSection(character, enabled, onChange, showAttributes = false) }
                item { PhaseOneKnowledgeSection(character, catalog, enabled, onChange, allowEntryChanges = false) }
            }
            5 -> item { IdentitySection(character, catalog, enabled, onChange) }
            6 -> item { ResourceSection(character, enabled, onChange) }
            7 -> item { ProtectionSection(character, enabled, onChange) }
            8 -> item { PhaseOnePathSection(character, catalog.filter { it.kind == CatalogKind.PATH }, enabled, onChange) }
            9 -> {
                item { PhaseOnePathSection(character, catalog.filter { it.kind == CatalogKind.PATH }, enabled, onChange) }
                item { PhaseOnePowerSection(character, catalog.filter { it.kind == CatalogKind.POWER }, enabled, onChange) }
            }
            10 -> item { PhaseOneInventoryWithBonusSection(character, catalog.filter { it.kind == CatalogKind.ITEM || it.kind == CatalogKind.ASH }, enabled, onChange) }
            11 -> item { BodySection(character, enabled, onChange) }
            12 -> item { TraitSection(character, enabled, onChange) }
            13 -> item { CreationReview(character) }
        }
        item("creation-navigation") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(enabled = step > 1, onClick = { onChange(character.copy(creationStep = step - 1)) }) { Text("VOLTAR") }
                if (step < CharacterCreation.STEP_COUNT) {
                    TextButton(enabled = error == null, onClick = { onChange(character.copy(creationStep = step + 1)) }) { Text("CONTINUAR") }
                } else {
                    val allValid = (1..12).all { CharacterCreation.stepError(it, character) == null }
                    TextButton(enabled = allValid, onClick = { onChange(CharacterCreation.finish(character)) }) { Text("FINALIZAR PERSONAGEM") }
                }
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
        Text("CONHECIMENTOS ESPECIAIS // ${CharacterCreation.specialKnowledges(character).size} / 5")
        Text("PONTOS DE CONHECIMENTO // ${CharacterCreation.knowledgePointsSpent(character)} / 15")
        Text("PONTOS DE HERANÇA // ${CharacterCreation.heritageSpent(character)} / 30")
        Text("CAMINHO // ${character.pathName.ifBlank { "PENDENTE" }}")
        Text("Ao finalizar, os orçamentos de criação desaparecem e a ficha passa ao modo normal.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
