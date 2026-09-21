plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":uimodel"))
    implementation(project(":accounts-domain"))
    implementation(project(":envinfo"))
    implementation(project(":preferences-domain"))
    implementation(project(":coreui"))
    implementation(project(":navigation"))
    implementation(project(":common"))
    implementation(project(":localization"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.splashscreen)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.activity)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)

    testImplementation(project(":commontest"))
}

val androidConfig = extensions.getByType<AndroidCommonConfig>()

android {
    namespace = "com.passbolt.mobile.android.feature.startup"
    defaultConfig {
        buildConfigField("int", "MIN_FULLY_SUPPORTED_SDK", "${androidConfig.minFullySupportedSdk}")
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}
