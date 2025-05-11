plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    kotlin("plugin.serialization") version "2.0.0"
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.paradigmaapp.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.paradigmaapp.android"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Dependencias base
    implementation(projects.shared)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.media3.exoplayer)
    debugImplementation(libs.compose.ui.tooling)

    //Dependencias nuevas
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.coil.compose)

    // Dependencia de Corrutinas de Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Dependencia de Timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Dependencia necesaria para MediaControlIntent
    implementation("androidx.media:media:1.7.0") // Añadida esta línea

    // Dependencias de Media3 Session y ExoPlayer
    implementation("androidx.media3:media3-session:1.6.1")
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    // Si usas otros módulos de media3 como UI o DASH, asegúrate de incluirlos también.

    // Dependencias de MediaRouter
    implementation("androidx.mediarouter:mediarouter:1.7.0")
}