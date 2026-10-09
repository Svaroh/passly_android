plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":common"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.timber)
}

android {
    namespace = "net.svaroh.passly.domain.inappreview"
}
