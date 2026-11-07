package com.padelaragon.shared.data.chat

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Incremental Server-Sent-Events decoder. Feed it text in arbitrary fragments (even mid-line);
 * it returns the payload of every event completed by a blank line.
 */
class SseParser {
    private val buffer = StringBuilder()
    private val data = StringBuilder()
    private var hasData = false

    fun feed(chunk: String): List<String> {
        buffer.append(chunk)
        val events = mutableListOf<String>()
        while (true) {
            val end = buffer.indexOfAny(charArrayOf('\n', '\r'))
            if (end < 0) break
            // A trailing "\r" may be the first half of "\r\n": wait for more input.
            if (buffer[end] == '\r' && end == buffer.length - 1) break
            val line = buffer.substring(0, end)
            val skip = if (buffer[end] == '\r' && buffer[end + 1] == '\n') 2 else 1
            buffer.delete(0, end + skip)
            handleLine(line)?.let(events::add)
        }
        return events
    }

    /** Flushes an event left open by a stream that ended without the final blank line. */
    fun finish(): List<String> {
        val events = mutableListOf<String>()
        if (buffer.isNotEmpty()) {
            handleLine(buffer.toString().trimEnd('\r'))?.let(events::add)
            buffer.clear()
        }
        handleLine("")?.let(events::add)
        return events
    }

    private fun handleLine(line: String): String? {
        if (line.isEmpty()) {
            if (!hasData) return null
            val payload = data.toString()
            data.clear()
            hasData = false
            return payload
        }
        if (line.startsWith(":")) return null
        val name = line.substringBefore(':')
        if (name != "data") return null
        val value = line.substringAfter(':', "").removePrefix(" ")
        if (hasData) data.append('\n')
        data.append(value)
        hasData = true
        return null
    }
}

private val frameJson = Json { ignoreUnknownKeys = true; isLenient = true }

/** Maps a raw SSE payload to a [ChatEvent]; unknown or malformed frames yield null. */
fun parseChatEvent(payload: String): ChatEvent? {
    if (payload.trim() == "[DONE]") return ChatEvent.Done
    val obj = try {
        frameJson.parseToJsonElement(payload) as? JsonObject
    } catch (_: Exception) {
        null
    } ?: return null
    obj["error"]?.jsonPrimitive?.contentOrNull?.let { return ChatEvent.Error(it) }
    obj["delta"]?.let { return (it as? JsonPrimitive)?.contentOrNull?.let(ChatEvent::Delta) }
    (obj["citations"] as? JsonArray)?.let { arr ->
        val items = arr.mapNotNull { el ->
            val c = el as? JsonObject ?: return@mapNotNull null
            val label = c["label"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            Citation(
                type = c["type"]?.jsonPrimitive?.contentOrNull ?: "rule",
                label = label,
                title = c["title"]?.jsonPrimitive?.contentOrNull,
                tool = c["tool"]?.jsonPrimitive?.contentOrNull
            )
        }
        return ChatEvent.Citations(items)
    }
    return null
}
