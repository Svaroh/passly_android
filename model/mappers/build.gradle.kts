plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":dto"))
    implementation(project(":share-domain"))
    implementation(project(":entity"))
    implementation(project(":common"))
    implementation(project(":uimodel"))
    implementation(project(":navigation"))
    implementation(project(":jsonmodel"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.gson)
    implementation(project(":supportedresourcetypes"))
}

android {
    namespace = "net.svaroh.passly.domain.mappers"
}
