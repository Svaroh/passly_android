plugins {
    id("passbolt.android.library")
    id(libs.plugins.kotlin.serialization.get().pluginId)
}

dependencies {
    implementation(libs.retrofit.gsonconverter)
    implementation(libs.kotlin.serializationjson)
}

android {
    namespace = "net.svaroh.passly.domain.dto"
}
