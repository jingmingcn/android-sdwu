// Top-level build file. Plugins are declared here and applied in :app.
// AGP 9 has built-in Kotlin support, so the kotlin-android plugin is not needed.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.hilt) apply false
}
