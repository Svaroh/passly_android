plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":uimodel"))
    implementation(project(":coreui"))
    implementation(project(":architecture"))
    implementation(project(":authentication"))
    implementation(project(":database"))
    implementation(project(":groupdetails"))
    implementation(project(":mappers"))
    implementation(project(":navigation"))
    implementation(project(":fulldatarefresh"))
    implementation(project(":folders-domain"))
    implementation(project(":resources-domain"))
    implementation(project(":localization"))
    implementation(project(":jsonmodel"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.sqlite)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.androidx.navigation3.runtime)
    debugImplementation(libs.compose.ui.tooling.preview)

    testImplementation(project(":commontest"))
    testImplementation(libs.gson)
    testImplementation(libs.json.path)
}

android {
    namespace = "net.svaroh.passly.feature.locationdetails"
    buildFeatures {
        compose = true
    }
}
