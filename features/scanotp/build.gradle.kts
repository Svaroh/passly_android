plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
    id(libs.plugins.kotlin.serialization.get().pluginId)
}

dependencies {
    implementation(project(":resourcetypes-domain"))
    implementation(project(":architecture"))
    implementation(project(":uimodel"))
    implementation(project(":coreui"))
    implementation(project(":common"))
    implementation(project(":localization"))
    implementation(project(":navigation"))
    implementation(project(":qrscan"))
    implementation(project(":otpcore"))
    implementation(project(":security"))
    implementation(project(":authentication"))
    implementation(project(":resourcetypes"))
    implementation(project(":resources-domain"))
    implementation(project(":serializers"))
    implementation(project(":supportedresourcetypes"))
    implementation(project(":jsonmodel"))
    implementation(project(":secrets-domain"))
    implementation(project(":metadatakeytrust"))
    implementation(project(":metadata-domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.camerax.view)
    implementation(libs.camerax)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.activity)
    debugImplementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    testImplementation(project(":commontest"))
    testImplementation(libs.gson)
    testImplementation(libs.json.path)
}

android {
    namespace = "net.svaroh.passly.feature.scanotp"
    buildFeatures {
        compose = true
    }
}
