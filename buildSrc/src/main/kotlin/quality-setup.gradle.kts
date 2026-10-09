import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    id("com.autonomousapps.dependency-analysis")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

val qualityConfig = extensions.getByType<QualityConfig>()
val qualityDeps = extensions.getByType<QualityDependencies>()

extensions.configure<KtlintExtension> {
    version.set(qualityConfig.ktlintVersion)
    android.set(true)
    verbose.set(true)
    debug.set(true)
}

extensions.configure<DetektExtension> {
    toolVersion = qualityConfig.detektVersion
    parallel = true
    config.setFrom("$rootDir/gradle/detekt-config.yml")
    buildUponDefaultConfig = true
    ignoredBuildTypes = listOf("qa", "automatedTests", "release")
    basePath = "build/reports/detekt-report.html"
}

dependencies {
    ktlintRuleset(qualityDeps.ktlintComposeRuleset)
    detektPlugins(qualityDeps.detektComposeRuleset)
}
