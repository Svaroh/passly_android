plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
    alias(libs.plugins.screenshot)
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":architecture"))
    implementation(project(":navigation"))
    implementation(project(":coreui"))
    implementation(project(":common"))
    implementation(project(":autofill"))
    implementation(project(":autofillresources"))
    implementation(project(":authentication"))
    implementation(project(":mappers"))
    implementation(project(":featureflags-domain"))
    implementation(project(":uimodel"))
    implementation(project(":logger"))
    implementation(project(":localization"))
    implementation(project(":entity"))
    implementation(project(":users-domain"))
    implementation(project(":transferaccounttoanotherdevice"))
    implementation(project(":gopenpgp"))
    implementation(project(":accountdetails"))
    implementation(project(":fulldatarefresh"))
    implementation(project(":biometrickey-domain"))
    implementation(project(":privatekey-domain"))
    implementation(project(":passphrasememorycache"))
    implementation(project(":auth-domain"))
    implementation(project(":encryptedstorage"))
    implementation(project(":preferences-domain"))
    implementation(project(":clipboard"))
    implementation(project(":accessibilitypolicies"))
    implementation(project(":testtags"))

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.biometric)
    implementation(libs.gson)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.icons)
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.activity)

    testImplementation(project(":commontest"))

    screenshotTestImplementation(project(":screenshottest"))
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.compose.ui.tooling)
}

android {
    namespace = "com.passbolt.mobile.android.feature.settings"
    buildFeatures {
        compose = true
    }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    // tolerate sub-pixel antialiasing differences between macOS (dev) and Linux (CI) layoutlib renderers
    screenshotTests {
        imageDifferenceThreshold = 0.001f
    }
}
