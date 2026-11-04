plugins {
    id("passbolt.android.library")
}

android {
    namespace = "net.svaroh.passly.core.sync"
}

dependencies {
    implementation(project(":architecture"))
    implementation(project(":networking"))
    implementation(project(":database"))
    implementation(project(":entity"))
    implementation(project(":dto"))
    implementation(project(":common"))
    implementation(project(":accounts-domain"))
    implementation(project(":resources-domain"))
    implementation(project(":folders-domain"))
    implementation(project(":users-domain"))
    implementation(project(":groups-domain"))
    implementation(project(":resourcetypes-domain"))
    implementation(project(":metadata-domain"))
    implementation(project(":secrets-domain"))
    implementation(project(":mappers"))
    implementation(project(":uimodel"))

    implementation(libs.room.runtime)
    implementation(libs.room.core)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.work.runtime)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gsonconverter)
    implementation(libs.timber)

    testImplementation(project(":commontest"))
    testImplementation(libs.truth)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)
    testImplementation(libs.work.testing)
}
