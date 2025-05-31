// Defino JvmTarget para las opciones del compilador de Kotlin, aunque el import explícito
// no es estrictamente necesario si no lo uso más abajo de forma directa.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Bloque de plugins que aplico a este módulo compartido de Kotlin Multiplatform.
plugins {
    // Plugin de Kotlin Multiplatform, esencial para la estructura KMP.
    // Lo referencio usando un alias de mi catálogo de versiones (libs.versions.toml).
    alias(libs.plugins.kotlinMultiplatform) //

    // Plugin de Biblioteca Android, necesario para que este módulo 'shared' pueda ser
    // una dependencia en mi módulo 'androidApp' y para definir código específico de Android (androidMain).
    alias(libs.plugins.androidLibrary) //

    // Plugin de Kotlin para la serialización, requerido para 'commonMain' si uso kotlinx.serialization.
    // Su versión se alinea con la del plugin de Kotlin Multiplatform.
    alias(libs.plugins.kotlinSerialization) //
    // Alternativamente, si no tuviera un alias para el plugin de serialización:
    // id("org.jetbrains.kotlin.plugin.serialization") version libs.versions.kotlin.get()
}

// Configuración principal de Kotlin Multiplatform para este módulo.
kotlin {
    // Defino el target para Android. Esto permite tener código específico en 'androidMain'
    // y que el módulo 'shared' sea consumido por 'androidApp'.
    androidTarget { //
        // Configuro las compilaciones para el target de Android.
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    // Establezco el destino de la JVM para el bytecode de Kotlin a Java 8.
                    // Esto asegura compatibilidad con versiones de Android más antiguas.
                    jvmTarget.set(JvmTarget.JVM_1_8) //
                    // Aquí podría añadir argumentos adicionales para el compilador si fueran necesarios,
                    // por ejemplo, para habilitar características experimentales.
                    // freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
        // Opcionalmente, podría publicar variantes específicas de la biblioteca Android (ej. release, debug),
        // pero para un módulo 'shared' interno, generalmente no es necesario.
        // publishLibraryVariants("release", "debug")
    }

    // Defino los targets para iOS.
    // Es una buena práctica nombrar explícitamente cada target para claridad.
    // Genero frameworks estáticos para una integración más sencilla con Xcode.
    iosX64("iosX64") { // Para simuladores en Macs con Intel.
        binaries.framework {
            baseName = "shared" // Nombre base para el framework generado.
            isStatic = true     // Genero un framework estático.
        }
    }
    iosArm64("iosArm64") { // Para dispositivos iOS reales (ARM64).
        binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }
    iosSimulatorArm64("iosSimulatorArm64") { // Para simuladores en Macs con Apple Silicon (ARM64).
        binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    // Defino los conjuntos de fuentes (source sets) y sus dependencias.
    // Esta es la estructura central de un proyecto KMP.
    sourceSets {
        // Dependencias para 'commonMain', el código compartido entre todas las plataformas.
        commonMain.dependencies {
            // Kotlinx Coroutines para programación asíncrona.
            // VERIFICAR: Asegurarme de que '1.7.3' es la versión deseada y está alineada
            // con mi versión de Kotlin. Idealmente, usar 'libs.kotlinx.coroutines.core'.
            implementation(libs.kotlinx.coroutines.core)

            // Ktor, mi cliente HTTP multiplataforma.
            implementation(libs.ktor.client.core) // Núcleo de Ktor.
            implementation(libs.ktor.client.content.negotiation) // Para negociación de contenido (ej. JSON).
            implementation(libs.ktor.serialization.kotlinx.json) // Adaptador de Ktor para kotlinx.serialization.
            // Logging para Ktor.
            implementation(libs.ktor.client.logging)

            // Kotlinx Serialization para JSON.
            // es la versión deseada y compatible con mi versión de Kotlin.
            implementation(libs.kotlinx.serialization.json)
        }

        // Dependencias específicas para 'androidMain', el código Kotlin que solo se compila para Android
        // dentro del módulo 'shared'.
        androidMain.dependencies {
            // Kotlinx Coroutines para Android, incluye integración con el Looper de Android.
            implementation(libs.kotlinx.coroutines.android)

            // Motor Ktor OkHttp para Android. OkHttp es una librería robusta y común.
            // Esto es correcto si quiero usar OkHttp específicamente como motor en Android.
            implementation(libs.ktor.client.okhttp) //
            // Si 'libs.ktor.client.android' en mi TOML apuntara a un motor diferente o a un artefacto general,
            // tendría que decidir cuál usar. 'okhttp' es una elección explícita y buena.
        }

        // Dependencias específicas para 'iosMain', el código Kotlin para iOS.
        iosMain.dependencies {
            // Motor Ktor Darwin para iOS, usa las APIs nativas de red de iOS (NSURLSession).
            // VERIFICAR: Usar 'libs.ktor.client.darwin' y la versión de Ktor correspondiente.
            implementation(libs.ktor.client.darwin)
        }

        // Dependencias para 'commonTest', los tests unitarios comunes a todas las plataformas.
        commonTest.dependencies {
            implementation(libs.kotlin.test) // Biblioteca de test de Kotlin.
        }
        // Podría definir dependencias para tests específicos de plataforma también si fuera necesario:
        // androidUnitTest.dependencies { ... }
        // iosTest.dependencies { ... }
    }
}

// Configuración específica de Android para este módulo, ya que es una 'androidLibrary'.
android {
    // Namespace de la biblioteca. Es importante para la generación de la clase R y para evitar
    // conflictos si este módulo se usa en múltiples proyectos.
    // "com.example.paradigmaapp.shared" es específico y está bien.
    namespace = "com.example.paradigmaapp.shared" //

    // Versión del SDK de Android contra la que se compila este módulo.
    // NOTA: '35' es muy reciente (preview). Considerar usar la última estable (ej. 34) para mayor compatibilidad
    // o si no necesito específicamente características de la API 35.
    compileSdk = 35 //

    // Configuración por defecto para este módulo de librería.
    defaultConfig {
        // Versión mínima de Android soportada por esta librería.
        // '28' (Android 9 Pie) es una elección razonable, cubriendo muchos dispositivos.
        minSdk = 28 //

        // El 'targetSdk' no se suele definir en módulos de librería, se hereda de la app principal.
        // Si se define, debe ser igual o menor que el 'targetSdk' de la app.
    }

    // Opciones de compilación de Java.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Código fuente compatible con Java 8.
        targetCompatibility = JavaVersion.VERSION_1_8 // Bytecode compatible con Java 8.
    }

    // Opciones de Lint para el análisis estático del código.
    lint {
        // 'abortOnError = false' significa que los errores de lint no detendrán la compilación.
        // Para una librería, podría ser más estricto ('true'), o manejar esto en CI.
        abortOnError = false //
    }
}