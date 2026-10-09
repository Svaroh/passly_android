plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":navigation"))
    implementation(project(":architecture"))
    implementation(project(":mappers"))
    implementation(project(":uimodel"))
    implementation(project(":networking"))
    implementation(project(":database"))
    implementation(project(":gopenpgp"))
    implementation(project(":authentication"))
    implementation(project(":coreui"))
    implementation(project(":groups-domain"))
    implementation(project(":users-domain"))
    implementation(project(":permissionsconfirmation-domain"))
    implementation(project(":localization"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.coil.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(libs.androidx.navigation3.runtime)
    debugImplementation(libs.compose.ui.tooling.preview)

    testImplementation(project(":commontest"))
}

android {
    namespace = "net.svaroh.passly.feature.groupdetails"
    buildFeatures {
        compose = true
    }
}
