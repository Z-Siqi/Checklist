plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "sqz.checklist.umbrella"
        compileSdk = 37
    }

    val exportDependencies = listOf(
        project(":framework:core:common"),
        project(":framework:core:data"),
        project(":framework:feature:task"),
    )

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ChecklistKit"
            isStatic = true
            linkerOpts.add("-lsqlite3")
            exportDependencies.forEach { dependency -> export(dependency) }
        }
    }

    sourceSets {
        commonMain.dependencies {
            exportDependencies.forEach { api(it) }
        }
    }
}
