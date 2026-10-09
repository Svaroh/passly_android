plugins {
    alias(libs.plugins.playstore.publisher) apply false
    alias(libs.plugins.kotlin.ksp)
    id(libs.plugins.dependency.analysis.get().pluginId)
}

apply(from = "gradle/versions.gradle.kts")

val androidCommonConfig = AndroidCommonConfig(
    compileSdk = extra["projectCompileSdk"] as Int,
    minSdk = extra["projectMinSdk"] as Int,
    minFullySupportedSdk = extra["projectMinFullySupportedSdk"] as Int,
    targetSdk = extra["projectTargetSdk"] as Int,
    jvmTarget = 17,
)

val commonLibraryDependencies = CommonLibraryDependencies(
    kotlinStdlib = libs.kotlin.stdlib,
    kotlinCoroutines = libs.kotlin.coroutines.asProvider(),
    timber = libs.timber,
    junit = libs.junit,
    mockitoKotlin = libs.mockito.kotlin,
    truth = libs.truth,
    turbine = libs.turbine,
    kotlinCoroutinesTest = libs.kotlin.coroutines.test,
    koinBom = libs.koin.bom,
    koinTest = libs.koin.test.asProvider(),
    koinTestJunit = libs.koin.test.junit,
    androidxJunit = libs.androidx.junit,
    desugarJdkLibs = libs.desugar.jdklibs,
)

val qualityConfig = QualityConfig(
    ktlintVersion = libs.versions.ktlintTool.get(),
    detektVersion = libs.versions.detekt.get(),
)

val qualityDependencies = QualityDependencies(
    ktlintComposeRuleset = libs.ktlint.compose.rulset,
    detektComposeRuleset = libs.detekt.compose.rulset,
)

allprojects {
    extensions.add("androidCommonConfig", androidCommonConfig)
    extensions.add("commonLibraryDependencies", commonLibraryDependencies)
    extensions.add("qualityConfig", qualityConfig)
    extensions.add("qualityDependencies", qualityDependencies)

    configurations.configureEach {
        if (name.endsWith("ReleaseRuntimeClasspath", ignoreCase = true)) {
            resolutionStrategy.activateDependencyLocking()
        }
    }

    tasks.register("resolveAndLockAll") {
        notCompatibleWithConfigurationCache("Filters configurations at execution time")
        doFirst {
            check(gradle.startParameter.isWriteDependencyLocks) {
                "$path must be run from the command line with the `--write-locks` flag"
            }
        }
        doLast {
            configurations.matching { it.isCanBeResolved }.toList().forEach { config ->
                try {
                    config.resolve()
                } catch (ignored: Exception) {
                    logger.info("Skipped unresolvable configuration '${config.name}'")
                }
            }
        }
    }
}

subprojects {
    apply(plugin = "quality-setup")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

tasks.register("coverageReport") {
    group = "verification"
    description = "Runs JaCoCo unit-test coverage reports for all Android modules."
    dependsOn(subprojects.map { subproject ->
        subproject.tasks.matching { it.name == "jacocoDebugUnitTestReport" }
    })
}

