plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":localization"))
    implementation(project(":envinfo"))
    implementation(project(":common"))
    implementation(project(":preferences-domain"))
    implementation(project(":uimodel"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "net.svaroh.passly.core.logger"
}
