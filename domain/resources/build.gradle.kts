plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":resourcetypes-domain"))
    implementation(project(":accounts-domain"))
    implementation(project(":common"))
    implementation(project(":architecture"))
    implementation(project(":mappers"))
    implementation(project(":uimodel"))
    implementation(project(":dto"))
    implementation(project(":networking"))
    implementation(project(":share-domain"))
    implementation(project(":favourites-domain"))
    implementation(project(":gopenpgp"))
    implementation(project(":secrets-domain"))
    implementation(project(":resourcetypes"))
    implementation(project(":authentication"))
    implementation(project(":users-domain"))
    implementation(project(":entity"))
    implementation(project(":tags-domain"))
    implementation(project(":serializers"))
    implementation(project(":passwordexpiry-domain"))
    implementation(project(":supportedresourcetypes"))
    implementation(project(":folders-domain"))
    implementation(project(":preferences-domain"))
    implementation(project(":privatekey-domain"))
    implementation(project(":passphrasememorycache"))
    implementation(project(":metadata-domain"))
    implementation(project(":groups-domain"))
    implementation(project(":permissionsconfirmation-domain"))
    implementation(project(":jsonmodel"))
    implementation(project(":coreui"))

    implementation(libs.gson)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.json.path)
    implementation(libs.paging.runtime)
    implementation(libs.kotlin.coroutines)

    testImplementation(project(":commontest"))
}

android {
    namespace = "net.svaroh.passly.domain.resources"
}
