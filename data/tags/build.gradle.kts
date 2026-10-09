plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":tags-domain"))
    implementation(project(":common"))
    implementation(project(":database"))
    implementation(project(":entity"))
    implementation(project(":uimodel"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.room.core)
    implementation(libs.room.paging)
    implementation(libs.sqlite.cipher) { artifact { type = "aar" } }
    implementation(libs.paging.runtime)
}

android {
    namespace = "net.svaroh.passly.data.tags"
}
