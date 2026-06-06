plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":architecture"))
    implementation(project(":common"))
    implementation(project(":passphrasememorycache"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "net.svaroh.passly.domain.auth"
}
