package com.padelaragon.shared.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.padelaragon.shared.data.chat.ChatApi
import com.padelaragon.shared.data.chat.ChatContext
import com.padelaragon.shared.data.chat.ChatEvent
import com.padelaragon.shared.data.chat.ChatException
import com.padelaragon.shared.data.chat.ChatFailure
import com.padelaragon.shared.data.chat.ChatLimits
import com.padelaragon.shared.data.chat.ChatMessage
import com.padelaragon.shared.data.chat.ChatPreferences
import com.padelaragon.shared.data.chat.ChatRole
import com.padelaragon.shared.data.chat.ChatStore
import com.padelaragon.shared.data.chat.Citation
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.sync.BackendConfig
import com.padelaragon.shared.di.MultiLeagueContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class ChatViewModel(
    private val store: ChatStore,
    private val api: ChatApi,
    private val config: () -> BackendConfig,
    private val preferences: ChatPreferences,
    private val favoritesFor: (leagueId: Int) -> List<Int> = { emptyList() },
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : ViewModel() {

    /** The answer being streamed; never persisted until the stream ends or is stopped. */
    data class Streaming(val text: String = "", val citations: List<Citation> = emptyList())

    data class UiState(
        val messages: List<ChatMessage> = emptyList(),
        val streaming: Streaming? = null,
        val error: ChatFailure? = null,
        val leagueId: Int? = null
    ) {
        val isBusy: Boolean get() = streaming != null
        /** The last question has no answer (failed or stopped), so it can be asked again. */
        val canRetry: Boolean get() = !isBusy && messages.lastOrNull()?.role == ChatRole.USER
    }

    private val streaming = MutableStateFlow<Streaming?>(null)
    private val error = MutableStateFlow<ChatFailure?>(null)
    private val leagueId = MutableStateFlow(preferences.leagueId)
    private var job: Job? = null

    val ui: StateFlow<UiState> = combine(store.messages, streaming, error, leagueId) { m, s, e, l ->
        UiState(m, s, e, l)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState(leagueId = leagueId.value))

    fun send(text: String) {
        val question = text.trim().take(ChatLimits.MAX_MESSAGE_CHARS)
        if (question.isEmpty() || streaming.value != null) return
        if (!config().isComplete) {
            error.value = ChatFailure.NOT_CONFIGURED
            return
        }
        error.value = null
        streaming.value = Streaming()
        job = viewModelScope.launch {
            store.add(ChatMessage(role = ChatRole.USER, content = question, createdAt = now()))
            runStream()
        }
    }

    fun retry() {
        if (!ui.value.canRetry) return
        if (!config().isComplete) {
            error.value = ChatFailure.NOT_CONFIGURED
            return
        }
        error.value = null
        streaming.value = Streaming()
        job = viewModelScope.launch { runStream() }
    }

    /** Stops the stream, keeping whatever text already arrived. */
    fun stop() {
        val running = job ?: return
        val partial = streaming.value
        running.cancel()
        job = null
        streaming.value = null
        if (partial != null && partial.text.isNotBlank()) {
            viewModelScope.launch { persist(partial) }
        }
    }

    fun newConversation() {
        job?.cancel()
        job = null
        streaming.value = null
        error.value = null
        viewModelScope.launch { store.clear() }
    }

    fun dismissError() {
        error.value = null
    }

    fun selectLeague(league: League?) {
        preferences.leagueId = league?.id
        leagueId.value = league?.id
    }

    private suspend fun runStream() {
        val history = store.messages.first()
            .filter { it.content.isNotBlank() }
            .takeLast(ChatLimits.MAX_TURNS)
            .map { it.copy(content = it.content.take(ChatLimits.MAX_MESSAGE_CHARS)) }
        val league = leagueId.value
        val context = ChatContext(league, league?.let(favoritesFor).orEmpty())
        var current = Streaming()
        var finished = false
        try {
            api.stream(config(), history, context).collect { event ->
                when (event) {
                    is ChatEvent.Delta -> {
                        current = current.copy(text = current.text + event.text)
                        streaming.value = current
                    }
                    is ChatEvent.Citations -> {
                        current = current.copy(citations = event.items)
                        streaming.value = current
                    }
                    is ChatEvent.Error -> throw ChatException(ChatFailure.UNAVAILABLE, event.code)
                    ChatEvent.Done -> finished = true
                }
            }
            if (finished || current.text.isNotBlank()) {
                persist(current)
            } else {
                error.value = ChatFailure.UNAVAILABLE
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ChatException) {
            error.value = e.failure
        } catch (e: Exception) {
            error.value = ChatFailure.UNKNOWN
        } finally {
            streaming.value = null
        }
    }

    private suspend fun persist(answer: Streaming) {
        if (answer.text.isBlank()) return
        store.add(
            ChatMessage(role = ChatRole.ASSISTANT, content = answer.text, citations = answer.citations, createdAt = now())
        )
    }
}

class ChatViewModelFactory(private val container: MultiLeagueContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: kotlin.reflect.KClass<T>, extras: CreationExtras): T {
        require(modelClass == ChatViewModel::class) { "Unknown ViewModel class: $modelClass" }
        return ChatViewModel(
            store = container.chatStore,
            api = container.chatApi,
            config = { container.syncSettings.backendConfig },
            preferences = container.chatPreferences,
            favoritesFor = { id ->
                container.leagueCatalog.byId(id)?.let { container[it].favoritesDataSource.favorites.value.toList() }.orEmpty()
            }
        ) as T
    }
}
