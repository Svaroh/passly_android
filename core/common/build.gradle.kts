plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":localization"))
    implementation(project(":architecture"))

    implementation(libs.androidx.core)
    implementation(libs.biometric)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.commons.validator)
}

android {
    namespace = "net.svaroh.passly.core.common"
}
