package com.padelaragon.shared.util

/**
 * Diagnostic logging. Silent unless [enabled] is set (Android: debuggable builds only;
 * desktop: `-Dpadelaragon.debug=true`).
 */
expect object Logger {
    var enabled: Boolean
    fun d(tag: String, msg: String)
    fun w(tag: String, msg: String, t: Throwable? = null)
    fun e(tag: String, msg: String, t: Throwable? = null)
}
