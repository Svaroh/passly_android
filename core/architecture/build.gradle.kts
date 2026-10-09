plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(libs.androidx.core)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "net.svaroh.passly.core.architecture"
    buildFeatures {
        viewBinding = true
    }
}
