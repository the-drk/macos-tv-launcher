plugins {
    id("com.android.application")
}

android {
    namespace = "com.thedrk.macoslauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.thedrk.macoslauncher"
        minSdk = 23
        targetSdk = 28
        versionCode = 1
        versionName = "0.1"
    }
}
