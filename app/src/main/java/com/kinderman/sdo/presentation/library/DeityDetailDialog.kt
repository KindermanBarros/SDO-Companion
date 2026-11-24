package com.kinderman.sdo.presentation.library

import androidx.annotation.StringRes
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
import com.kinderman.sdo.domain.model.DeityGmContent
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
                        DeityStructuredDetails(gm)
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


@Composable
private fun DeityStructuredDetails(gm: DeityGmContent) {
    DeitySection(R.string.deity_detail_epigraph, gm.epigraph)
    DeitySection(R.string.deity_detail_overview, gm.overview)
    DeitySection(R.string.deity_detail_public_image, gm.publicImageSummary)
    SdoCardTitle(stringResource(R.string.deity_detail_identity))
    DeityField(R.string.deity_detail_name, gm.name)
    DeityField(R.string.deity_detail_category, gm.identity.categoria.ifBlank { gm.rawCategory })
    DeityField(R.string.deity_detail_domain, gm.identity.dominio)
    DeityField(R.string.deity_detail_sanctuary, gm.identity.santuario)
    DeityField(R.string.deity_detail_stance, gm.identity.postura)
    DeityField(R.string.deity_detail_cult, gm.identity.culto)
    DeityField(R.string.deity_detail_manifestation, gm.identity.manifestacao)
    DeitySection(R.string.deity_detail_thought, gm.linhaDePensamento)
    DeitySection(R.string.deity_detail_tabu, gm.tabu)
    gm.pillars?.let { pillars ->
        SdoCardTitle(stringResource(R.string.deity_detail_pillars))
        DeityField(R.string.deity_detail_aspect, pillars.aspecto.toString())
        DeityField(R.string.deity_detail_domain, pillars.dominio.toString())
        DeityField(R.string.deity_detail_sanctuary, pillars.santuario.toString())
        DeityField(R.string.deity_detail_presence, pillars.presenca.toString())
    }
    gm.tacticalStats?.let { stats ->
        SdoCardTitle(stringResource(R.string.deity_detail_stats))
        Text(stringResource(R.string.deity_detail_pools, stats.pv, stats.pe, stats.pm, stats.ps, stats.pd))
        if (stats.attributes.isNotEmpty()) {
            SdoCardTitle(stringResource(R.string.deity_detail_attributes))
            stats.attributes.forEach { (name, value) ->
                Text(stringResource(R.string.deity_detail_field, name, value.toString()))
            }
        }
        if (stats.protections.isNotEmpty()) {
            SdoCardTitle(stringResource(R.string.deity_detail_protections))
            stats.protections.forEach { (name, value) ->
                Text(stringResource(R.string.deity_detail_field, name, value.toString()))
            }
        }
    }
    DeityField(R.string.deity_detail_destiny, gm.destinyPoints.toString())
    val corruption = gm.irradiatedCorruption
    if (corruption.intensidade.isNotBlank() || corruption.efeitoPeculiar.isNotBlank()) {
        SdoCardTitle(stringResource(R.string.deity_detail_corruption))
        DeityField(R.string.deity_detail_intensity, corruption.intensidade)
        DeityField(R.string.deity_detail_peculiar_effect, corruption.efeitoPeculiar)
    }
    if (gm.domainActs.isNotEmpty()) {
        SdoCardTitle(gm.actsHeading.ifBlank { stringResource(R.string.deity_detail_acts) })
        gm.domainActs.forEach { act ->
            SdoCardTitle(act.name)
            Text(stringResource(R.string.deity_detail_cost, act.costPd))
            act.listedDc?.let { Text(stringResource(R.string.deity_detail_dc, it)) }
            if (act.description.isNotBlank()) Text(act.description)
        }
    }
    if (gm.passiveCapabilities.isNotEmpty()) {
        SdoCardTitle(stringResource(R.string.deity_detail_passives))
        gm.passiveCapabilities.forEach { passive ->
            SdoCardTitle(passive.name)
            if (passive.description.isNotBlank()) Text(passive.description)
        }
    }
    val gift = gm.heraldGift
    if (listOf(gift.heading, gift.name, gift.headerNote, gift.effect, gift.downside).any { it.isNotBlank() }) {
        SdoCardTitle(gift.heading.ifBlank { stringResource(R.string.deity_detail_gift) })
        DeityField(R.string.deity_detail_name, gift.name)
        DeityField(R.string.deity_detail_execution, gift.execution)
        if (gift.headerNote.isNotBlank()) Text(gift.headerNote)
        DeityField(R.string.deity_detail_effect, gift.effect)
        DeityField(R.string.deity_detail_downside, gift.downside)
    }
    DeitySection(R.string.deity_detail_behavior, gm.behavior)
    DeitySection(R.string.deity_detail_mystery, gm.centralMystery)
    DeitySection(R.string.deity_detail_hooks, gm.tableHooks)
    if (gm.tags.isNotEmpty()) DeityField(R.string.deity_detail_tags, gm.tags.joinToString(", "))
}

@Composable
private fun DeitySection(@StringRes heading: Int, body: String) {
    if (body.isNotBlank()) {
        SdoCardTitle(stringResource(heading))
        Text(body)
    }
}

@Composable
private fun DeityField(@StringRes label: Int, value: String) {
    if (value.isNotBlank()) {
        Text(stringResource(R.string.deity_detail_field, stringResource(label), value))
    }
}
