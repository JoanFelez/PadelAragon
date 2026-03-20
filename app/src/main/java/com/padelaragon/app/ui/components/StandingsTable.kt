package com.padelaragon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.padelaragon.app.data.model.StandingRow

private val COL_POS = 36.dp
private val COL_TEAM = 160.dp
private val COL_STAT = 40.dp

@Composable
fun StandingsTable(
    standings: List<StandingRow>,
    modifier: Modifier = Modifier,
    onTeamClick: ((teamId: Int, teamName: String) -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier) {
        StandingsHeaderRow(scrollState)

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(standings) { index, row ->
                StandingsDataRow(
                    row = row,
                    isEvenRow = index % 2 == 0,
                    onTeamClick = onTeamClick,
                    scrollState = scrollState
                )
            }
        }
    }
}

@Composable
private fun StandingsHeaderRow(scrollState: androidx.compose.foundation.ScrollState) {
    Row(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell(text = "#", width = COL_POS)
        HeaderCell(text = "Equipo", width = COL_TEAM)
        HeaderCell(text = "Pt", width = COL_STAT)
        HeaderCell(text = "JJ", width = COL_STAT)
        HeaderCell(text = "EG", width = COL_STAT)
        HeaderCell(text = "EP", width = COL_STAT)
        HeaderCell(text = "PG", width = COL_STAT)
        HeaderCell(text = "PP", width = COL_STAT)
        HeaderCell(text = "SG", width = COL_STAT)
        HeaderCell(text = "SP", width = COL_STAT)
        HeaderCell(text = "JG", width = COL_STAT)
        HeaderCell(text = "JP", width = COL_STAT)
    }
}

@Composable
private fun StandingsDataRow(
    row: StandingRow,
    isEvenRow: Boolean,
    onTeamClick: ((teamId: Int, teamName: String) -> Unit)? = null,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val rowBackground = if (isEvenRow) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .background(rowBackground)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position - first column
        Text(
            text = row.position.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.width(COL_POS)
        )

        // Team name - second column, explicitly white/black for visibility
        Text(
            text = row.teamName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            textDecoration = if (onTeamClick != null) TextDecoration.Underline else TextDecoration.None,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .width(COL_TEAM)
                .then(
                    if (onTeamClick != null) {
                        Modifier.clickable { onTeamClick(row.teamId, row.teamName) }
                    } else {
                        Modifier
                    }
                )
        )

        // Stats columns
        DataCell(text = row.points.toString(), width = COL_STAT, fontWeight = FontWeight.Bold)
        DataCell(text = row.matchesPlayed.toString(), width = COL_STAT)
        DataCell(text = row.encountersWon.toString(), width = COL_STAT)
        DataCell(text = row.encountersLost.toString(), width = COL_STAT)
        DataCell(text = row.matchesWon.toString(), width = COL_STAT)
        DataCell(text = row.matchesLost.toString(), width = COL_STAT)
        DataCell(text = row.setsWon.toString(), width = COL_STAT)
        DataCell(text = row.setsLost.toString(), width = COL_STAT)
        DataCell(text = row.gamesWon.toString(), width = COL_STAT)
        DataCell(text = row.gamesLost.toString(), width = COL_STAT)
    }
}

@Composable
private fun HeaderCell(
    text: String,
    width: Dp
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSecondary,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun DataCell(
    text: String,
    width: Dp,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = fontWeight,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.width(width)
    )
}
