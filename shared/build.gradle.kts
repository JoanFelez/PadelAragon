import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.ksp)
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}

// Empty default means scrape-only mode; never commit values (local.properties is gitignored).
fun backendDefault(propKey: String, envKey: String): String =
    localProps.getProperty(propKey)?.takeIf { it.isNotBlank() } ?: System.getenv(envKey).orEmpty()

val appVersion = "${property("appVersionMajor")}.${property("appVersionMinor")}.${property("appVersionPatch")}"

kotlin {
    jvmToolchain(21)

    androidLibrary {
        namespace = "com.padelaragon.shared"
        compileSdk = 37
        minSdk = 26
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
            api(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.ktor.client.core)
            implementation(libs.ksoup)
            api(libs.multiplatform.settings)
            implementation(compose.materialIconsExtended)
            implementation(libs.navigation.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.adaptive)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.adaptive.layout)
            implementation(libs.adaptive.navigation)
            implementation(libs.material3.adaptive.navigation.suite)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.core.ktx)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.okhttp)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
                implementation(libs.multiplatform.settings.test)
            }
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
}

tasks.withType<Test>().configureEach {
    useJUnit()
    systemProperty("contracts.dir", rootProject.file("contracts/fixtures").absolutePath)
}

buildkonfig {
    packageName = "com.padelaragon.shared"
    defaultConfigs {
        buildConfigField(Type.STRING, "APP_VERSION", appVersion)
        buildConfigField(Type.STRING, "BACKEND_URL", backendDefault("backend.url", "PADEL_BACKEND_URL"))
        buildConfigField(Type.STRING, "APP_KEY", backendDefault("backend.appKey", "PADEL_APP_KEY"))
    }
}
