plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":mfa-domain"))
    implementation(project(":architecture"))
    implementation(project(":networking"))
    implementation(project(":common"))
    implementation(project(":dto"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "net.svaroh.passly.data.mfa"
}
