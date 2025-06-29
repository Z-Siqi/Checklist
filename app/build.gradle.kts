plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp") version "2.0.0-1.0.24"
}

android {
    namespace = "com.sqz.checklist"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sqz.checklist"
        minSdk = 26
        targetSdk = 36
        versionCode = 38 // 50
        versionName = "0.5.0-Build_DEBUG_38" // 0.5.0-Build_CI

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            // Enables code-related app optimization.
            isMinifyEnabled = true

            // Enables resource shrinking.
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3-android:1.3.2")
    implementation("androidx.compose.foundation:foundation-android:1.8.3")
    implementation("androidx.compose.runtime:runtime-android:1.8.3")
    implementation("androidx.work:work-runtime-ktx:2.10.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.06.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // Leakcanary
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.0")
    // Room
    ksp("androidx.room:room-compiler:2.7.2")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    testImplementation("androidx.room:room-testing:2.7.2")
    // Media
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("io.sanghun:compose-video:1.2.0")
    implementation("androidx.media3:media3-ui:1.7.1")
    implementation("androidx.media3:media3-exoplayer:1.7.1")
    implementation("com.otaliastudios:transcoder:0.11.2")
}
