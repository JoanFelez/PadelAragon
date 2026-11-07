package com.padelaragon.shared.ui.chat

import com.padelaragon.shared.data.chat.ChatFailure

val StarterQuestions = listOf(
    "¿Cuántos sets se juegan en un partido y cómo se puntúa?",
    "¿Qué dice el reglamento sobre el punto de oro?",
    "¿Cómo se desempata en la clasificación de la liga?",
    "¿Cuántos jugadores debe tener un equipo para jugar una jornada?",
    "¿Qué equipos lideran la clasificación ahora mismo?"
)

fun ChatFailure.userMessage(): String = when (this) {
    ChatFailure.NOT_CONFIGURED ->
        "El asistente necesita configurar el servidor. Indica la URL y la clave en Ajustes."
    ChatFailure.OFFLINE -> "No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo."
    ChatFailure.UNAUTHORIZED -> "El servidor ha rechazado la clave. Revisa la clave en Ajustes."
    ChatFailure.QUOTA -> "Límite gratuito alcanzado. Inténtalo más tarde."
    ChatFailure.UNAVAILABLE ->
        "El asistente no está disponible ahora mismo (puede que se haya alcanzado el límite gratuito). Inténtalo más tarde."
    ChatFailure.INVALID -> "No se pudo enviar la pregunta. Prueba con un mensaje más corto."
    ChatFailure.UNKNOWN -> "Ha ocurrido un error inesperado. Inténtalo de nuevo."
}
