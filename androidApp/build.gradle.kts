plugins {
    alias(libs.plugins.androidApplication) // Plugin de aplicación Android
    alias(libs.plugins.kotlinAndroid)      // Plugin de Kotlin para Android
    alias(libs.plugins.kotlinSerialization) // kotlinx.serialization, si se usa directamente aquí
    alias(libs.plugins.compose.compiler)   // Compilador de Jetpack Compose
}

android {
    namespace = "com.example.paradigmaapp.android" // Namespace de la aplicación
    compileSdk = 35 // SDK de compilación

    defaultConfig {
        applicationId = "org.paradigmamedia.paradigmaapp" // ID único de la app
        minSdk = 28 // Mínima versión de Android soportada
        targetSdk = 35 // SDK objetivo
        versionCode = 1 // Código de versión para releases
        versionName = "1.0" // Nombre de versión visible
    }

    buildFeatures {
        compose = true // Habilitar Jetpack Compose
    }

    composeOptions {
        // Versión de la extensión del compilador de Kotlin para Compose.
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.extension.get().toString()
    }

    packaging {
        resources {
            // Excluye archivos de metadatos de licencias para evitar conflictos de duplicados.
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getProperty("MYAPP_RELEASE_STORE_FILE", "none"))
            storePassword = System.getProperty("MYAPP_RELEASE_STORE_PASSWORD", "none")
            keyAlias = System.getProperty("MYAPP_RELEASE_KEY_ALIAS", "none")
            keyPassword = System.getProperty("MYAPP_RELEASE_KEY_PASSWORD", "none")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Le decimos que use la configuración de firma que acabamos de crear
            signingConfig = signingConfigs.getByName("release")
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
    // Módulo compartido
    implementation(projects.shared)

    // Jetpack Compose - BoM para gestionar versiones consistentes.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose) // Integración de Compose con Activity
    implementation(libs.androidx.lifecycle.viewmodel.compose) // Para `viewModel()` en Composables
    debugImplementation(libs.compose.ui.tooling) // Herramientas de Compose (Live Edit, Inspector)
    implementation(libs.compose.ui.tooling.preview) // Para @Preview

    // Navegación con Jetpack Compose
    implementation(libs.androidx.navigation.compose)

    // Media3 ExoPlayer (Reproducción de audio/video)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session) // Integración con MediaSession

    implementation("androidx.paging:paging-compose:3.3.6")

    // Ktor (Networking)
    implementation(libs.ktor.client.android)

    // Kotlinx Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Kotlinx Serialization (JSON)
    implementation(libs.kotlinx.serialization.json)

    // Coil (Carga de imágenes en Compose)
    implementation(libs.coil.compose)

    // Timber (Logging)
    implementation(libs.timber)
}