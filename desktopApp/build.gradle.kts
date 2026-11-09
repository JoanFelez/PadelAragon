plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

val versionMajor = property("appVersionMajor")
val versionMinor = property("appVersionMinor")
val versionPatch = property("appVersionPatch")

group = "com.padelaragon.desktop"
version = "$versionMajor.$versionMinor.$versionPatch"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "com.padelaragon.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm
            )
            packageName = "PadelAragon"
            packageVersion = "$versionMajor.$versionMinor.$versionPatch"
            // jpackage/jlink trims the bundled runtime image to only the JDK modules
            // jdeps detects as statically referenced. TLS handshakes with Let's
            // Encrypt's ISRG Root X1 chain need EC crypto support (jdk.crypto.ec),
            // which OkHttp/the JDK only pull in reflectively via the JCA provider
            // lookup - jdeps never sees it, so it silently gets stripped from the
            // packaged .exe/.msi. That causes "PKIX path building failed" only in
            // the packaged app, never in `./gradlew run` (which uses the full JDK).
            // Including all modules avoids this class of runtime-image bug.
            includeAllModules = true
            windows {
                menuGroup = "PadelAragon"
                perUserInstall = true
            }
        }
    }
}
