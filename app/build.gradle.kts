plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.antutu.ABenchMark"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.antutu.ABenchMark"
        minSdk = 26
        targetSdk = 35
        versionCode = 17
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
