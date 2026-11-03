// Code Snippet Viewer: a module for the Android Teaching Framework.
// It depends only on :framework (contracts + Compose + Material 3). Add other libraries below
// with implementation(...) if your module needs them.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "edu.sdwu.teaching.modules.codesnippets"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":framework"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
