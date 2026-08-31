plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":coreui"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
}

android {
    namespace = "com.passbolt.mobile.android.core.screenshottest"
    buildFeatures {
        compose = true
    }
}
