plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":privatekey-domain"))
    implementation(project(":encryptedstorage"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.security)
}

android {
    namespace = "net.svaroh.passly.data.privatekey"
}
