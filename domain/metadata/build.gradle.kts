plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":architecture"))
    implementation(project(":common"))
    implementation(project(":uimodel"))
    implementation(project(":privatekey-domain"))
    implementation(project(":users-domain"))
    implementation(project(":folders-domain"))
    implementation(project(":passphrasememorycache"))
    implementation(project(":gopenpgp"))
    implementation(project(":dto"))
    implementation(project(":mappers"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.kotlin.coroutines)
    implementation(libs.gson)
    implementation(libs.timber)

    testImplementation(project(":commontest"))

    androidTestImplementation(":gopenpgp@aar")
    androidTestImplementation(platform(libs.koin.bom))
    androidTestImplementation(libs.koin.test)
    androidTestImplementation(libs.koin.test.junit)
    androidTestImplementation(libs.kotlin.coroutines.test)
    androidTestImplementation(libs.android.tests.runner)
    androidTestImplementation(libs.android.test.rules)
    androidTestImplementation(libs.dexmaker.mockito.inline.extended)
    androidTestImplementation(libs.mockito.kotlin)
}

android {
    namespace = "net.svaroh.passly.domain.metadata"
}
