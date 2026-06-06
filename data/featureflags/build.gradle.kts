plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":featureflags-domain"))
    implementation(project(":architecture"))
    implementation(project(":networking"))
    implementation(project(":dto"))
    implementation(project(":encryptedstorage"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "net.svaroh.passly.data.featureflags"
}
