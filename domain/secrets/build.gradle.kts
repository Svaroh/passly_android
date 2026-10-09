plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":common"))
    implementation(project(":gopenpgp"))
    implementation(project(":architecture"))
    implementation(project(":uimodel"))
    implementation(project(":serializers"))
    implementation(project(":supportedresourcetypes"))
    implementation(project(":passphrasememorycache"))
    implementation(project(":privatekey-domain"))
    implementation(project(":database"))
    implementation(project(":entity"))
    implementation(project(":jsonmodel"))
    implementation(libs.room.runtime)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.gson)
    implementation(libs.jsonschema.friend)
    implementation(libs.json.path)
}

android {
    namespace = "net.svaroh.passly.domain.secrets"
}
