// Define el tipo de destino JVM para las opciones del compilador de Kotlin.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget // No es estrictamente necesario aquí si no se usa explícitamente abajo.

// Bloque de plugins aplicados a este módulo compartido de Kotlin Multiplatform.
plugins {
    // Plugin de Kotlin Multiplatform, usando un alias del catálogo de versiones.
    alias(libs.plugins.kotlinMultiplatform)
    // Plugin de Biblioteca Android, para que este módulo pueda ser una dependencia de Android.
    alias(libs.plugins.androidLibrary)
    // Plugin de Kotlin para la serialización, necesario para commonMain.
    // Su versión se alinea con la versión del plugin de Kotlin Multiplatform.
    alias(libs.plugins.kotlinSerialization) // Asumiendo que tienes este alias en tu TOML para el plugin de serialización.
    // Si no, usa: id("org.jetbrains.kotlin.plugin.serialization") version libs.versions.kotlin.get()
    // Plugin de Kotlin para Jetpack Compose (si usas Compose en commonMain, lo cual es experimental).
    // Si Compose es solo para androidApp, este plugin no es necesario aquí.
    // alias(libs.plugins.compose.compiler) // Comenta o elimina si no usas Compose en commonMain.
}

// Configuración de Kotlin Multiplatform.
kotlin {
    // Define el destino Android.
    androidTarget {
        // Configura las compilaciones para el destino Android.
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    // Establece el destino JVM para el bytecode de Kotlin.
                    jvmTarget.set(JvmTarget.JVM_1_8)
                    // Argumentos adicionales para el compilador si son necesarios.
                    // freeCompilerArgs.add("-Xexpect-actual-classes") // Para clases expect/actual (puede ser necesario)
                }
            }
        }
        // Opcional: Publica variantes específicas de la biblioteca Android.
        // publishLibraryVariants("release", "debug")
    }

    // Define los destinos iOS.
    // Es una buena práctica nombrar explícitamente cada target.
    iosX64("iosX64") {
        binaries.framework {
            baseName = "shared" // Nombre base para el framework de iOS.
            isStatic = true     // Genera un framework estático.
        }
    }
    iosArm64("iosArm64") {
        binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }
    iosSimulatorArm64("iosSimulatorArm64") {
        binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    // Define los conjuntos de fuentes (source sets) y sus dependencias.
    sourceSets {
        // Dependencias para el código común a todas las plataformas.
        commonMain.dependencies {
            // Kotlinx Coroutines (núcleo) para programación asíncrona.
            // Añade `kotlinx-coroutines-core` a tu libs.versions.toml.
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // Reemplazar con libs.kotlinx.coroutines.core

            // Ktor (cliente HTTP multiplataforma).
            implementation(libs.ktor.client.core)                // Núcleo de Ktor.
            implementation(libs.ktor.client.content.negotiation) // Para negociación de contenido (ej. JSON).
            implementation(libs.ktor.serialization.kotlinx.json) // Adaptador de Ktor para kotlinx.serialization.
            // Añade `ktor-client-logging` a tu libs.versions.toml.
            implementation("io.ktor:ktor-client-logging:2.3.8") // Reemplazar con libs.ktor.client.logging (para logs de Ktor).

            // Kotlinx Serialization (biblioteca para serialización/deserialización JSON).
            // Añade `kotlinx-serialization-json` a tu libs.versions.toml.
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3") // Reemplazar con libs.kotlinx.serialization.json
        }

        // Dependencias específicas para el código Android en el módulo shared.
        androidMain.dependencies {
            // Kotlinx Coroutines para Android (integración con el ciclo de vida de Android).
            // Añade `kotlinx-coroutines-android` a tu libs.versions.toml.
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // Reemplazar con libs.kotlinx.coroutines.android

            // Motor Ktor OkHttp para Android (tu TOML usa ktor-client-android que es un motor general).
            // OkHttp es una opción robusta y común. Si `libs.ktor.client.android` es tu elección de motor, está bien.
            // Si quieres usar OkHttp específicamente:
            implementation(libs.ktor.client.okhttp) // Asegúrate de tener `ktor-client-okhttp` en tu TOML.
            // Si tu TOML tiene `ktor-client-android` y es el motor que quieres, usa ese.
        }

        // Dependencias específicas para el código iOS en el módulo shared.
        iosMain.dependencies {
            // Motor Ktor Darwin para iOS.
            // Añade `ktor-client-darwin` a tu libs.versions.toml.
            implementation("io.ktor:ktor-client-darwin:2.3.8") // Reemplazar con libs.ktor.client.darwin
        }

        // Dependencias para los tests comunes.
        commonTest.dependencies {
            implementation(libs.kotlin.test) // Biblioteca de test de Kotlin.
        }
        // Puedes definir dependencias para tests específicos de plataforma también:
        // androidUnitTest.dependencies { ... }
        // iosTest.dependencies { ... }
    }
}

// Configuración específica de Android para este módulo de biblioteca.
android {
    // Namespace de la biblioteca, importante para evitar conflictos y para la clase R.
    // Cambiado para ser específico del módulo shared.
    namespace = "com.example.paradigmaapp.shared"
    // Versión del SDK contra la que se compila.
    compileSdk = 35
    // Configuración por defecto.
    defaultConfig {
        minSdk = 28 // Versión mínima de Android soportada por esta biblioteca.
    }
    // Opciones de compilación de Java.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint {
        abortOnError = false // No detener la compilación por errores de lint.
    }
}