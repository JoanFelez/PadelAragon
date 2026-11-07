plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}

application {
    mainClass = "com.padelaragon.scraper.MainKt"
}

tasks.test {
    systemProperty("contracts.dir", rootProject.file("contracts/fixtures").absolutePath)
}
