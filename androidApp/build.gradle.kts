// Bloque de plugins aplicados a este módulo de aplicación Android.
plugins {
    // Plugin para la aplicación Android, usando un alias del catálogo de versiones.
    alias(libs.plugins.androidApplication)
    // Plugin de Kotlin para Android, usando un alias.
    alias(libs.plugins.kotlinAndroid)
    // Plugin de Kotlin para la serialización. Necesario para convertir objetos Kotlin a/desde JSON.
    // Usa el alias definido en libs.versions.toml ([plugins] sección).
    alias(libs.plugins.kotlinSerialization)
    // Plugin de Kotlin para Jetpack Compose, usando un alias. Habilita la compilación de @Composable.
    alias(libs.plugins.compose.compiler)
}

// Configuración específica para Android.
android {
    // Namespace de la aplicación, usado para generar la clase R.
    namespace = "com.example.paradigmaapp.android"
    // Versión del SDK contra la que se compila la aplicación.
    compileSdk = 35

    // Configuración por defecto de la aplicación.
    defaultConfig {
        applicationId = "com.example.paradigmaapp.android" // Identificador único de la app.
        minSdk = 28 // Versión mínima de Android soportada.
        targetSdk = 35 // Versión del SDK objetivo.
        versionCode = 1 // Código de versión interno.
        versionName = "1.0" // Nombre de versión visible al usuario.
    }

    // Habilita características de compilación.
    buildFeatures {
        compose = true // Habilita Jetpack Compose.
    }

    // Opciones específicas para Jetpack Compose.
    composeOptions {
        // Versión de la extensión del compilador de Kotlin para Compose.
        // Debe ser compatible con la versión de Kotlin que estás utilizando.
        // Se toma del catálogo de versiones.
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.extension.get().toString() // Asumiendo que tienes `compose-compiler-extension` en [versions]
        // Si definiste `compose-compiler = "1.5.4"` en [versions] para la extensión, usa:
        // kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get() // o como lo hayas nombrado.
        // Por ahora, usaré la que tenías hardcodeada si no tienes el alias específico para la extensión:
        // kotlinCompilerExtensionVersion = "1.5.4" // REVISAR COMPATIBILIDAD CON KOTLIN 2.0.0
    }

    // Configuración del empaquetado del APK.
    packaging {
        // Excluye archivos de metadatos específicos para evitar conflictos de duplicados.
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Configuración para los tipos de compilación (build types).
    buildTypes {
        // Configuración para la compilación de 'release'.
        getByName("release") {
            isMinifyEnabled = false // Deshabilita la minificación (reducción de código). Considera habilitarlo para producción.
        }
    }

    // Opciones de compilación de Java.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Compatibilidad con Java 8 para el código fuente.
        targetCompatibility = JavaVersion.VERSION_1_8 // Compatibilidad con Java 8 para el bytecode.
    }

    // Opciones del compilador de Kotlin.
    kotlinOptions {
        jvmTarget = "1.8" // Versión de la JVM objetivo para el bytecode de Kotlin.
    }
}

// Bloque de dependencias del módulo.
dependencies {
    // Dependencia del módulo ':shared' (tu lógica KMP compartida).
    implementation(projects.shared)

    // Jetpack Compose (BOM - Bill of Materials, para gestionar versiones de Compose)
    implementation(platform(libs.compose.bom)) // Plataforma BOM para alinear versiones de Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)      // Iconos Material Core
    implementation(libs.androidx.compose.material.icons.extended)  // Iconos Material Extendidos
    implementation(libs.androidx.activity.compose)                 // Integración de Compose con Activity
    implementation(libs.androidx.lifecycle.viewmodel.compose)      // Para delegados viewModel() en Compose
    debugImplementation(libs.compose.ui.tooling)                   // Herramientas de Compose (solo para debug)
    implementation(libs.compose.ui.tooling.preview)                // Para @Preview

    // Navegación con Jetpack Compose - Usando la dependencia estándar y correcta
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx) // Para funcionalidades runtime de navegación con Kotlin

    // Media3 ExoPlayer (Reproductor de medios)
    implementation(libs.androidx.media3.common)       // Componentes comunes de Media3
    implementation(libs.androidx.media3.exoplayer)    // Núcleo de ExoPlayer
    implementation(libs.androidx.media3.session)      // Para integración con MediaSession (recomendado)
    implementation(libs.androidx.media3.ui)           // Componentes UI opcionales para Media3

    // Ktor (Motor para Android, si el módulo shared no lo provee o si androidApp hace llamadas directas)
    // Si :shared ya configura y usa ktor-client-okhttp o ktor-client-android en su androidMain,
    // esta dependencia aquí podría ser redundante o para un uso separado.
    implementation(libs.ktor.client.android) // O libs.ktor.client.okhttp si prefieres y lo tienes en TOML

    // Kotlinx Coroutines (Para programación asíncrona en Android)
    implementation(libs.kotlinx.coroutines.android)

    // Kotlinx Serialization (Runtime JSON, si se usa directamente en androidApp)
    // Si la serialización ocurre solo en :shared, esta podría ser transitiva.
    implementation(libs.kotlinx.serialization.json)

    // Coil (Carga de imágenes para Compose)
    implementation(libs.coil.compose)

    // Timber (Biblioteca de logging)
    implementation(libs.timber)

    // Biblioteca Estándar de Kotlin (usualmente implícita)
    implementation(libs.kotlin.stdlib)
}