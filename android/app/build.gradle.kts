plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Alamat web Jejak Langkah. Di GitHub Actions diisi otomatis (https://<akun>.github.io/<repo>/).
val appUrl = (project.findProperty("appUrl") as String?) ?: "https://example.github.io/jejak-langkah/"

android {
    namespace = "id.angga.jejak"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.angga.jejak"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "APP_URL", "\"$appUrl\"")
    }

    // Kunci tetap supaya APK baru bisa menimpa APK lama tanpa uninstall.
    signingConfigs {
        getByName("debug") {
            storeFile = file("jejak.keystore")
            storePassword = "jejaklangkah"
            keyAlias = "jejak"
            keyPassword = "jejaklangkah"
        }
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
