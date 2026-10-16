import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    kotlin("android")
}

val config = Properties().apply {
    rootProject.file("developer.properties").takeIf { it.isFile }
        ?.inputStream()?.use { load(it) }
}
fun setting(name: String, fallback: String) = config.getProperty(name, fallback).trim()
fun quoted(value: String): String {
    require(value.none { it.code < 32 }) { "Configuration values must be single-line strings." }
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
val dayStyle = setting("MAP_STYLE_URL", "https://tiles.openfreemap.org/styles/liberty")
val nightStyle = setting("MAP_NIGHT_STYLE_URL", "https://tiles.openfreemap.org/styles/dark")
val attribution = setting("MAP_ATTRIBUTION", "OpenFreeMap | (c) OpenMapTiles | (c) OpenStreetMap contributors")
require(listOf(dayStyle, nightStyle).all { it.startsWith("https://") }) {
    "Map styles must use HTTPS. Use the in-app Test map button for the bundled offline layout."
}
require(attribution.isNotBlank()) { "MAP_ATTRIBUTION cannot be empty." }

android {
    namespace = "com.haytam.aerodrive"
    compileSdk = 36
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "com.haytam.aerodrive"
        minSdk = 28
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.1-dev"
        buildConfigField("String", "MAP_STYLE_URL", quoted(dayStyle))
        buildConfigField("String", "MAP_NIGHT_STYLE_URL", quoted(nightStyle))
        buildConfigField("String", "MAP_ATTRIBUTION", quoted(attribution))
    }
    buildFeatures { buildConfig = true }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

// Keep the test host validator out of a release APK.
androidComponents {
    beforeVariants(selector().withBuildType("release")) { it.enable = false }
}

dependencies {
    implementation(project(":core"))
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.car.app:app:1.7.0")
    implementation("androidx.car.app:app-projected:1.7.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.9.0")
    implementation("org.maplibre.gl:android-sdk:12.0.1")
}
