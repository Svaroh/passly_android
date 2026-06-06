plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":biometrickey-domain"))
    implementation(project(":encryptedstorage"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "net.svaroh.passly.data.biometrickey"
}
