plugins {
    id("passbolt.android.library")
}

dependencies {
    api(project(":networking"))
    implementation(project(":common"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.retrofit.scalarsconverter)
}

android {
    namespace = "net.svaroh.passly.services.pwnedpasswordsapi"
}
