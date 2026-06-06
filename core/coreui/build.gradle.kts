plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
    alias(libs.plugins.screenshot)
}

dependencies {
    implementation(project(":localization"))
    implementation(project(":uimodel"))
    implementation(project(":common"))
    implementation(project(":testtags"))

    implementation(libs.androidx.core)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.coil.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.windowsizeclass)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.icons)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    debugImplementation(libs.compose.ui.tooling.preview)

    screenshotTestImplementation(project(":screenshottest"))
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.compose.ui.tooling)
}

android {
    namespace = "net.svaroh.passly.core.ui"
    buildFeatures {
        compose = true
    }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    // tolerate sub-pixel antialiasing differences between macOS (dev) and Linux (CI) layoutlib renderers
    screenshotTests {
        imageDifferenceThreshold = 0.001f
    }
}
