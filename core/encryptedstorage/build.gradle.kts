plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":common"))
    implementation(libs.security)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)

    androidTestImplementation(libs.android.tests.runner)
}

android {
    namespace = "com.passbolt.mobile.android.core.encrypted"
}
