plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":permissionsconfirmation-domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "com.passbolt.mobile.android.data.permissionsconfirmation"
}
