plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":architecture"))
}

android {
    namespace = "net.svaroh.passly.domain.mfa"
}
