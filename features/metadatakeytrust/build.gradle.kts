plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":uimodel"))
    implementation(project(":coreui"))
    implementation(project(":common"))
    implementation(project(":localization"))
    implementation(project(":architecture"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)
}

android {
    namespace = "net.svaroh.passly.feature.metadatakeytrust"
    buildFeatures {
        compose = true
    }
}
