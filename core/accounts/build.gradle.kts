plugins {
    id("passbolt.android.library")
    id(libs.plugins.kotlin.serialization.get().pluginId)
}

dependencies {
    implementation(project(":encryptedstorage"))
    implementation(project(":uimodel"))
    implementation(project(":biometrickey-domain"))
    implementation(project(":gopenpgp"))
    implementation(project(":dto"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.kotlin.serializationjson)
    implementation(libs.security)
}

android {
    namespace = "net.svaroh.passly.core.accounts"
}
