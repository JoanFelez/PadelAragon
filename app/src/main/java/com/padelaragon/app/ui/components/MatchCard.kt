package com.padelaragon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.padelaragon.app.data.model.MatchResult

@Composable
fun MatchCard(
    match: MatchResult,
    modifier: Modifier = Modifier,
    onTeamClick: ((teamId: Int, teamName: String) -> Unit)? = null
) {
    val isLocalTeamClickable = onTeamClick != null && match.localTeamId > 0
    val isVisitorTeamClickable = onTeamClick != null && match.visitorTeamId > 0

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
        androidx.compose.foundation.layout.Column(
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
        }
    }
}
