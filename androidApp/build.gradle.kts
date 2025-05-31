// Este es el archivo de configuración de Gradle para mi módulo de aplicación Android (:androidApp).
// Aquí defino los plugins, la configuración específica de Android y las dependencias de este módulo.
// @author Mario Alguacil Juárez

// Bloque de plugins aplicados a este módulo de aplicación Android.
plugins {
    // Plugin para la aplicación Android, lo obtengo de mi catálogo de versiones (libs).
    alias(libs.plugins.androidApplication)
    // Plugin de Kotlin para Android.
    alias(libs.plugins.kotlinAndroid)
    // Plugin de Kotlin para la serialización, necesario si este módulo maneja directamente JSON.
    // Si toda la serialización ocurre en :shared, podría no ser estrictamente necesario aquí,
    // pero es seguro incluirlo si hay alguna duda o uso directo.
    alias(libs.plugins.kotlinSerialization)
    // Plugin de Kotlin para Jetpack Compose. Habilita la compilación de mis funciones @Composable.
    alias(libs.plugins.compose.compiler)
}

// Configuración específica para Android.
android {
    // Namespace de la aplicación. Es importante para la generación de la clase R y debe ser único.
    namespace = "com.example.paradigmaapp.android"
    // Versión del SDK de Android contra la que compilo mi aplicación.
    compileSdk = 35 //

    // Configuración por defecto de la aplicación.
    defaultConfig {
        applicationId = "com.example.paradigmaapp.android" // Identificador único de mi app en Play Store.
        minSdk = 28 // Versión mínima de Android que soportará mi aplicación (Android 9 Pie).
        // Versión del SDK a la que está dirigida mi app.
        // Es bueno mantenerla actualizada con la última versión estable o la misma que compileSdk.
        targetSdk = 35
        versionCode = 1 // Código de versión interno, se incrementa con cada release.
        versionName = "1.0" // Nombre de versión visible al usuario.
    }

    // Habilito características de compilación.
    buildFeatures {
        compose = true // Habilito Jetpack Compose para construir mi UI.
    }

    // Opciones específicas para Jetpack Compose.
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.extension.get().toString()
    }

    // Configuración del empaquetado de mi APK/App Bundle.
    packaging {
        // Excluyo archivos de metadatos específicos de algunas librerías para evitar
        // conflictos de duplicados al empaquetar.
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Configuración para los tipos de compilación (build types), como 'debug' y 'release'.
    buildTypes {
        // Configuración específica para la compilación de 'release'.
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Opciones de compilación de Java.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Mi código fuente es compatible con Java 8.
        targetCompatibility = JavaVersion.VERSION_1_8 // El bytecode generado es compatible con Java 8.
    }

    // Opciones del compilador de Kotlin.
    kotlinOptions {
        jvmTarget = "1.8" // El bytecode de Kotlin se compila para la JVM 1.8.
    }
}

// Bloque de dependencias de este módulo :androidApp.
dependencies {
    // Dependencia de mi módulo ':shared', que contiene la lógica de negocio común y KMP.
    implementation(projects.shared)

    // Jetpack Compose.
    // Uso el Bill of Materials (BOM) de Compose para asegurar que todas las librerías de Compose
    // que utilizo sean versiones compatibles entre sí.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui) // UI básica de Compose.
    implementation(libs.compose.ui.graphics) // Gráficos en Compose.
    implementation(libs.compose.foundation) // Elementos básicos de UI y layout.
    implementation(libs.compose.material3) // Componentes de Material Design 3.
    implementation(libs.androidx.compose.material.icons.core)      // Iconos base de Material.
    implementation(libs.androidx.compose.material.icons.extended)  // Iconos extendidos de Material.
    implementation(libs.androidx.activity.compose)                 // Para integrar Compose con Activity.
    implementation(libs.androidx.lifecycle.viewmodel.compose)      // Para usar `viewModel()` en Composables.
    // Herramientas de Compose, solo para compilaciones de depuración (ej. Live Edit, Layout Inspector).
    debugImplementation(libs.compose.ui.tooling)
    // Para usar la anotación @Preview en mis Composables.
    implementation(libs.compose.ui.tooling.preview)

    // Navegación con Jetpack Compose.
    implementation(libs.androidx.navigation.compose) // Para integrar Navigation con Compose.
    implementation(libs.androidx.navigation.runtime.ktx) // Extensiones Kotlin para Navigation.

    // Media3 ExoPlayer (para la reproducción de audio y video).
    implementation(libs.androidx.media3.common)       // Componentes comunes de Media3.
    implementation(libs.androidx.media3.exoplayer)    // El núcleo de ExoPlayer.
    implementation(libs.androidx.media3.session)      // Para integración con MediaSession, útil para controles multimedia del sistema.
    implementation(libs.androidx.media3.ui)           // Componentes UI opcionales para Media3 (ej. PlayerControlView).

    implementation(libs.ktor.client.android) // Motor Ktor para Android (podría ser OkHttp o CIO según la config del artefacto).

    // Kotlinx Coroutines (para programación asíncrona en Android).
    implementation(libs.kotlinx.coroutines.android) // Incluye el dispatcher para el hilo principal de Android.

    // Kotlinx Serialization (para manejar JSON, si se usa directamente en :androidApp).
    // Si toda la serialización/deserialización ocurre en :shared, esta dependencia
    // sería transitiva y no necesitaría declararla explícitamente aquí.
    implementation(libs.kotlinx.serialization.json)

    // Coil (para cargar imágenes de forma asíncrona en Compose).
    implementation(libs.coil.compose)

    // Timber (para logging).
    implementation(libs.timber)

    // Biblioteca Estándar de Kotlin (generalmente se añade implícitamente, pero es bueno ser explícito).
    implementation(libs.kotlin.stdlib)
}