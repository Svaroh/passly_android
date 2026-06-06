plugins {
    id("passbolt.android.library")
    id(libs.plugins.kotlin.serialization.get().pluginId)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":localization"))
    implementation(project(":supportedresourcetypes"))
    implementation(project(":jsonmodel"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.gson)
    implementation(libs.json.path)
    implementation(libs.kotlin.serializationjson)
}

android {
    namespace = "net.svaroh.passly.uimodel"
}
