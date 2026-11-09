package com.padelaragon.shared.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikepenz.markdown.m3.Markdown
import com.padelaragon.shared.data.chat.ChatFailure
import com.padelaragon.shared.data.chat.ChatLimits
import com.padelaragon.shared.data.chat.ChatMessage
import com.padelaragon.shared.data.chat.ChatRole
import com.padelaragon.shared.data.chat.Citation
import com.padelaragon.shared.di.MultiLeagueContainer
import com.padelaragon.shared.ui.viewmodel.ChatViewModel
import com.padelaragon.shared.ui.viewmodel.ChatViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(container: MultiLeagueContainer, onOpenSettings: () -> Unit) {
    val vm: ChatViewModel = viewModel(factory = remember { ChatViewModelFactory(container) })
    val state by vm.ui.collectAsState()
    val leagues by container.leagues.collectAsState()
    var draft by remember { mutableStateOf("") }
    var leagueMenu by remember { mutableStateOf(false) }

    val send = {
        if (draft.isNotBlank() && !state.isBusy) {
            vm.send(draft)
            draft = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asistente") },
                actions = {
                    Box {
                        TextButton(onClick = { leagueMenu = true }) {
                            Text(leagues.firstOrNull { it.id == state.leagueId }?.displayName ?: "Sin liga")
                        }
                        DropdownMenu(expanded = leagueMenu, onDismissRequest = { leagueMenu = false }) {
                            DropdownMenuItem(text = { Text("Sin liga") }, onClick = { vm.selectLeague(null); leagueMenu = false })
                            leagues.forEach { league ->
                                DropdownMenuItem(
                                    text = { Text(league.displayName) },
                                    onClick = { vm.selectLeague(league); leagueMenu = false }
                                )
                            }
                        }
                    }
                    IconButton(onClick = vm::newConversation, enabled = state.messages.isNotEmpty() || state.isBusy) {
                        Icon(Icons.Filled.AddComment, contentDescription = "Nueva conversación")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (state.messages.isEmpty() && !state.isBusy) {
                    StarterQuestionsView(onPick = vm::send)
                } else {
                    MessageList(state, vm)
                }
            }
            state.error?.let { failure ->
                ErrorBanner(
                    failure = failure,
                    canRetry = state.canRetry,
                    onRetry = vm::retry,
                    onOpenSettings = onOpenSettings,
                    onDismiss = vm::dismissError
                )
            }
            if (state.error == null && state.canRetry) {
                TextButton(onClick = vm::retry, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Reintentar respuesta") }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(ChatLimits.MAX_MESSAGE_CHARS) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Pregunta sobre el reglamento o la liga") },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() })
                )
                if (state.isBusy) {
                    IconButton(onClick = vm::stop) { Icon(Icons.Filled.Stop, contentDescription = "Detener") }
                } else {
                    IconButton(onClick = { send() }, enabled = draft.isNotBlank()) {
                        Icon(Icons.Filled.Send, contentDescription = "Enviar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StarterQuestionsView(onPick: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("¿En qué puedo ayudarte?", style = MaterialTheme.typography.titleLarge)
        Text(
            "Respondo con el reglamento y los datos de la liga. Puedo equivocarme: contrasta lo importante con la federación.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StarterQuestions.forEach { q -> SuggestionChip(onClick = { onPick(q) }, label = { Text(q) }) }
        }
    }
}

@Composable
private fun MessageList(state: ChatViewModel.UiState, vm: ChatViewModel) {
    val listState = rememberLazyListState()
    val streamingText = state.streaming?.text.orEmpty()
    val lastIndex = state.messages.size + (if (state.streaming != null) 1 else 0) - 1
    LaunchedEffect(state.messages.size, state.streaming != null, streamingText.length) {
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.messages, key = { it.id }) { MessageBubble(it.role, it.content, it.citations) }
        state.streaming?.let { s ->
            item(key = "streaming") { MessageBubble(ChatRole.ASSISTANT, s.text.ifEmpty { "Pensando…" }, s.citations) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageBubble(role: ChatRole, content: String, citations: List<Citation>) {
    val isUser = role == ChatRole.USER
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isUser) Text(content, style = MaterialTheme.typography.bodyMedium) else Markdown(content)
                if (citations.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        citations.forEach { c ->
                            AssistChip(onClick = {}, label = { Text(c.chipText(), style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
            }
        }
    }
}

internal fun Citation.chipText(): String = title?.takeIf { it.isNotBlank() }?.let { "$label · $it" } ?: label

@Composable
private fun ErrorBanner(
    failure: ChatFailure,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(failure.userMessage(), color = MaterialTheme.colorScheme.onErrorContainer)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (failure == ChatFailure.NOT_CONFIGURED || failure == ChatFailure.UNAUTHORIZED) {
                    Button(onClick = onOpenSettings) { Text("Ir a Ajustes") }
                }
                if (canRetry && failure != ChatFailure.NOT_CONFIGURED) {
                    Button(onClick = onRetry) { Text("Reintentar") }
                }
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        }
    }
}
