package com.padelaragon.shared.data.chat

import com.padelaragon.shared.data.network.createPlatformHttpClient
import com.padelaragon.shared.data.sync.BackendConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Limits enforced by the Worker (`backend/worker/src/chat/index.ts`). */
object ChatLimits {
    const val MAX_TURNS = 10
    const val MAX_MESSAGE_CHARS = 1000
}

/** Streaming client of `POST /api/chat`, authenticated with `X-App-Key`. */
class ChatApi(
    private val client: HttpClient = createDefaultChatClient(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    /**
     * Cold flow of decoded events. Failures before or during the stream are thrown as
     * [ChatException]; the flow completes after [ChatEvent.Done] or when the server closes.
     */
    fun stream(config: BackendConfig, messages: List<ChatMessage>, context: ChatContext): Flow<ChatEvent> = flow {
        if (!config.isComplete) throw ChatException(ChatFailure.NOT_CONFIGURED)
        val body = buildBody(messages, context)
        try {
            client.preparePost(config.baseUrl + "/api/chat") {
                header("X-App-Key", config.appKey)
                header(HttpHeaders.Accept, "text/event-stream")
                setBody(io.ktor.http.content.TextContent(body, ContentType.Application.Json))
            }.execute { response ->
                val status = response.status.value
                if (status !in 200..299) throw ChatException(failureFor(status), "HTTP $status")
                val channel = response.bodyAsChannel()
                val parser = SseParser()
                var done = false
                suspend fun emitPayloads(payloads: List<String>) {
                    for (p in payloads) {
                        val event = parseChatEvent(p) ?: continue
                        emit(event)
                        if (event is ChatEvent.Done) done = true
                    }
                }
                while (!done) {
                    val line = channel.readUTF8Line() ?: break
                    emitPayloads(parser.feed(line + "\n"))
                }
                if (!done) emitPayloads(parser.finish())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ChatException) {
            throw e
        } catch (e: IOException) {
            throw ChatException(ChatFailure.OFFLINE, e.message, e)
        } catch (e: Exception) {
            throw ChatException(ChatFailure.OFFLINE, e.message, e)
        }
    }.flowOn(dispatcher)

    private fun failureFor(status: Int) = when (status) {
        401, 403 -> ChatFailure.UNAUTHORIZED
        429 -> ChatFailure.QUOTA
        400 -> ChatFailure.INVALID
        else -> ChatFailure.UNAVAILABLE
    }

    internal fun buildBody(messages: List<ChatMessage>, context: ChatContext): String {
        val body: JsonObject = buildJsonObject {
            put("messages", JsonArray(messages.map {
                buildJsonObject {
                    put("role", it.role.wire)
                    put("content", it.content)
                }
            }))
            context.leagueId?.let { id ->
                put("leagueId", id)
                if (context.favoriteGroupIds.isNotEmpty()) {
                    put("favoriteGroupIds", JsonArray(context.favoriteGroupIds.map(::JsonPrimitive)))
                }
            }
        }
        return body.toString()
    }

    companion object {
        fun createDefaultChatClient(): HttpClient = createPlatformHttpClient {
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 60_000
                requestTimeoutMillis = 180_000
            }
            expectSuccess = false
        }
    }
}
