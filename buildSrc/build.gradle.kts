plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization.plugin)
    implementation(libs.compose.compiler.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.dependency.analysis.gradle.plugin)
    // DAGP <= 3.17.0 bundles kotlin-metadata-jvm 2.2.20, which only reads metadata <=2.3.0 and fails
    // on Kotlin 2.4. Force it to the project Kotlin version. DAGP 3.18.0 fixes this, but its new
    // FindKotlinMagicTask OOMs on CI, so we stay on 3.17.0 with this override for now:
    // https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1724
    implementation(libs.kotlin.metadata.jvm)
    implementation(libs.easylauncher)
    implementation(libs.licensee)
    implementation(libs.app.distribution.gradle.plugin)
}
