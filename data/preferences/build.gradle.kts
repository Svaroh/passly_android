plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":preferences-domain"))
    implementation(project(":encryptedstorage"))
    implementation(project(":common"))
    implementation(project(":uimodel"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "net.svaroh.passly.data.preferences"
}
