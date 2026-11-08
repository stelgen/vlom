plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.stelgen.vlom"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.stelgen.vlom"
        minSdk = 28          // Android 9.0 (REQ-001 P2)
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        // REQ-001 P3: universal APK = arm32 + arm64 в одном APK
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Тесты с первого дня (профессиональный канон: фича без теста = незаконченная фича)
    testImplementation("junit:junit:4.13.2")
    // org.json на JVM (unit-тесты McmlJson); на Android — встроенная реализация
    testImplementation("org.json:json:20240303")
}
