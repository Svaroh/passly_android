plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(":gopenpgp@aar")
    implementation(project(":common"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)

    androidTestImplementation(":gopenpgp@aar")
    androidTestImplementation(platform(libs.koin.bom))
    androidTestImplementation(libs.koin.test)
    androidTestImplementation(libs.koin.test.junit)
    androidTestImplementation(libs.kotlin.coroutines.test)
    androidTestImplementation(libs.android.tests.runner)
    androidTestImplementation(libs.android.test.rules)

    androidTestImplementation(project(":serializers"))
    androidTestImplementation(libs.gson)
}

android {
    namespace = "net.svaroh.passly.core.gopenpgp"

    packaging {
        resources.excludes += "META-INF/{AL2.0,LGPL2.1}"
    }
}
