package com.kinderman.sdo.presentation.journal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.R
import com.kinderman.sdo.domain.model.CampaignJournalEntry
import com.kinderman.sdo.domain.model.CampaignJournalPolicy
import com.kinderman.sdo.domain.model.CampaignState
import com.kinderman.sdo.domain.model.JournalSyncState
import com.kinderman.sdo.ui.HudBackground
import com.kinderman.sdo.ui.LocalSdoPreferences
import com.kinderman.sdo.ui.SdoActionButton
import com.kinderman.sdo.ui.SdoActionStyle
import com.kinderman.sdo.ui.SdoCard
import com.kinderman.sdo.ui.SdoField
import com.kinderman.sdo.ui.SdoOutlinedButton
import com.kinderman.sdo.ui.SdoScreenMasthead
import com.kinderman.sdo.ui.SdoShapeTokens
import com.kinderman.sdo.ui.SdoTextButton
import com.kinderman.sdo.ui.SdoVisualMode
import com.kinderman.sdo.ui.TechPanel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val StandardInfoBorderColor = Color(0xB85690DA)
private val StandardLiveIndicatorColor = Color(0xFF00BCD4)

@Composable
fun JournalScreen(
    state: JournalUiState,
    onRetry: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onNewEntry: () -> Unit,
    onEditEntry: (CampaignJournalEntry) -> Unit,
    onUpdateDraft: (String, String) -> Unit,
    onSaveDraft: () -> Unit,
    onReloadConflict: () -> Unit,
    onRequestReplaceConflict: () -> Unit,
    onDismissReplaceConflict: () -> Unit,
    onConfirmReplaceConflict: () -> Unit,
    onRequestDelete: (CampaignJournalEntry) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onBackPressed: () -> Unit,
    onDismissDiscard: () -> Unit,
    onConfirmDiscard: () -> Unit,
) {
    BackHandler(onBack = onBackPressed)

    val campaign = state.campaign
    val archived = campaign?.state == CampaignState.ARCHIVED || state.syncState == JournalSyncState.ARCHIVED
    val eyebrow = if (archived) {
        stringResource(R.string.journal_eyebrow_archived)
    } else {
        stringResource(R.string.journal_eyebrow)
    }
    val campaignName = campaign?.name?.ifBlank { "Campanha" } ?: "Campanha"

    HudBackground(
        Modifier
            .fillMaxSize()
            .testTag("journal-screen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SdoOutlinedButton(
                    onClick = onBackPressed,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("journal-back-button"),
                ) {
                    val backLabel = if (state.mode == JournalMode.LIST) {
                        stringResource(R.string.journal_back)
                    } else {
                        stringResource(R.string.journal_back_to_list)
                    }
                    Text(backLabel)
                }
                Spacer(Modifier.weight(1f))
                JournalSyncBadge(syncState = state.syncState)
            }

            SdoScreenMasthead(
                eyebrow = eyebrow,
                title = campaignName,
                metadata = stringResource(R.string.journal_entries_count, state.entries.size),
            )

            state.statusMessage?.let { message ->
                JournalNoticeBanner(
                    message = message,
                    isError = state.uncertainSave || state.disconnectedDuringEdit,
                    modifier = Modifier.testTag("journal-status-banner"),
                )
            }

            when {
                state.syncState == JournalSyncState.ACCESS_REVOKED -> {
                    JournalBlockingState(
                        title = stringResource(R.string.journal_access_revoked_title),
                        message = stringResource(R.string.journal_access_revoked_message),
                        actionLabel = stringResource(R.string.journal_back),
                        onAction = onBackPressed,
                        tag = "journal-access-revoked",
                    )
                }

                state.syncState == JournalSyncState.UNAVAILABLE_OFFLINE -> {
                    JournalBlockingState(
                        title = stringResource(R.string.journal_offline_unavailable_title),
                        message = stringResource(R.string.journal_offline_unavailable_message),
                        actionLabel = stringResource(R.string.journal_retry),
                        onAction = onRetry,
                        tag = "journal-offline-unavailable",
                    )
                }

                state.syncState == JournalSyncState.ERROR && state.entries.isEmpty() -> {
                    JournalBlockingState(
                        title = stringResource(R.string.journal_error_title),
                        message = stringResource(R.string.journal_error_message),
                        actionLabel = stringResource(R.string.journal_retry),
                        onAction = onRetry,
                        tag = "journal-error-state",
                    )
                }

                state.syncState == JournalSyncState.LOADING && state.entries.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("journal-loading"),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                else -> {
                    when (state.mode) {
                        JournalMode.LIST -> JournalListContent(
                            state = state,
                            onNewEntry = onNewEntry,
                            onOpenEntry = onOpenEntry,
                            modifier = Modifier.weight(1f),
                        )

                        JournalMode.DETAIL -> JournalDetailContent(
                            state = state,
                            onEdit = onEditEntry,
                            onDelete = onRequestDelete,
                            modifier = Modifier.weight(1f),
                        )

                        JournalMode.EDITOR -> JournalEditorContent(
                            state = state,
                            onUpdateDraft = onUpdateDraft,
                            onSaveDraft = onSaveDraft,
                            onCancel = onBackPressed,
                            modifier = Modifier.weight(1f),
                        )

                        JournalMode.CONFLICT -> JournalConflictContent(
                            state = state,
                            onReload = onReloadConflict,
                            onRequestReplace = onRequestReplaceConflict,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    if (state.discardConfirmVisible) {
        AlertDialog(
            onDismissRequest = onDismissDiscard,
            modifier = Modifier.testTag("journal-discard-dialog"),
            title = { Text(stringResource(R.string.journal_discard_title)) },
            text = { Text(stringResource(R.string.journal_discard_message)) },
            confirmButton = {
                SdoTextButton(
                    onClick = onConfirmDiscard,
                    modifier = Modifier.testTag("journal-discard-confirm"),
                ) {
                    Text(stringResource(R.string.journal_discard_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                SdoTextButton(onClick = onDismissDiscard) {
                    Text(stringResource(R.string.journal_keep_editing))
                }
            },
        )
    }

    state.deleteConfirmEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            modifier = Modifier.testTag("journal-delete-dialog"),
            title = { Text(stringResource(R.string.journal_delete_title)) },
            text = { Text("${entry.title}\n\n${stringResource(R.string.journal_delete_message)}") },
            confirmButton = {
                SdoTextButton(
                    onClick = onConfirmDelete,
                    modifier = Modifier.testTag("journal-delete-confirm"),
                ) {
                    Text(stringResource(R.string.journal_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                SdoTextButton(onClick = onDismissDelete) {
                    Text(stringResource(R.string.journal_cancel))
                }
            },
        )
    }

    if (state.replaceConfirmVisible) {
        AlertDialog(
            onDismissRequest = onDismissReplaceConflict,
            modifier = Modifier.testTag("journal-replace-dialog"),
            title = { Text(stringResource(R.string.journal_conflict_replace_title)) },
            text = { Text(stringResource(R.string.journal_conflict_replace_message)) },
            confirmButton = {
                SdoTextButton(
                    onClick = onConfirmReplaceConflict,
                    modifier = Modifier.testTag("journal-replace-confirm"),
                ) {
                    Text(stringResource(R.string.journal_conflict_replace_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                SdoTextButton(onClick = onDismissReplaceConflict) {
                    Text(stringResource(R.string.journal_cancel))
                }
            },
        )
    }
}

@Composable
private fun JournalSyncBadge(syncState: JournalSyncState) {
    val standard = LocalSdoPreferences.current.visualMode != SdoVisualMode.CYBERGRUNGE
    val (labelRes, accent) = when (syncState) {
        JournalSyncState.LOADING -> R.string.journal_sync_loading to MaterialTheme.colorScheme.outline
        JournalSyncState.LIVE -> R.string.journal_sync_live to if (standard) StandardLiveIndicatorColor else MaterialTheme.colorScheme.primary
        JournalSyncState.CACHED_OFFLINE -> R.string.journal_sync_cached_offline to MaterialTheme.colorScheme.secondary
        JournalSyncState.UNAVAILABLE_OFFLINE -> R.string.journal_sync_unavailable_offline to MaterialTheme.colorScheme.error
        JournalSyncState.ACCESS_REVOKED -> R.string.journal_sync_access_revoked to MaterialTheme.colorScheme.error
        JournalSyncState.ARCHIVED -> R.string.journal_sync_archived to if (standard) StandardInfoBorderColor else MaterialTheme.colorScheme.outline
        JournalSyncState.ERROR -> R.string.journal_sync_error to MaterialTheme.colorScheme.error
    }
    Surface(
        modifier = Modifier.testTag("journal-sync-badge"),
        shape = SdoShapeTokens.control,
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent),
    ) {
        Text(
            text = stringResource(labelRes),
            color = accent,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun JournalNoticeBanner(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val standard = LocalSdoPreferences.current.visualMode != SdoVisualMode.CYBERGRUNGE
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        standard -> StandardInfoBorderColor
        else -> MaterialTheme.colorScheme.primary
    }
    SdoCard(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun JournalBlockingState(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    tag: String,
) {
    TechPanel(modifier = Modifier.testTag(tag)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SdoActionButton(
            label = actionLabel,
            onClick = onAction,
            style = SdoActionStyle.PRIMARY,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun JournalListContent(
    state: JournalUiState,
    onNewEntry: () -> Unit,
    onOpenEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.canCreate) {
            SdoActionButton(
                label = stringResource(R.string.journal_new_entry),
                onClick = onNewEntry,
                style = SdoActionStyle.PRIMARY,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal-new-entry"),
            )
        }

        if (state.entries.isEmpty()) {
            TechPanel(modifier = Modifier.testTag("journal-empty-state")) {
                Text(
                    text = stringResource(R.string.journal_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (state.canCreate) {
                        stringResource(R.string.journal_empty_active)
                    } else {
                        stringResource(R.string.journal_empty_readonly)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("journal-entry-list"),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    JournalEntryCard(
                        entry = entry,
                        onClick = { onOpenEntry(entry.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun JournalEntryCard(
    entry: CampaignJournalEntry,
    onClick: () -> Unit,
) {
    val standard = LocalSdoPreferences.current.visualMode != SdoVisualMode.CYBERGRUNGE
    val borderColor = if (standard) StandardInfoBorderColor else MaterialTheme.colorScheme.secondary
    val authorLabel = entry.authorName.ifBlank { CampaignJournalPolicy.REMOVED_AUTHOR_FALLBACK }
    val updatedLabel = formatJournalTimestamp(entry.updatedAt)
    val preview = remember(entry.body) {
        entry.body.trim().replace('\n', ' ').take(180)
    }

    SdoCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .testTag("journal-entry-card-${entry.id}"),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.journal_meta_author, authorLabel) + " · " + updatedLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = preview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun JournalDetailContent(
    state: JournalUiState,
    onEdit: (CampaignJournalEntry) -> Unit,
    onDelete: (CampaignJournalEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entry = state.selectedEntry
    if (entry == null) {
        JournalBlockingState(
            title = stringResource(R.string.journal_empty_title),
            message = stringResource(R.string.journal_empty_readonly),
            actionLabel = stringResource(R.string.journal_back_to_list),
            onAction = {},
            tag = "journal-detail-missing",
        )
        return
    }

    val authorLabel = entry.authorName.ifBlank { CampaignJournalPolicy.REMOVED_AUTHOR_FALLBACK }
    val editorLabel = entry.lastEditorName.ifBlank { CampaignJournalPolicy.REMOVED_AUTHOR_FALLBACK }
    val updatedLabel = formatJournalTimestamp(entry.updatedAt)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag("journal-detail-panel"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TechPanel(
            accent = if (LocalSdoPreferences.current.visualMode != SdoVisualMode.CYBERGRUNGE) {
                StandardInfoBorderColor
            } else {
                MaterialTheme.colorScheme.primary
            },
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.journal_meta_author, authorLabel),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.journal_meta_updated, updatedLabel, editorLabel, entry.revision),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = entry.body,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        if (state.canMutateSelected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SdoActionButton(
                    label = stringResource(R.string.journal_edit),
                    onClick = { onEdit(entry) },
                    style = SdoActionStyle.PRIMARY,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("journal-edit-entry"),
                )
                SdoActionButton(
                    label = stringResource(R.string.journal_delete),
                    onClick = { onDelete(entry) },
                    style = SdoActionStyle.DESTRUCTIVE,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("journal-delete-entry"),
                )
            }
        }
    }
}

@Composable
private fun JournalEditorContent(
    state: JournalUiState,
    onUpdateDraft: (String, String) -> Unit,
    onSaveDraft: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = state.draft ?: return
    val canSave = !state.saving && !state.disconnectedDuringEdit

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag("journal-editor-panel"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TechPanel {
            SdoField(
                label = stringResource(R.string.journal_field_title),
                value = draft.title,
                onValueChange = { onUpdateDraft(it, draft.body) },
                enabled = !state.saving,
                supportingText = stringResource(R.string.journal_field_title_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal-editor-title"),
            )
            SdoField(
                label = stringResource(R.string.journal_field_body),
                value = draft.body,
                onValueChange = { onUpdateDraft(draft.title, it) },
                enabled = !state.saving,
                multiline = true,
                supportingText = stringResource(R.string.journal_field_body_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
                    .testTag("journal-editor-body"),
            )
            state.validationError?.let { errorText ->
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("journal-validation-error"),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SdoActionButton(
                label = stringResource(R.string.journal_cancel),
                onClick = onCancel,
                enabled = !state.saving,
                style = SdoActionStyle.SECONDARY,
                modifier = Modifier.weight(1f),
            )
            SdoActionButton(
                label = if (state.saving) stringResource(R.string.journal_saving) else stringResource(R.string.journal_save),
                onClick = onSaveDraft,
                enabled = canSave,
                style = SdoActionStyle.PRIMARY,
                modifier = Modifier
                    .weight(1f)
                    .testTag("journal-save-entry"),
            )
        }
    }
}

@Composable
private fun JournalConflictContent(
    state: JournalUiState,
    onReload: () -> Unit,
    onRequestReplace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = state.draft
    val server = state.conflictServerEntry

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag("journal-conflict-panel"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TechPanel(accent = MaterialTheme.colorScheme.error) {
            Text(
                text = stringResource(R.string.journal_conflict_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = stringResource(R.string.journal_conflict_message),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (draft != null) {
            TechPanel {
                Text(
                    text = stringResource(R.string.journal_conflict_yours),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(draft.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(draft.body, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (server != null) {
            TechPanel {
                Text(
                    text = stringResource(R.string.journal_conflict_server),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(server.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(server.body, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SdoActionButton(
                label = stringResource(R.string.journal_conflict_reload),
                onClick = onReload,
                style = SdoActionStyle.PRIMARY,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal-conflict-reload"),
            )
            SdoActionButton(
                label = stringResource(R.string.journal_conflict_replace),
                onClick = onRequestReplace,
                style = SdoActionStyle.DESTRUCTIVE,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal-conflict-replace"),
            )
        }
    }
}

private fun formatJournalTimestamp(epochMs: Long): String {
    if (epochMs <= 0L) return "agora"
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
    return formatter.format(Date(epochMs))
}
