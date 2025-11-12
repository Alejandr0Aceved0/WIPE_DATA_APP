plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.hilt) // Para Hilt
    alias(libs.plugins.ksp)  // Para Hilt y Room
}

android {
    namespace = "com.alejoacevedodev.wipe_data_beta"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.alejoacevedodev.wipe_data_beta"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- Core y Lifecycle ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose) // Para ViewModel

    // --- Compose ---
    implementation(platform(libs.androidx.compose.bom)) // BOM
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose) // Para Navegación

    // --- Inyección de Dependencias (Hilt) ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // ... (tus otras dependencias de Room)
    ksp(libs.room.compiler)

    // --- Base de Datos (Room) ---
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // --- Corrutinas ---
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // --- Utilidades (SAF) ---
    implementation(libs.androidx.documentfile)

    // --- Dependencia para Temas (Material 2) ---
    // A menudo es necesaria para los temas base de la app (themes.xml)
    implementation(libs.material)

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}