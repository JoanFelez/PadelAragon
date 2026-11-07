import com.codingfeline.buildkonfig.compiler.FieldSpec.Type

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.buildkonfig)
}

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
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

buildkonfig {
    packageName = "com.padelaragon.shared"
    defaultConfigs {
        buildConfigField(Type.STRING, "APP_VERSION", appVersion)
    }
}
