# Paradigma Media  

![App Screenshot]((https://drive.google.com/file/d/1vkgLQBbyiW-6miRk79ucc6Dxmhwfdkab/view?usp=sharing))  

## 📝 Descripción  
Aplicación Android para explorar y reproducir podcasts desde archive.org. Desarrollada con Jetpack Compose y ExoPlayer, ofrece una aplciación para disfrutar de los podcast de Pradigma Media.

## ✨ Características principales  
- 🎧 Reproductor de podcast
- 

## 🏗 Estructura del código  

### 🧩 Componentes principales  

| Archivo | Descripción |  
|---------|-------------|  
| `PodcastScreen.kt` | Pantalla principal con lista y reproductor |  
| `ArchiveService.kt` | Cliente para API de archive.org |  
| `AudioPlayer.kt` | Componente de reproductor con ExoPlayer |  
| `PodcastList.kt` | Lista lazy de podcasts |  

### 📦 Modelos de datos  
- `Podcast.kt`: Estructura de datos para podcasts  

### 🎨 Temas y UI  
- `MyApplicationTheme.kt`: Configuración de temas Material 3  

## 📦 Dependencias  

```gradle
dependencies {
    implementation(projects.shared)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.media3.exoplayer)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.coil.compose)
}
```

## 🚀 Cómo usar  
1. La app carga automáticamente podcasts al iniciar  
2. Toca cualquier podcast para reproducirlo  
3. Controles disponibles:  
   - ▶️/⏸ Play/Pause  
   - 🔊 Control de volumen  
   - 🎚 Barra de progreso arrastrable  

## 🛠 Patrones utilizados  
- MVVM (implícito en estructura Compose)  
- Repository Pattern (ArchiveService)  
- Lazy Loading (paginación de podcasts)  

## 📸 Capturas  

| Modo Claro | Modo Oscuro |  
|------------|-------------|  
| ![Light](https://drive.google.com/file/d/1Kvuefv8LKr-SmjLtLaCjKsEMLwDNaAcJ/view?usp=sharing) | ![Dark](https://drive.google.com/file/d/1vkgLQBbyiW-6miRk79ucc6Dxmhwfdkab/view?usp=sharing) |  

## 👨‍💻 Autor  

**Mario Alguacil Juárez**  
- 📧 alguacilmario6@gmail.com
- 🔗 [GitHub](https://github.com/MarioAJ11)  

---  
