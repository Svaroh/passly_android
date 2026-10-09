plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":accounts-domain"))
    api(project(":architecture"))
    implementation(project(":uimodel"))
    implementation(project(":coreui"))
    implementation(project(":authentication"))
    implementation(project(":common"))
    implementation(project(":mappers"))
    implementation(project(":localization"))
    implementation(project(":security"))
    implementation(project(":transferaccounttoanotherdevice"))
    implementation(project(":navigation"))
    implementation(project(":users-domain"))

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.coil.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)

    testImplementation(project(":commontest"))
}

android {
    namespace = "net.svaroh.passly.feature.accountdetails"
    buildFeatures {
        compose = true
    }
}
