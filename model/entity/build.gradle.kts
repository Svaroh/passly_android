plugins {
    id("passbolt.android.library")
    alias(libs.plugins.kotlin.ksp)
}

dependencies {
    implementation(libs.room.core)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.gson)
}

android {
    namespace = "net.svaroh.passly.domain.entity"
}
