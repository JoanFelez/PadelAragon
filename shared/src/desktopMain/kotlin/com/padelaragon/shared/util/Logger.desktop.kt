package com.padelaragon.shared.util

actual object Logger {
    actual var enabled: Boolean = System.getProperty("padelaragon.debug") == "true"

    actual fun d(tag: String, msg: String) {
        if (enabled) println("D/$tag: $msg")
    }

    actual fun w(tag: String, msg: String, t: Throwable?) {
        if (enabled) println("W/$tag: $msg${t?.let { " - ${it.message}" } ?: ""}")
    }

    actual fun e(tag: String, msg: String, t: Throwable?) {
        if (enabled) println("E/$tag: $msg${t?.let { " - ${it.message}" } ?: ""}")
    }
}
