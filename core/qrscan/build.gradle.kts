plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.qr.scan)
    implementation(libs.camerax)
    implementation(libs.camerax.view)
    implementation(libs.camerax.mlvision)
}

android {
    namespace = "net.svaroh.passly.core.qrscan"
}
