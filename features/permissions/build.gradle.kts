plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
    id(libs.plugins.kotlin.serialization.get().pluginId)
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
    implementation(project(":resources-domain"))
    implementation(project(":groups-domain"))
    implementation(project(":users-domain"))
    implementation(project(":folders-domain"))
    implementation(project(":localization"))
    implementation(project(":jsonmodel"))
    implementation(project(":supportedresourcetypes"))
    implementation(project(":metadata-domain"))
    implementation(project(":serializers"))
    implementation(project(":metadatakeytrust"))
    implementation(project(":permissionsconfirmation-domain"))
    implementation(project(":featureflags-domain"))
    implementation(project(":entity"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.coil.compose)
    implementation(libs.koin.compose)
    implementation(libs.kotlin.serializationjson)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.ui.tooling)
    implementation(libs.androidx.navigation3.runtime)

    debugImplementation(libs.compose.ui.tooling.preview)

    testImplementation(project(":commontest"))
    testImplementation(libs.gson)
    testImplementation(libs.json.path)
}

android {
    namespace = "com.passbolt.mobile.android.feature.permissions"
    buildFeatures {
        compose = true
    }
}
