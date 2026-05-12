plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
}
kotlin {
    android {
        namespace = "sqz.checklist.task"
        compileSdk = 37
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                api(libs.okio)

                api(project(":framework:core:data"))
                api(project(":framework:core:common"))
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                //implementation("com.lemonappdev:konsist:0.17.3")
            }
        }
    }
}

lint {
    checkGeneratedSources = false
    disable += "RestrictedApi"
}
