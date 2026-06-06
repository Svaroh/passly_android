plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":resourcetypes-domain"))
    implementation(project(":common"))
    implementation(project(":coreui"))
    implementation(project(":localization"))
    implementation(project(":featureflags-domain"))
    implementation(project(":uimodel"))
    implementation(project(":architecture"))
    implementation(project(":idlingresource"))
    implementation(project(":entity"))
    implementation(project(":supportedresourcetypes"))
    implementation(project(":metadata-domain"))
    implementation(project(":navigation"))

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
    implementation(libs.compose.activity)

    testImplementation(project(":commontest"))
}

android {
    namespace = "net.svaroh.passly.feature.createresourcemenu"
    buildFeatures {
        compose = true
    }
}
