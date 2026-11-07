plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    testImplementation(kotlin("test"))
}

application {
    mainClass = "com.padelaragon.scraper.MainKt"
}
