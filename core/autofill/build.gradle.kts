plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":coreui"))
    implementation(project(":localization"))
    implementation(project(":navigation"))
    implementation(project(":uimodel"))
    implementation(project(":jsonmodel"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockito.kotlin)
}

android {
    namespace = "net.svaroh.passly.core.autofill"

    buildFeatures {
        buildConfig = true
    }
}
