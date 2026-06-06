plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":architecture"))
    implementation(project(":authentication"))
    implementation(project(":passphrasememorycache"))
    implementation(project(":common"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.kotlin.coroutines.test)
    implementation(libs.android.test.ktx)
    implementation(libs.mockito.kotlin)
}

android {
    namespace = "net.svaroh.passly.core.commontest"
}
