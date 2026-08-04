plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":architecture"))
    implementation(project(":common"))
    implementation(project(":folders-domain"))
    implementation(project(":groups-domain"))
    implementation(project(":mappers"))
    implementation(project(":users-domain"))
    implementation(project(":uimodel"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "com.passbolt.mobile.android.domain.permissionsconfirmation"
}
