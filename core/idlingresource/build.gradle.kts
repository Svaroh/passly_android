plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.espresso.idling.resource)
}

android {
    namespace = "net.svaroh.passly.core.idlingresource"
}
