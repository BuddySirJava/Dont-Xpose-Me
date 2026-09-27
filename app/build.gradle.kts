plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "me.dontxpose"
    compileSdk = 34

    defaultConfig {
        applicationId = "me.dontxpose"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    signingConfigs {
        val storePath = System.getenv("RELEASE_STORE_FILE")
            ?: (project.findProperty("RELEASE_STORE_FILE") as String?)
        if (!storePath.isNullOrBlank()) {
            create("release") {
                storeFile = file(storePath)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                    ?: project.findProperty("RELEASE_STORE_PASSWORD") as String?
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                    ?: project.findProperty("RELEASE_KEY_ALIAS") as String?
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
                    ?: project.findProperty("RELEASE_KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "OldTargetApi")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    compileOnly(project(":xposed-api"))

    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.fragment:fragment-ktx:1.8.4")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.compose.ui:ui:1.7.5")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    testImplementation("junit:junit:4.13.2")
}
