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
        applicationId = "com.example.paradigmaapp.android" // ID único de la app
        minSdk = 28 // Mínima versión de Android soportada
        targetSdk = 35 // SDK objetivo (Alinear con compileSdk)
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

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true // Habilitar R8/ProGuard para ofuscar y reducir código
            isShrinkResources = true // Habilitar reducción de recursos no utilizados
            // Archivos de reglas de ProGuard.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), // Reglas por defecto de Android
                "proguard-rules.pro" // Reglas personalizadas del proyecto
            ) //
        }
        // Se podría definir un buildTypes "debug" aquí si se necesitaran configuraciones específicas
        // diferentes a las predeterminadas
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
    // implementation(libs.androidx.navigation.runtime.ktx) // Esta dependencia ya no es necesaria explícitamente con las versiones recientes de navigation-compose

    // Media3 ExoPlayer (Reproducción de audio/video)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session) // Integración con MediaSession
    // implementation(libs.androidx.media3.ui) // Componentes UI opcionales para Media3 (Añadir si se usan controles UI de Media3)

    // Ktor (Networking)
    // Si Ktor se usa solo en :shared, esta dependencia podría no ser necesaria aquí directamente.
    // Sin embargo, si :androidApp hace llamadas de red directas o usa el motor de Android de Ktor:
    implementation(libs.ktor.client.android) // Motor Ktor para Android (ej. OkHttp o CIO)

    // Kotlinx Coroutines
    implementation(libs.kotlinx.coroutines.android) // Soporte para Android (Dispatchers.Main)

    // Kotlinx Serialization (JSON)
    // Necesario si este módulo serializa/deserializa JSON directamente.
    // Si toda la serialización ocurre en :shared, puede ser transitiva.
    implementation(libs.kotlinx.serialization.json) //

    // Coil (Carga de imágenes en Compose)
    implementation(libs.coil.compose)

    // Timber (Logging) - Se eliminarán las llamadas en el código fuente, pero la dependencia puede permanecer
    // si se planea reintroducir para builds de depuración controlados.
    // Para una build de producción limpia, se podría comentar o usar variantes de compilación.
    // Por ahora, la mantendré si está en tu libs, asumiendo que las llamadas se eliminan.
    implementation(libs.timber)
}