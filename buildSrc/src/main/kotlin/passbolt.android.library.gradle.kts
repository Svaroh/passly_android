import com.android.build.api.dsl.LibraryExtension

plugins {
    id("com.android.library")
}

val androidConfig = extensions.getByType<AndroidCommonConfig>()
val commonDeps = extensions.getByType<CommonLibraryDependencies>()

extensions.configure<LibraryExtension> {
    compileSdk = androidConfig.compileSdk
    defaultConfig {
        minSdk = androidConfig.minSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        val javaVersion = JavaVersion.toVersion(androidConfig.jvmTarget)
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = false
        checkAllWarnings = true
        warningsAsErrors = false
        disable += lintDisabledIssues
    }
    testOptions {
        managedDevices {
            localDevices {
                create("pixel5@targetSdk") {
                    device = "Pixel 5"
                    apiLevel = androidConfig.targetSdk
                    systemImageSource = "aosp"
                }
            }
        }
        animationsDisabled = true
        unitTests.all {
            val processors = Runtime.getRuntime().availableProcessors() - 1
            it.maxParallelForks = maxOf(processors, 1)
        }
    }
    buildTypes.getByName("release") {
        isMinifyEnabled = false
        if (file("proguard-rules.pro").exists()) {
            consumerProguardFiles("proguard-rules.pro")
        }
    }
}

kotlin {
    jvmToolchain(androidConfig.jvmTarget)
}

registerUnitTestAggregate()
registerCoverageTasks()

dependencies {
    implementation(commonDeps.kotlinStdlib)
    implementation(commonDeps.kotlinCoroutines)
    implementation(commonDeps.timber)
    testImplementation(commonDeps.junit)
    testImplementation(commonDeps.mockitoKotlin)
    testImplementation(commonDeps.truth)
    testImplementation(commonDeps.turbine)
    testImplementation(commonDeps.kotlinCoroutinesTest)
    testImplementation(platform(commonDeps.koinBom))
    testImplementation(commonDeps.koinTest)
    testImplementation(commonDeps.koinTestJunit)
    androidTestImplementation(commonDeps.androidxJunit)
    androidTestImplementation(commonDeps.truth)
    coreLibraryDesugaring(commonDeps.desugarJdkLibs)
}
