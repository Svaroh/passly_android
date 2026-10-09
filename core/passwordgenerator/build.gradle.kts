plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":localization"))
    implementation(project(":uimodel"))
    implementation(project(":architecture"))
    implementation(project(":common"))
    implementation(project(":pwnedpasswordsapi"))
    implementation(project(":mappers"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)

    testImplementation(project(":commontest"))
}

android {
    namespace = "net.svaroh.passly.core.passwordgenerator"
}
