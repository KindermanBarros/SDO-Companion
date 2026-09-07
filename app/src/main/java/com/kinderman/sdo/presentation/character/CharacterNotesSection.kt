package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.PersonalNote
import com.kinderman.sdo.domain.model.nextPersonalNoteTitle
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.Panel
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.TechCutDark
import com.kinderman.sdo.ui.TechPanel

@Composable
internal fun NotesSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = Acid) {
        SectionHeader("14", "Registros pessoais")
        Text(
            "Crie registros separados para pistas, memórias e acontecimentos da campanha.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )

        character.personalNotes.forEachIndexed { index, note ->
            PersonalNoteCard(
                index = index,
                note = note,
                enabled = enabled,
                onRemove = {
                    onChange(
                        character.copy(
                            personalNotes = character.personalNotes.filterIndexed { noteIndex, _ ->
                                noteIndex != index
                            },
                        ),
                    )
                },
                onValue = { updated ->
                    onChange(
                        character.copy(
                            personalNotes = character.personalNotes.replace(index, updated),
                        ),
                    )
                },
            )
        }

        AddButton("Novo registro pessoal", enabled) {
            onChange(
                character.copy(
                    personalNotes = character.personalNotes + PersonalNote(
                        title = nextPersonalNoteTitle(character.personalNotes),
                    ),
                ),
            )
        }
    }
}

@Composable
private fun PersonalNoteCard(
    index: Int,
    note: PersonalNote,
    enabled: Boolean,
    onRemove: () -> Unit,
    onValue: (PersonalNote) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = TechCutDark,
                shape = CutCornerShape(topEnd = 18.dp, bottomStart = 10.dp),
            ),
        shape = CutCornerShape(topEnd = 18.dp, bottomStart = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "REGISTRO ${(index + 1).toString().padStart(2, '0')}",
                    modifier = Modifier.weight(1f),
                    color = Ice,
                    style = MaterialTheme.typography.labelLarge,
                )
                RemoveButton(enabled, "Remover registro", onRemove)
            }
            HudTextField("Título", note.title, enabled = enabled) {
                onValue(note.copy(title = it))
            }
            HudTextField("Anotação", note.text, multiline = true, enabled = enabled) {
                onValue(note.copy(text = it))
            }
        }
    }
}
