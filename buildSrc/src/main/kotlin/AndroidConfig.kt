import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

val lintDisabledIssues = setOf(
    "GoogleAppIndexingWarning", "GradleDependency", "NewerVersionAvailable", "UnusedIds",
    "Autofill", "PermissionImpliesUnsupportedChromeOsHardware", "WrongConstant", "RequiredSize",
    "Instantiatable", "InvalidPackage", "MissingTranslation", "ExtraTranslation",
)


/**
 * Registers an aggregate `unitTest` task that runs Android unit tests.
 * AGP 9 renamed unit tests to host tests (`testDebugHostTest`); we depend on whichever exists.
 */
fun Project.registerUnitTestAggregate() {
    val unitTestTask = tasks.register("unitTest")
    afterEvaluate {
        unitTestTask.configure {
            listOf("testDebugHostTest", "testDebugUnitTest").forEach { name ->
                if (project.tasks.names.contains(name)) {
                    dependsOn(name)
                }
            }
        }
    }
}

/**
 * Registers JaCoCo unit test coverage tasks on an Android module.
 */
fun Project.registerCoverageTasks() {
    pluginManager.apply("jacoco")

    tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
        extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class.java) {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    val reportTask = tasks.register("jacocoDebugUnitTestReport", org.gradle.testing.jacoco.tasks.JacocoReport::class.java) {
        group = "verification"
        description = "Generates a JaCoCo coverage report for debug unit tests."

        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }

        val fileFilter = listOf(
            "**/R.class",
            "**/R\$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "**/*\$ViewInjector*.*",
            "**/*\$ViewBinder*.*",
            "**/*MembersInjector*.*",
            "**/*_Factory.*",
            "**/*_Provide*Factory*.*",
            "**/*_ViewBinding*.*",
            "**/AutoValue_*.*",
            "**/R2.class",
            "**/R2\$*.class",
            "**/*Directions\$*",
            "**/*Directions.*",
            "**/*Binding.*",
            "**/*Args.*",
            "**/*Companion*.*",
            "**/*Module.*",
            "**/*Dagger*.*",
            "**/*Hilt*.*"
        )

        classDirectories.setFrom(
            fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
                exclude(fileFilter)
            },
            fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
                exclude(fileFilter)
            }
        )
        sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
        executionData.setFrom(
            fileTree(layout.buildDirectory) {
                include(
                    "jacoco/testDebugUnitTest.exec",
                    "jacoco/testDebugHostTest.exec",
                    "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
                )
            }
        )
    }

    afterEvaluate {
        reportTask.configure {
            listOf("testDebugHostTest", "testDebugUnitTest").forEach { testTaskName ->
                if (project.tasks.names.contains(testTaskName)) {
                    dependsOn(testTaskName)
                }
            }
        }
    }
}

