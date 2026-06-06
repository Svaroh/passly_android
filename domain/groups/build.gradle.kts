plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":architecture"))
    implementation(project(":common"))
    implementation(project(":uimodel"))
    implementation(project(":users-domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.kotlin.coroutines)
    implementation(libs.paging.runtime)
    implementation(libs.timber)
}

android {
    namespace = "net.svaroh.passly.domain.groups"
}
