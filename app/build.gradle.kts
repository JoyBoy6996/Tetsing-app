plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.joyboy.performanceoverlay"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.joyboy.performanceoverlay"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
