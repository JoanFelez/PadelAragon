package com.padelaragon.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.padelaragon.shared.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Padel Aragón") {
        App()
    }
}
