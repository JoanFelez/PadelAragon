package com.padelaragon.shared.ui.viewmodel

import com.padelaragon.shared.data.chat.ChatApi
import com.padelaragon.shared.data.chat.ChatMessage
import com.padelaragon.shared.data.chat.ChatPreferences
import com.padelaragon.shared.data.chat.ChatRole
import com.padelaragon.shared.data.chat.ChatStore
import com.padelaragon.shared.data.chat.ChatFailure
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.sync.BackendConfig
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private class FakeStore : ChatStore {
        private val state = MutableStateFlow<List<ChatMessage>>(emptyList())
        private var next = 1L
        override val messages = state
        override suspend fun add(message: ChatMessage): Long {
            val id = next++
            state.value = (state.value + message.copy(id = id)).takeLast(50)
            return id
        }
        override suspend fun remove(id: Long) { state.value = state.value.filterNot { it.id == id } }
        override suspend fun clear() { state.value = emptyList() }
    }

    private val config = BackendConfig("https://api.example.invalid", "k")
    private val store = FakeStore()
    private val requests = mutableListOf<HttpRequestData>()
    private val prefs = ChatPreferences(MapSettings())

    // The mock engine and the client run on real threads, so wait on wall-clock time.
    private suspend fun ChatViewModel.idle() {
        withContext(Dispatchers.Default) { withTimeout(5_000) { ui.first { !it.isBusy } } }
    }

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm(cfg: BackendConfig = config, handler: MockRequestHandler) =
        ChatViewModel(
            store, ChatApi(HttpClient(MockEngine { req -> requests += req; handler(req) })), { cfg }, prefs,
            favoritesFor = { listOf(7) }, now = { 1L }
        )

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.sseOk(body: String) = respond(
        body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/event-stream")
    )

    @Test
    fun `streams deltas then persists answer with citations`() = runTest {
        val vm = vm { sseOk(
            "data: {\"delta\":\"Hola \"}\n\ndata: {\"delta\":\"**mundo**\"}\n\n" +
                "data: {\"citations\":[{\"type\":\"rule\",\"label\":\"Art. 1\"}]}\n\ndata: [DONE]\n\n"
        ) }
        vm.idle()
        vm.send("  ¿Qué tal?  ")
        vm.idle()
        val ui = vm.ui.value
        assertNull(ui.streaming)
        assertNull(ui.error)
        assertEquals(listOf(ChatRole.USER, ChatRole.ASSISTANT), ui.messages.map { it.role })
        assertEquals("¿Qué tal?", ui.messages[0].content)
        assertEquals("Hola **mundo**", ui.messages[1].content)
        assertEquals("Art. 1", ui.messages[1].citations.single().label)
    }

    @Test
    fun `not configured shows error without calling the network or storing`() = runTest {
        val vm = vm(BackendConfig("", "")) { error("must not be called") }
        vm.idle()
        vm.send("hola")
        vm.idle()
        assertEquals(ChatFailure.NOT_CONFIGURED, vm.ui.value.error)
        assertTrue(vm.ui.value.messages.isEmpty())
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `quota error keeps the question and retry succeeds`() = runTest {
        var calls = 0
        val vm = vm {
            if (calls++ == 0) respond("{}", HttpStatusCode.TooManyRequests)
            else sseOk("data: {\"delta\":\"ok\"}\n\ndata: [DONE]\n\n")
        }
        vm.idle()
        vm.send("pregunta")
        vm.idle()
        assertEquals(ChatFailure.QUOTA, vm.ui.value.error)
        assertTrue(vm.ui.value.canRetry)
        vm.retry()
        vm.idle()
        assertNull(vm.ui.value.error)
        assertEquals(listOf("pregunta", "ok"), vm.ui.value.messages.map { it.content })
    }

    @Test
    fun `stream error frame discards partial text and flags failure`() = runTest {
        val vm = vm { sseOk("data: {\"delta\":\"par\"}\n\ndata: {\"error\":\"provider_failed\"}\n\ndata: [DONE]\n\n") }
        vm.idle()
        vm.send("q")
        vm.idle()
        assertEquals(ChatFailure.UNAVAILABLE, vm.ui.value.error)
        assertEquals(1, vm.ui.value.messages.size)
    }

    @Test
    fun `stop cancels the stream and keeps the question retryable`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val vm = vm {
            val channel = ByteChannel(autoFlush = true)
            channel.writeStringUtf8("data: {\"delta\":\"parcial\"}\n\n")
            gate.await()
            channel.close()
            respond(channel, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/event-stream"))
        }
        vm.idle()
        vm.send("q")
        assertTrue(vm.ui.value.isBusy)
        vm.stop()
        vm.idle()
        gate.complete(Unit)
        assertTrue(!vm.ui.value.isBusy)
        assertEquals(listOf("q"), vm.ui.value.messages.map { it.content })
        assertTrue(vm.ui.value.canRetry)
    }

    @Test
    fun `request carries league context and only the last 10 truncated turns`() = runTest {
        prefs.leagueId = League.ABSOLUTA.id
        repeat(12) {
            store.add(ChatMessage(role = if (it % 2 == 0) ChatRole.USER else ChatRole.ASSISTANT, content = "x".repeat(1500)))
        }
        val vm = vm { sseOk("data: [DONE]\n\n") }
        vm.idle()
        vm.send("nueva")
        vm.idle()
        val json = Json.parseToJsonElement((requests.single().body as TextContent).text).jsonObject
        val messages = json["messages"]!!.jsonArray
        assertEquals(10, messages.size)
        assertEquals("nueva", messages.last().jsonObject["content"]!!.jsonPrimitive.content)
        assertTrue(messages.all { it.jsonObject["content"]!!.jsonPrimitive.content.length <= 1000 })
        assertEquals(27951, json["leagueId"]!!.jsonPrimitive.int)
        assertEquals(7, json["favoriteGroupIds"]!!.jsonArray[0].jsonPrimitive.int)
    }

    @Test
    fun `new conversation clears history`() = runTest {
        val vm = vm { sseOk("data: {\"delta\":\"a\"}\n\ndata: [DONE]\n\n") }
        vm.idle()
        vm.send("q")
        vm.idle()
        vm.newConversation()
        vm.idle()
        assertTrue(vm.ui.value.messages.isEmpty())
    }
}
