package com.padelaragon.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.padelaragon.app.data.model.Gender
import com.padelaragon.app.data.model.Player
import com.padelaragon.app.data.model.TeamDetail
import com.padelaragon.app.domain.usecase.AgedPlayer
import com.padelaragon.app.domain.usecase.CoupleCombination
import com.padelaragon.app.domain.usecase.GeneratePossibleCouplesUseCase
import java.time.Year

data class PossiblePairsInput(
    val teamDetail: TeamDetail,
    val groupName: String,
    val teamId: Int,
    val teamName: String,
    val groupId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PossiblePairsScreen(
    input: PossiblePairsInput?,
    onBack: () -> Unit,
    onRetry: () -> Unit = onBack
) {
    var selectedNames by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var showResults by rememberSaveable { mutableStateOf(false) }
    val useCase = remember { GeneratePossibleCouplesUseCase() }
    val gender = input?.let { resolveGender(it.groupName, it.teamDetail.category) }
    val eligiblePlayers = input?.teamDetail?.players
        ?.mapNotNull { player -> player.ageOrNull()?.let { age -> player to age } }
        .orEmpty()
    val unavailableAgeCount = (input?.teamDetail?.players?.size ?: 0) - eligiblePlayers.size
    val result = remember(showResults, selectedNames, gender) {
        if (showResults && gender != null) {
            useCase(
                eligiblePlayers
                    .filter { (player, _) -> player.name in selectedNames }
                    .map { (player, age) -> AgedPlayer(player.name, age) },
                gender
            )
        } else {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showResults) "Posibles parejas" else "Seleccionar jugadores",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    OutlinedButton(
                        onClick = { if (showResults) showResults = false else onBack() },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("←")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            input == null -> UnavailablePairsData(
                message = "No se pudieron cargar los datos del equipo para generar parejas.",
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding)
            )
            input.teamDetail.players.isEmpty() -> UnavailablePairsData(
                message = "No hay jugadores disponibles en la plantilla.",
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding)
            )
            gender == null -> UnavailablePairsData(
                message = "No se pudo determinar si la categoría es masculina o femenina.",
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding)
            )
            eligiblePlayers.isEmpty() -> UnavailablePairsData(
                message = "No hay años de nacimiento válidos para calcular las edades.",
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding)
            )
            showResults -> PossiblePairsResults(
                result = result,
                onChangeSelection = { showResults = false },
                modifier = Modifier.padding(innerPadding)
            )
            else -> PossiblePairsSelection(
                players = eligiblePlayers,
                unavailableAgeCount = unavailableAgeCount,
                selectedNames = selectedNames,
                gender = gender,
                onTogglePlayer = { name ->
                    selectedNames = if (name in selectedNames) {
                        selectedNames - name
                    } else {
                        selectedNames + name
                    }
                },
                onGenerate = { showResults = true },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun PossiblePairsSelection(
    players: List<Pair<Player, Int>>,
    unavailableAgeCount: Int,
    selectedNames: List<String>,
    gender: Gender,
    onTogglePlayer: (String) -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val thresholds = if (gender == Gender.MASCULINA) {
                GeneratePossibleCouplesUseCase.MASCULINA_THRESHOLDS
            } else {
                GeneratePossibleCouplesUseCase.FEMENINA_THRESHOLDS
            }
            Text(
                text = "Selecciona entre 6 y 16 jugadores. Se formarán tres parejas con sumas " +
                    "mínimas de ${thresholds.joinToString(", ")} años.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (unavailableAgeCount > 0) {
            item {
                Text(
                    text = "$unavailableAgeCount jugador(es) no se pueden seleccionar por no tener un año de nacimiento válido.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        items(players, key = { (player, _) -> player.name }) { (player, age) ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = player.name in selectedNames,
                        onCheckedChange = { onTogglePlayer(player.name) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(player.name, fontWeight = FontWeight.Medium)
                        Text(
                            text = "$age años · nacimiento ${player.birthYear}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            Button(
                onClick = onGenerate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generar parejas (${selectedNames.size} seleccionados)")
            }
        }
    }
}

@Composable
private fun PossiblePairsResults(
    result: GeneratePossibleCouplesUseCase.Result?,
    onChangeSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (result) {
            is GeneratePossibleCouplesUseCase.Result.Success -> {
                item {
                    Text(
                        text = "${result.combinations.size} combinación(es), ordenadas de más ajustada a más holgada.",
                        fontWeight = FontWeight.Bold
                    )
                }
                items(result.combinations) { combination ->
                    CombinationCard(combination)
                }
            }
            GeneratePossibleCouplesUseCase.Result.NotEnoughPlayersSelected,
            GeneratePossibleCouplesUseCase.Result.TooManyPlayersSelected -> {
                item {
                    Text(
                        text = "La selección debe contener entre 6 y 16 jugadores.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            GeneratePossibleCouplesUseCase.Result.NoCombinationsPossible -> {
                item {
                    Text(
                        text = "No hay ninguna combinación posible con los jugadores seleccionados.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            null -> {
                item {
                    Text(
                        text = "No se pudieron calcular las posibles parejas.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        item {
            OutlinedButton(onClick = onChangeSelection, modifier = Modifier.fillMaxWidth()) {
                Text("Modificar selección")
            }
        }
    }
}

@Composable
private fun CombinationCard(combination: CoupleCombination) {
    Card {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            combination.pairs.forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${pair.player1.name} + ${pair.player2.name}",
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${pair.ageSum} / ${pair.requiredSum}",
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun UnavailablePairsData(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

private fun resolveGender(groupName: String, category: String?): Gender? {
    val text = "$groupName ${category.orEmpty()}".uppercase()
    return when {
        text.contains("FEMENINA") -> Gender.FEMENINA
        text.contains("MASCULINA") -> Gender.MASCULINA
        else -> null
    }
}

private fun Player.ageOrNull(): Int? {
    val birthYear = birthYear?.toIntOrNull() ?: return null
    val currentYear = Year.now().value
    return (currentYear - birthYear).takeIf { birthYear in 1900..currentYear }
}
