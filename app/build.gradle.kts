plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pednia.kiosco"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pednia.kiosco"
        minSdk = 24          // Android 7.0 — cubre tablets de gama media
        targetSdk = 34
        versionCode = 1
        versionName = "1.0-prueba"
    }

    buildTypes {
        // La APK de depuración basta para probar; se firma con la debug key
        // automáticamente, así no hace falta configurar un keystore.
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    // WebViewAssetLoader: sirve el contenido bajo el origen https virtual
    // (appassets.androidplatform.net) → contexto seguro para getUserMedia.
    implementation("androidx.webkit:webkit:1.11.0")
}
