plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
    id(libs.plugins.kotlin.serialization.get().pluginId)
}

dependencies {
    implementation(project(":authentication"))
    implementation(project(":fulldatarefresh"))
    implementation(project(":architecture"))
    implementation(project(":coreui"))
    implementation(project(":uimodel"))
    implementation(project(":localization"))
    implementation(project(":passwordexpiry-domain"))
    implementation(project(":passwordpolicies-domain"))
    implementation(project(":featureflags-domain"))
    implementation(project(":entity"))
    implementation(project(":passwordgenerator"))
    implementation(project(":common"))
    implementation(project(":mappers"))
    implementation(project(":navigation"))
    implementation(project(":metadata-domain"))
    implementation(project(":supportedresourcetypes"))
    implementation(project(":secrets-domain"))
    implementation(project(":jsonmodel"))
    implementation(project(":resourcetypes"))
    implementation(project(":resources-domain"))
    implementation(project(":folders-domain"))
    implementation(project(":accounts-domain"))
    implementation(project(":permissionsconfirmation-domain"))
    implementation(project(":serializers"))
    implementation(project(":metadatakeytrust"))
    implementation(project(":idlingresource"))
    implementation(project(":testtags"))
    implementation(project("::resourcetypes-domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.espresso.idling.resource)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.ui.tooling)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.kotlin.serializationjson)
    implementation(libs.androidx.navigation3.runtime)

    debugImplementation(libs.compose.ui.tooling.preview)

    testImplementation(project(":commontest"))
    testImplementation(project(":passphrasememorycache"))
    testImplementation(project(":networking"))
    testImplementation(libs.gson)
    testImplementation(libs.json.path)
    testImplementation(libs.json.assert)
}

android {
    namespace = "com.passbolt.mobile.android.feature.resourceform"
    buildFeatures {
        compose = true
    }
}
