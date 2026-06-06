plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":entity"))
    implementation(project(":privatekey-domain"))
    implementation(project(":uimodel"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "net.svaroh.passly.domain.accounts"
}
