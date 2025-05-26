plugins {
    alias(libs.plugins.androidApplication).apply(false) // Define el plugin para aplicaciones Android
    alias(libs.plugins.androidLibrary).apply(false)    // Define el plugin para bibliotecas Android
    alias(libs.plugins.kotlinAndroid).apply(false)       // Define el plugin de Kotlin para Android
    alias(libs.plugins.kotlinMultiplatform).apply(false) // Define el plugin de Kotlin Multiplatform
    alias(libs.plugins.compose.compiler).apply(false)  // Define el plugin del compilador de Compose
}