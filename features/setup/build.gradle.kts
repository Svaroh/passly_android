plugins {
    id("passbolt.android.library")
    id(libs.plugins.kotlin.serialization.get().pluginId)
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":architecture"))
    implementation(project(":coreui"))
    implementation(project(":common"))
    implementation(project(":navigation"))
    implementation(project(":qrscan"))
    implementation(project(":mobiletransfer-domain"))
    implementation(project(":uimodel"))
    implementation(project(":dto"))
    implementation(project(":gopenpgp"))
    implementation(project(":autofill"))
    implementation(project(":autofillresources"))
    implementation(project(":authentication"))
    implementation(project(":security"))
    implementation(project(":logger"))
    implementation(project(":localization"))
    implementation(project(":helpmenu"))
    implementation(project(":logs"))
    implementation(project(":accounts"))
    implementation(project(":biometrickey-domain"))
    implementation(project(":privatekey-domain"))
    implementation(project(":passphrasememorycache"))
    implementation(project(":encryptedstorage"))
    implementation(project(":auth-domain"))
    implementation(project(":preferences-domain"))
    implementation(project(":main"))
    implementation(project(":testtags"))
    implementation(project(":accessibilitypolicies"))

    implementation(libs.biometric)
    implementation(libs.camerax.view)
    implementation(libs.camerax)
    implementation(libs.gson)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.kotlin.serializationjson)
    implementation(libs.okio)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
    implementation(libs.compose.icons)

    testImplementation(libs.kotlin.serializationjson)
    testImplementation(project(":commontest"))
}

android {
    namespace = "net.svaroh.passly.feature.setup"
    buildFeatures {
        compose = true
    }
}
