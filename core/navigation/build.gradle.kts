plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
    id(libs.plugins.kotlin.serialization.get().pluginId)
}

android {
    namespace = "net.svaroh.passly.core.navigation"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":localization"))
    implementation(project(":common"))
    implementation(project(":uimodel"))

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlin.serializationjson)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
}
