package com.padelaragon.shared.util

import android.util.Log

actual object Logger {
    actual var enabled: Boolean = false

    actual fun d(tag: String, msg: String) {
        if (enabled) Log.d(tag, msg)
    }

    actual fun w(tag: String, msg: String, t: Throwable?) {
        if (enabled) Log.w(tag, msg, t)
    }

    actual fun e(tag: String, msg: String, t: Throwable?) {
        if (enabled) Log.e(tag, msg, t)
    }
}
