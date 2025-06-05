import org.jetbrains.kotlin.gradle.dsl.JvmTarget // Necesario para JvmTarget.JVM_1_8

plugins {
    alias(libs.plugins.kotlinMultiplatform) // Plugin esencial para KMP
    alias(libs.plugins.androidLibrary)      // Para que 'shared' sea una biblioteca Android
    alias(libs.plugins.kotlinSerialization) // Para kotlinx.serialization
}

kotlin {
    androidTarget { // Configuración del target de Android
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8) // Compatibilidad con Java 8 para Android
                    // freeCompilerArgs.add("-Xexpect-actual-classes") // Habilitar si se usa esta característica
                }
            }
        }
        // publishLibraryVariants("release", "debug") // Opcional, usualmente no necesario para módulos internos
    }

    // Configuración de los targets de iOS para diferentes arquitecturas.
    // Se generan frameworks estáticos para una integración más sencilla con Xcode.
    iosX64("iosX64") { // Para simuladores en Macs Intel
        binaries.framework {
            baseName = "shared" // Nombre base del framework
            isStatic = true // Framework estático
        }
    }
    iosArm64("iosArm64") { // Para dispositivos iOS físicos (ARM64)
        binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }
    iosSimulatorArm64("iosSimulatorArm64") { // Para simuladores en Macs Apple Silicon (ARM64)
        binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    // Definición de los conjuntos de fuentes (source sets) y sus dependencias.
    sourceSets {
        commonMain.dependencies {
            // Coroutines para programación asíncrona multiplataforma.
            implementation(libs.kotlinx.coroutines.core)

            // Ktor para networking multiplataforma.
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation) // Para serialización/deserialización (JSON)
            implementation(libs.ktor.serialization.kotlinx.json) // Adaptador de Ktor para kotlinx.serialization
            implementation(libs.ktor.client.logging) // Logging para Ktor

            // Kotlinx Serialization para JSON.
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            // Coroutines específicas para Android
            implementation(libs.kotlinx.coroutines.android)

            // Motor Ktor OkHttp para Android.
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            // Motor Ktor Darwin (NSURLSession) para iOS.
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test) // Biblioteca de testing de Kotlin
        }
        // androidUnitTest.dependencies { /* Dependencias de test específicas para Android */ }
        // iosTest.dependencies { /* Dependencias de test específicas para iOS */ }
    }
}

android { // Configuración específica de Android para este módulo de biblioteca
    namespace = "com.example.paradigmaapp.shared" // Namespace único para el módulo
    compileSdk = 35 // SDK de compilación (Considerar usar 34 si no se necesitan APIs de 35)

    defaultConfig {
        minSdk = 28 // Mínima versión de Android soportada
        // targetSdk no se suele definir en módulos de librería.
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 //
        targetCompatibility = JavaVersion.VERSION_1_8 //
    }

    // Configuración de Lint.
    lint {
        abortOnError = false // No detener la compilación por errores de Lint (considerar true para CI)
    }
}