package com.padelaragon.shared.data.chat

import com.padelaragon.shared.data.sync.BackendConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.writeStringUtf8
import io.ktor.utils.io.close
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChatApiTest {
    private val config = BackendConfig("https://api.example.invalid", "secret")
    private val sse = headersOf(HttpHeaders.ContentType, "text/event-stream")
    private val question = listOf(ChatMessage(role = ChatRole.USER, content = "¿Hola?"))

    private fun parse(vararg chunks: String): List<ChatEvent> {
        val parser = SseParser()
        return (chunks.flatMap { parser.feed(it) } + parser.finish()).mapNotNull(::parseChatEvent)
    }

    @Test
    fun `parser reassembles frames split across chunks`() {
        val events = parse("data: {\"del", "ta\":\"Hol", "a\"}\n", "\ndata: {\"delta\":\" ñ\"}\r", "\n\r\n", "data: [DONE]\n\n")
        assertEquals(listOf(ChatEvent.Delta("Hola"), ChatEvent.Delta(" ñ"), ChatEvent.Done), events)
    }

    @Test
    fun `parser decodes citations error and ignores comments and unknown frames`() {
        val events = parse(
            ": keep-alive\n\n",
            "data: {\"citations\":[{\"type\":\"rule\",\"label\":\"Art. 5\",\"title\":\"Puntuación\"},{\"type\":\"data\",\"tool\":\"getStandings\",\"label\":\"Grupo 1\"}]}\n\n",
            "data: {\"other\":1}\n\n",
            "data: not json\n\n",
            "data: {\"error\":\"provider_failed\"}\n\n"
        )
        val citations = assertIs<ChatEvent.Citations>(events[0]).items
        assertEquals(listOf("Art. 5", "Grupo 1"), citations.map { it.label })
        assertEquals("getStandings", citations[1].tool)
        assertEquals(ChatEvent.Error("provider_failed"), events[1])
        assertEquals(2, events.size)
    }

    @Test
    fun `unterminated final event is flushed`() {
        assertEquals(listOf(ChatEvent.Delta("x")), parse("data: {\"delta\":\"x\"}"))
    }

    @Test
    fun `stream sends key and body and emits events until DONE`() = runBlocking {
        var request: HttpRequestData? = null
        val client = HttpClient(MockEngine { req ->
            request = req
            val channel = ByteChannel(autoFlush = true)
            val body = "data: {\"delta\":\"Hola\"}\n\ndata: {\"citations\":[]}\n\ndata: [DONE]\n\ndata: {\"delta\":\"ignored\"}\n\n"
            channel.writeStringUtf8(body)
            channel.close()
            respond(channel, HttpStatusCode.OK, sse)
        })
        val events = ChatApi(client)
            .stream(config, question, ChatContext(27951, listOf(1, 2))).toList()

        assertEquals(listOf(ChatEvent.Delta("Hola"), ChatEvent.Citations(emptyList()), ChatEvent.Done), events)
        val sent = request!!
        assertEquals("secret", sent.headers["X-App-Key"])
        assertEquals("https://api.example.invalid/api/chat", sent.url.toString())
        val json = Json.parseToJsonElement((sent.body as TextContent).text).jsonObject
        assertEquals("user", json["messages"]!!.jsonArray[0].jsonObject["role"]!!.jsonPrimitive.content)
        assertEquals(27951, json["leagueId"]!!.jsonPrimitive.int)
        assertEquals(2, json["favoriteGroupIds"]!!.jsonArray.size)
    }

    @Test
    fun `http statuses map to failures`() = runBlocking {
        suspend fun failure(status: HttpStatusCode): ChatFailure {
            val api = ChatApi(HttpClient(MockEngine { respond("{}", status) }))
            return assertFailsWith<ChatException> { api.stream(config, question, ChatContext()).toList() }.failure
        }
        assertEquals(ChatFailure.UNAUTHORIZED, failure(HttpStatusCode.Unauthorized))
        assertEquals(ChatFailure.QUOTA, failure(HttpStatusCode.TooManyRequests))
        assertEquals(ChatFailure.UNAVAILABLE, failure(HttpStatusCode.ServiceUnavailable))
        assertEquals(ChatFailure.INVALID, failure(HttpStatusCode.BadRequest))
    }

    @Test
    fun `unconfigured backend and network errors`() = runBlocking {
        val api = ChatApi(HttpClient(MockEngine { throw java.io.IOException("down") }))
        assertEquals(
            ChatFailure.NOT_CONFIGURED,
            assertFailsWith<ChatException> { api.stream(BackendConfig("", ""), question, ChatContext()).toList() }.failure
        )
        assertEquals(
            ChatFailure.OFFLINE,
            assertFailsWith<ChatException> { api.stream(config, question, ChatContext()).toList() }.failure
        )
        assertTrue(true)
    }
}
