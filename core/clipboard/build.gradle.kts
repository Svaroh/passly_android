plugins {
    id("passbolt.android.library")
}

android {
    namespace = "net.svaroh.passly.core.clipboard"
}

dependencies {
    implementation(project(":localization"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}
