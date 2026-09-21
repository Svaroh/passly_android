plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":common"))
    implementation(project(":dto"))
    implementation(project(":architecture"))
    implementation(project(":localization"))
    implementation(project(":auth-domain"))

    api(libs.retrofit)
    api(libs.okhttp)
    api(libs.okhttp.logging.interceptor)

    implementation(libs.retrofit.gsonconverter)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.gson)

    testImplementation(libs.mockwebserver)
}

android {
    namespace = "com.passbolt.mobile.android.core.networking"
}
