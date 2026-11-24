package com.kinderman.sdo.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.R
import com.kinderman.sdo.domain.model.DeityDetailView
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.ui.SdoCardTitle
import com.kinderman.sdo.ui.SdoOutlinedButton
import com.kinderman.sdo.ui.SdoTextButton

@Composable
internal fun DeityDetailButton(onClick: () -> Unit) {
    SdoOutlinedButton(onClick = onClick, modifier = Modifier.testTag("deity_detail_open")) {
        Text(stringResource(R.string.deity_open_detail))
    }
}

@Composable
internal fun DeityDetailDialog(
    deityId: String,
    campaignId: String?,
    session: UserSession?,
    canShowGm: Boolean,
    load: suspend (String, String?) -> DeityDetailView?,
    onDismiss: () -> Unit,
) {
    var detail by remember(deityId, campaignId, session, canShowGm) { mutableStateOf<DeityDetailView?>(null) }
    var loading by remember(deityId, campaignId, session, canShowGm) { mutableStateOf(true) }
    LaunchedEffect(deityId, campaignId, session, canShowGm) {
        try {
            detail = load(deityId, campaignId)
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Exception) {
            detail = null
        } finally {
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { detail?.publicProfile?.let { Text(it.name) } },
        text = {
            val value = detail
            if (value == null) {
                Text(stringResource(if (loading) R.string.deity_detail_loading else R.string.deity_detail_unavailable))
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(value.publicProfile.publicTierLabel)
                    SdoCardTitle(value.publicProfile.paraJogadoresHeading)
                    Text(value.publicProfile.paraJogadores)
                    value.gmContent?.takeIf { canShowGm }?.let { gm ->
                        SdoCardTitle(gm.paraOMestreHeading)
                        Text(gm.paraOMestre)
                        gm.extraSections.forEach { section ->
                            SdoCardTitle(section.heading)
                            Text(section.body)
                        }
                    }
                }
            }
        },
        confirmButton = {
            SdoTextButton(onClick = onDismiss) { Text(stringResource(R.string.deity_detail_close)) }
        },
    )
}
