package com.padelaragon.shared

expect val platformName: String

val appVersion: String get() = BuildKonfig.APP_VERSION
