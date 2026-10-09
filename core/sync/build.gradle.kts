plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.work.runtime)
}

android {
    namespace = "net.svaroh.passly.core.sync"
}
