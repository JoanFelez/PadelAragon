package com.padelaragon.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.padelaragon.app.data.model.MatchDetail
import com.padelaragon.app.data.model.MatchResult

@Composable
fun MatchCard(
    match: MatchResult,
    modifier: Modifier = Modifier,
    detail: MatchDetail? = null,
    isLoadingDetail: Boolean = false,
    onToggleDetail: (() -> Unit)? = null,
    onTeamClick: ((teamId: Int, teamName: String) -> Unit)? = null
) {
    val isLocalTeamClickable = onTeamClick != null && match.localTeamId > 0
    val isVisitorTeamClickable = onTeamClick != null && match.visitorTeamId > 0
    var expanded by remember { mutableStateOf(false) }
    val canExpand = onToggleDetail != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.localTeam,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isLocalTeamClickable) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = (
                        if (isLocalTeamClickable) {
                            Modifier.clickable { onTeamClick?.invoke(match.localTeamId, match.localTeam) }
                        } else {
                            Modifier
                        }
                    ).then(Modifier.weight(1f))
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${match.localScore} - ${match.visitorScore}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = match.visitorTeam,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isVisitorTeamClickable) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.End,
                    modifier = (
                        if (isVisitorTeamClickable) {
                            Modifier.clickable { onTeamClick?.invoke(match.visitorTeamId, match.visitorTeam) }
                        } else {
                            Modifier
                        }
                    ).then(Modifier.weight(1f))
                )
            }

            val metadataParts = buildList {
                match.date?.takeIf { it.isNotBlank() }?.let { add("📅 $it") }
                match.venue?.takeIf { it.isNotBlank() }?.let { add("📍 $it") }
            }

            if (metadataParts.isNotEmpty()) {
                Text(
                    text = metadataParts.joinToString("   "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expand/collapse toggle
            if (canExpand) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expanded = !expanded
                            if (expanded && detail == null && !isLoadingDetail) {
                                onToggleDetail?.invoke()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Ocultar detalle" else "Ver detalle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expandable detail content
            AnimatedVisibility(
                visible = expanded && canExpand,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    if (isLoadingDetail && detail == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else if (detail != null && detail.pairs.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            detail.pairs.forEach { pair ->
                                PairDetailRow(pair)
                            }
                        }
                    } else if (!isLoadingDetail) {
                        Text(
                            text = "Sin detalles disponibles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PairDetailRow(pair: com.padelaragon.app.data.model.PairDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Pareja ${pair.pairNumber}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pair.localPlayer1,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pair.localPlayer2,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = pair.sets.joinToString("  ") { "${it.localScore}-${it.visitorScore}" },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = pair.visitorPlayer1,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End
                )
                Text(
                    text = pair.visitorPlayer2,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
