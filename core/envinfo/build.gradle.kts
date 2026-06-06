plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "net.svaroh.passly.core.envinfo"
}
