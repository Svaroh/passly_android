plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":architecture"))
    implementation(project(":common"))
    implementation(project(":uimodel"))
    implementation(project(":resources-domain"))
    implementation(project(":authentication"))
    implementation(project(":fulldatarefresh"))
    implementation(project(":coreui"))
    implementation(project(":localization"))
    implementation(project(":idlingresource"))
    implementation(project(":jsonmodel"))

    implementation(libs.androidx.core)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.espresso.idling.resource)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)

    testImplementation(project(":commontest"))
}

android {
    namespace = "net.svaroh.passly.feature.otpmoremenu"
    buildFeatures {
        compose = true
    }
}
