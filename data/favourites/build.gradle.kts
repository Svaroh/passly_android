plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":favourites-domain"))
    implementation(project(":architecture"))
    implementation(project(":networking"))
    implementation(project(":dto"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "net.svaroh.passly.data.favourites"
}
