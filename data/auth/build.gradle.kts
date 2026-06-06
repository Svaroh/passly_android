plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":auth-domain"))
    implementation(project(":architecture"))
    implementation(project(":networking"))
    implementation(project(":dto"))
    implementation(project(":common"))
    implementation(project(":encryptedstorage"))
    implementation(project(":passphrasememorycache"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "net.svaroh.passly.data.auth"
}
