package com.padelaragon.shared.data.chat

import kotlinx.serialization.Serializable

enum class ChatRole(val wire: String) { USER("user"), ASSISTANT("assistant") }

/** Source of an answer: a rulebook article (`rule`) or a league-data tool result (`data`). */
@Serializable
data class Citation(val type: String = "rule", val label: String, val title: String? = null, val tool: String? = null)

data class ChatMessage(
    val id: Long = 0,
    val role: ChatRole,
    val content: String,
    val citations: List<Citation> = emptyList(),
    val createdAt: Long = 0
)

data class ChatContext(val leagueId: Int? = null, val favoriteGroupIds: List<Int> = emptyList())

/** One decoded `data:` frame of the `/api/chat` SSE stream. */
sealed interface ChatEvent {
    data class Delta(val text: String) : ChatEvent
    data class Citations(val items: List<Citation>) : ChatEvent
    data class Error(val code: String) : ChatEvent
    data object Done : ChatEvent
}

enum class ChatFailure { NOT_CONFIGURED, OFFLINE, UNAUTHORIZED, QUOTA, UNAVAILABLE, INVALID, UNKNOWN }

class ChatException(val failure: ChatFailure, message: String? = null, cause: Throwable? = null) :
    Exception(message ?: failure.name, cause)
