plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":architecture"))
    implementation(project(":common"))
    implementation(project(":uimodel"))
    implementation(project(":dto"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "com.passbolt.mobile.android.domain.mobiletransfer"
}
