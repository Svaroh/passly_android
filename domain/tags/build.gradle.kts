plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":common"))
    implementation(project(":uimodel"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.paging.runtime)
}

android {
    namespace = "net.svaroh.passly.domain.tags"
}
