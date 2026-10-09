plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.otp)
    implementation(libs.apache.commons.codec)
}

android {
    namespace = "net.svaroh.passly.core.otp"
}
