val versionMajor = (property("appVersionMajor") as String).toInt()
val versionMinor = (property("appVersionMinor") as String).toInt()
val versionPatch = (property("appVersionPatch") as String).toInt()

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.padelaragon.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.padelaragon.app"
        minSdk = 26
        targetSdk = 36
        versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
        versionName = "$versionMajor.$versionMinor.$versionPatch"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing is opt-in: only applied when the keystore is provided via
    // environment variables or gradle properties (CI release workflow).
    fun signingValue(name: String): String? =
        System.getenv(name)?.takeIf { it.isNotBlank() }
            ?: (findProperty(name) as String?)?.takeIf { it.isNotBlank() }

    val releaseStoreFile = signingValue("ANDROID_KEYSTORE_FILE")
    val hasReleaseSigning = releaseStoreFile != null &&
        signingValue("ANDROID_KEYSTORE_PASSWORD") != null &&
        signingValue("ANDROID_KEY_ALIAS") != null &&
        signingValue("ANDROID_KEY_PASSWORD") != null

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = signingValue("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = signingValue("ANDROID_KEY_ALIAS")
                keyPassword = signingValue("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // Let android.util.Log and other android.jar stubs return
            // defaults (0/null/false) instead of throwing, so pure-JVM
            // parser tests work without Robolectric.
            isReturnDefaultValues = true
        }
    }
}

base {
    archivesName.set("PadelAragon-v$versionMajor.$versionMinor.$versionPatch")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.03.00")

    implementation(project(":shared"))
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("com.google.android.material:material:1.13.0")


    implementation("com.google.android.gms:play-services-base:18.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // --- Local JVM test stack (app/src/test) ---
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
