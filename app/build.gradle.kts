val versionMajor = 1
val versionMinor = 2
val versionPatch = 0

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.padelaragon.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.padelaragon.app"
        minSdk = 26
        targetSdk = 36
        versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
        versionName = "$versionMajor.$versionMinor.$versionPatch"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

base {
    archivesName.set("PadelAragon-v$versionMajor.$versionMinor.$versionPatch")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.03.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("com.google.android.material:material:1.13.0")

    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    implementation("org.jsoup:jsoup:1.22.1")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.google.android.gms:play-services-base:18.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
