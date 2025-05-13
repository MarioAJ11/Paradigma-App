package com.example.paradigmaapp.android

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.paradigmaapp.android.api.AndainaStream
import com.example.paradigmaapp.android.api.ArchiveService
import com.example.paradigmaapp.android.audio.AudioPlayer
import com.example.paradigmaapp.android.audio.PlayStreaming
import com.example.paradigmaapp.android.podcast.Podcast
import com.example.paradigmaapp.android.podcast.PodcastList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

// Componentes de UI personalizados
import com.example.paradigmaapp.android.ui.SearchBar
import com.example.paradigmaapp.android.ui.UserIcon
// Manejo de insets del sistema
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.media3.common.C
import com.example.paradigmaapp.android.ui.SettingsDialog

/**
 * Constantes para SharedPreferences
 */
private const val PREFS_NAME = "PodcastPlaybackPrefs"
private const val PREF_CURRENT_PODCAST_URL = "currentPodcastUrl"
private const val PREF_CURRENT_POSITION = "currentPosition"
private const val PREF_IS_STREAM_ACTIVE = "isStreamActive"

/**
 * Obtiene las SharedPreferences para guardar el estado de reproducción y ajustes
 */
fun getSharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

/**
 * Guarda el progreso de reproducción de un podcast
 */
fun savePlaybackProgress(context: Context, podcastUrl: String?, positionMillis: Long) {
    getSharedPreferences(context).edit().apply {
        if (podcastUrl != null) {
            putString(PREF_CURRENT_PODCAST_URL, podcastUrl)
            putLong(PREF_CURRENT_POSITION, positionMillis)
            Timber.d("Saved progress for URL: $podcastUrl at position: $positionMillis")
        } else {
            remove(PREF_CURRENT_PODCAST_URL)
            remove(PREF_CURRENT_POSITION)
            Timber.d("Cleared saved playback progress.")
        }
        apply()
    }
}

/**
 * Carga el progreso de reproducción guardado
 */
fun loadPlaybackProgress(context: Context): Pair<String?, Long> {
    val prefs = getSharedPreferences(context)
    val podcastUrl = prefs.getString(PREF_CURRENT_PODCAST_URL, null)
    val position = prefs.getLong(PREF_CURRENT_POSITION, 0L)
    Timber.d("Loaded saved progress - URL: $podcastUrl, Position: $position")
    return Pair(podcastUrl, position)
}

/**
 * Guarda el estado de si el stream de Andaina debe estar activo.
 */
fun saveIsStreamActive(context: Context, isActive: Boolean) {
    getSharedPreferences(context).edit().putBoolean(PREF_IS_STREAM_ACTIVE, isActive).apply()
    Timber.d("Saved stream active state: $isActive")
}

/**
 * Carga el estado de si el stream de Andaina debe estar activo.
 */
fun loadIsStreamActive(context: Context): Boolean {
    val isActive = getSharedPreferences(context).getBoolean(PREF_IS_STREAM_ACTIVE, true) // Valor por defecto true
    Timber.d("Loaded stream active state: $isActive")
    return isActive
}

/**
 * Pantalla principal de la aplicación que muestra la lista de podcasts y controles de reproducción.
 *
 * Características principales:
 * - Muestra lista de podcasts con capacidad de búsqueda
 * - Control de reproducción para podcasts individuales
 * - Control de streaming en vivo (Andaina)
 * - Persistencia del estado de reproducción y ajustes
 * - Interfaz de usuario responsive que considera los insets del sistema
 * - Diálogo de ajustes para configuraciones simples.
 */
@Composable
fun AppScreen() { // Removed NavHostController parameter
    // Contexto y servicios
    val context = LocalContext.current
    val archiveService = remember { ArchiveService() }
    val andainaStreamPlayer = remember(context) { AndainaStream(context) }

    // Estados de la UI y datos
    var initialPodcasts by remember { mutableStateOf<List<Podcast>>(emptyList()) }
    var filteredPodcasts by remember { mutableStateOf<List<Podcast>>(emptyList()) }
    var isLoadingInitial by remember { mutableStateOf(true) }
    var currentPodcast by remember { mutableStateOf<Podcast?>(null) }
    var searchText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Estados de reproducción
    var podcastProgress by remember { mutableStateOf(0f) }
    var podcastDuration by remember { mutableStateOf(0L) }
    var isPodcastPlaying by remember { mutableStateOf(false) }
    // Usamos el estado guardado para determinar si el stream debe estar activo
    var isAndainaStreamActive by remember { mutableStateOf(loadIsStreamActive(context)) } // Mantenemos este estado para la lógica del stream
    var isAndainaPlaying by remember { mutableStateOf(false) }
    var seekJob by remember { mutableStateOf<Job?>(null) }
    // Estado para controlar si el stream ha fallado al intentar cargar
    var hasStreamLoadFailed by remember { mutableStateOf(false) }


    // Estado para controlar la visibilidad del diálogo de ajustes
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Player de podcast con listeners para eventos
    val podcastExoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Timber.e("Podcast ExoPlayer error: ${error.errorCodeName} - ${error.message}")
                        Toast.makeText(context, "Error al reproducir podcast: ${error.message}", Toast.LENGTH_SHORT).show()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Timber.d("Podcast playback state changed to: $playbackState")
                        if (playbackState == Player.STATE_ENDED) {
                            // Opcional: Limpiar el podcast actual y progreso al finalizar
                            coroutineScope.launch {
                                delay(500) // Pequeño delay antes de limpiar
                                if (playbackState == Player.STATE_ENDED) { // Doble check
                                    currentPodcast = null
                                    savePlaybackProgress(context, null, 0L)
                                    Timber.d("Podcast ended, cleared current podcast and progress.")
                                }
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        isPodcastPlaying = isPlaying
                        // La lógica de guardado de progreso al pausar está en onPlayPauseClick
                    }
                })
            }
    }

    // Añadir listener de errores al player de streaming
    LaunchedEffect(andainaStreamPlayer.exoPlayer) {
        andainaStreamPlayer.exoPlayer?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Timber.e("Stream ExoPlayer error: ${error.errorCodeName} - ${error.message}")
                Toast.makeText(context, "Error al reproducir stream: ${error.message}", Toast.LENGTH_SHORT).show()
                // Detener el stream si hay un error grave
                andainaStreamPlayer.stop()
                hasStreamLoadFailed = true // Marcar que la carga del stream falló
            }

            // También puedes escuchar cambios de estado para manejar buffering, etc.
            override fun onPlaybackStateChanged(playbackState: Int) {
                when(playbackState) {
                    Player.STATE_BUFFERING -> Timber.d("Stream buffering...")
                    Player.STATE_READY -> {
                        Timber.d("Stream ready to play")
                        hasStreamLoadFailed = false // Si llega a READY, significa que no falló la carga inicial
                    }
                    Player.STATE_ENDED -> {
                        Timber.d("Stream ended")
                        // Si el stream termina inesperadamente, podrías intentar reconectar
                        // o mostrar un mensaje. Para un stream en vivo, STATE_ENDED puede
                        // indicar un problema en la fuente.
                        if (isAndainaStreamActive) {
                            Toast.makeText(context, "El streaming de Andaina ha finalizado.", Toast.LENGTH_SHORT).show()
                        }
                        isAndainaPlaying = false // Asegurar que el estado de UI se actualice
                        hasStreamLoadFailed = true // Considerar que el stream falló si termina inesperadamente
                    }
                    Player.STATE_IDLE -> {
                        Timber.d("Stream idle")
                        isAndainaPlaying = false // Asegurar que el estado de UI se actualice
                        // Si pasa a IDLE después de un error, hasStreamLoadFailed ya estará true.
                        // Si pasa a IDLE por otra razón (como detener manualmente),
                        // hasStreamLoadFailed debería mantenerse según la última carga.
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isAndainaPlaying = isPlaying
                if (isPlaying) {
                    hasStreamLoadFailed = false // Si está reproduciendo, no ha fallado
                }
            }
        })
    }


    // Determina qué player está activo actualmente para la UI del AudioPlayer
    val activePlayer = remember(currentPodcast) {
        if (currentPodcast != null) podcastExoPlayer else andainaStreamPlayer.exoPlayer
    }

    // Determina si algo está reproduciendo para la UI del AudioPlayer
    val isPlaying = remember(currentPodcast, isPodcastPlaying, isAndainaPlaying) {
        if (currentPodcast != null) isPodcastPlaying else isAndainaPlaying
    }

    // Determina si se está reproduciendo un stream en vivo para la UI del AudioPlayer
    val isLiveStream = remember(currentPodcast) { currentPodcast == null }


    // Efecto para cargar el progreso guardado al iniciar y manejar el podcast inicial
    LaunchedEffect(Unit) {
        val (savedPodcastUrl, savedPosition) = loadPlaybackProgress(context)

        // Intentar establecer el currentPodcast si se cargó un progreso guardado
        if (savedPodcastUrl != null) {
            coroutineScope.launch(Dispatchers.IO) {
                // Necesitamos obtener la lista inicial primero para encontrar el podcast guardado
                if (initialPodcasts.isEmpty()) {
                    try {
                        val allPodcasts = archiveService.fetchAllPodcasts("mario011")
                        withContext(Dispatchers.Main) {
                            initialPodcasts = allPodcasts
                            filteredPodcasts = if (searchText.isBlank()) {
                                initialPodcasts
                            } else {
                                initialPodcasts.filter { podcast ->
                                    podcast.title.contains(searchText, ignoreCase = true)
                                }
                            }
                            isLoadingInitial = false
                        }
                    } catch (e: Exception) {
                        Timber.e("Error fetching initial podcasts for resume: $e")
                        withContext(Dispatchers.Main) {
                            initialPodcasts = emptyList()
                            filteredPodcasts = emptyList()
                            isLoadingInitial = false
                            Toast.makeText(context, "Error al cargar podcasts.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Ahora buscamos el podcast guardado en la lista inicial
                val savedPodcast = initialPodcasts.find { it.url == savedPodcastUrl }

                withContext(Dispatchers.Main) {
                    currentPodcast = savedPodcast
                    if (currentPodcast == null) {
                        Timber.w("Saved podcast URL not found in the current list, clearing saved progress.")
                        savePlaybackProgress(context, null, 0L)
                        // Opcional: Notificar al usuario que el podcast guardado no se encontró.
                    }
                    // La carga real con la posición guardada y el inicio de play
                    // se maneja en el LaunchedEffect(currentPodcast)
                }
            }
        } else {
            // Si no hay progreso guardado, asegurar que la lista inicial se cargue
            // Y si el stream debe estar activo, intentar iniciarlo
            if (initialPodcasts.isEmpty() && isLoadingInitial) {
                try {
                    coroutineScope.launch(Dispatchers.IO) {
                        val allPodcasts = archiveService.fetchAllPodcasts("mario011")
                        withContext(Dispatchers.Main) {
                            initialPodcasts = allPodcasts
                            isLoadingInitial = false
                            filteredPodcasts = if (searchText.isBlank()) {
                                initialPodcasts
                            } else {
                                initialPodcasts.filter { podcast ->
                                    podcast.title.contains(searchText, ignoreCase = true)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e("Error fetching initial podcasts when no saved progress: $e")
                    initialPodcasts = emptyList()
                    filteredPodcasts = emptyList()
                    isLoadingInitial = false
                    Toast.makeText(context, "Error al cargar podcasts.", Toast.LENGTH_SHORT).show()
                }
            }

            // Intentar iniciar el stream al inicio si está activo por defecto y no hay podcast guardado
            if (isAndainaStreamActive && savedPodcastUrl == null) {
                andainaStreamPlayer.play()
            }
        }
    }


    // Efecto para actualizar estados de reproducción periódicamente
    LaunchedEffect(podcastExoPlayer, andainaStreamPlayer, currentPodcast) {
        while (isActive) {
            isPodcastPlaying = podcastExoPlayer.isPlaying
            isAndainaPlaying = andainaStreamPlayer.isPlaying()

            if (currentPodcast != null) {
                val duration = podcastExoPlayer.duration
                val currentPos = podcastExoPlayer.currentPosition
                podcastDuration = if (duration > 0 && duration != C.TIME_UNSET) duration else 0L
                podcastProgress = if (podcastDuration > 0) currentPos.toFloat() / podcastDuration.toFloat() else 0f
                // Opcional: Guardar progreso periódicamente (menos frecuente que 500ms)
                // Por ejemplo, guardar cada 5 segundos:
                if (podcastExoPlayer.isPlaying && currentPos > 0 && podcastDuration > 0 && podcastDuration != C.TIME_UNSET) {
                    // Evitar guardar a cada tick si el progreso no ha cambiado significativamente
                    // Podrías guardar cada N segundos o cada X% de progreso
                    // Para simplificar, guardamos al menos cada pocos segundos si está reproduciendo
                    if (currentPos % 5000 < 500) { // Guardar cada ~5 segundos
                        savePlaybackProgress(context, currentPodcast?.url, currentPos)
                        Timber.d("Periodic progress saved: $currentPos")
                    }
                }
            } else {
                podcastProgress = 0f
                podcastDuration = 0L
            }

            delay(500)
        }
    }

    // Efecto para cargar la lista inicial de podcasts (asegurando que ocurra si el resume logic no la disparó)
    LaunchedEffect(Unit) {
        if (initialPodcasts.isEmpty() && isLoadingInitial) {
            try {
                coroutineScope.launch(Dispatchers.IO) {
                    val allPodcasts = archiveService.fetchAllPodcasts("mario011")
                    withContext(Dispatchers.Main) {
                        initialPodcasts = allPodcasts
                        isLoadingInitial = false
                        filteredPodcasts = if (searchText.isBlank()) {
                            initialPodcasts
                        } else {
                            initialPodcasts.filter { podcast ->
                                podcast.title.contains(searchText, ignoreCase = true)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e("Error fetching initial podcasts: $e")
                initialPodcasts = emptyList()
                filteredPodcasts = emptyList()
                isLoadingInitial = false
                Toast.makeText(context, "Error al cargar podcasts.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Efecto para filtrar podcasts según texto de búsqueda
    LaunchedEffect(searchText, initialPodcasts) {
        filteredPodcasts = if (searchText.isBlank()) {
            initialPodcasts
        } else {
            initialPodcasts.filter { podcast ->
                podcast.title.contains(searchText, ignoreCase = true)
//                        || podcast.description.contains(searchText, ignoreCase = true) // Opcional: buscar también en descripción
            }
        }
    }

    // Efecto para manejar cambios en el podcast seleccionado y iniciar reproducción
    LaunchedEffect(currentPodcast) {
        val podcastToPlay = currentPodcast

        if (podcastToPlay != null) {
            // Detener stream si está reproduciendo al seleccionar un podcast
            if (andainaStreamPlayer.isPlaying()) {
                andainaStreamPlayer.stop()
            }
            hasStreamLoadFailed = false // Resetear el estado de fallo del stream

            if (podcastToPlay.url.isNotEmpty()) {
                val (savedPodcastUrl, savedPosition) = loadPlaybackProgress(context)

                // Lógica para cargar desde posición guardada o iniciar desde 0
                // Solo aplicamos la posición guardada si es el MISMO podcast y la posición es > 0
                if (savedPodcastUrl == podcastToPlay.url && savedPosition > 0) {
                    // Si ya tenemos el mismo MediaItem cargado, solo hacemos seek
                    if (podcastExoPlayer.currentMediaItem?.localConfiguration?.uri.toString() == podcastToPlay.url) {
                        podcastExoPlayer.seekTo(savedPosition)
                        Timber.d("Podcast set with saved position (seek): $savedPosition")
                    } else {
                        // Si es el mismo podcast pero el MediaItem no está cargado, lo cargamos con la posición
                        podcastExoPlayer.clearMediaItems()
                        podcastExoPlayer.setMediaItem(MediaItem.fromUri(podcastToPlay.url), savedPosition)
                        podcastExoPlayer.prepare()
                        Timber.d("Podcast set with saved position (setMediaItem): $savedPosition")
                    }
                    podcastExoPlayer.playWhenReady = true // Iniciar reproducción automáticamente
                } else {
                    // Si es un podcast nuevo o no hay progreso guardado, iniciar desde 0
                    podcastExoPlayer.clearMediaItems()
                    podcastExoPlayer.setMediaItem(MediaItem.fromUri(podcastToPlay.url))
                    podcastExoPlayer.prepare()
                    Timber.d("Starting new podcast: loaded and playing.")
                    podcastExoPlayer.playWhenReady = true // Iniciar reproducción automáticamente
                }

                // Guardar el nuevo podcast seleccionado y la posición inicial (que será 0 o la cargada)
                // La posición se actualizará al pausar, buscar o salir.
                // Guardamos la posición actual del player después de setear el MediaItem
                savePlaybackProgress(context, podcastToPlay.url, podcastExoPlayer.currentPosition)


            } else {
                // Si el podcast tiene URL vacía, mostrar un mensaje
                Toast.makeText(context, "URL de podcast no válida.", Toast.LENGTH_SHORT).show()
                currentPodcast = null // Limpiar el estado si la URL no es válida
            }
        } else { // Si currentPodcast es null (por ejemplo, al finalizar un podcast o cambiar a stream)
            // Limpiar reproductor si no hay podcast seleccionado
            if (podcastExoPlayer.playbackState != Player.STATE_IDLE && podcastExoPlayer.playbackState != Player.STATE_ENDED) {
                podcastExoPlayer.stop()
                podcastExoPlayer.clearMediaItems()
                Timber.d("Cleared podcast player because no podcast is selected.")
            }

            // Limpiar progreso guardado cuando no hay podcast seleccionado
            savePlaybackProgress(context, null, 0L)
            Timber.d("Cleared saved progress because no podcast is selected.")

            // Si el stream debe estar activo y no ha fallado al cargar al inicio, intentar iniciar
            // Solo iniciamos aquí si isAndainaStreamActive es true y no hay podcast seleccionado.
            // La lógica para iniciar al inicio de la app está en el LaunchedEffect(Unit).
            // Aquí nos aseguramos de que si cambiamos de un podcast a "nada" (stream),
            // el stream intente reproducirse si está activo y no ha fallado su carga inicial.
            if (isAndainaStreamActive && !andainaStreamPlayer.isPlaying() && !hasStreamLoadFailed) {
                // Nota: Esta lógica podría llevar a intentar reproducir el stream si cambias de podcast a null manualmente.
                // Considera si este es el comportamiento deseado. Si solo quieres que intente
                // iniciar al inicio de la app o al pulsar el botón, podrías quitar esta llamada aquí.
                // Lo mantenemos por ahora para que vuelva al stream si quitas un podcast manualmente.
                andainaStreamPlayer.play()
            }
        }
    }

    // Efecto para manejar el estado activo/inactivo del stream desde los ajustes
    // y controlar la reproducción/parada del stream.
    LaunchedEffect(isAndainaStreamActive) {
        if (isAndainaStreamActive) {
            // Si se activa y no hay podcast reproduciendo, intentar reproducir el stream
            if (currentPodcast == null && !andainaStreamPlayer.isPlaying()) {
                // Resetear el estado de fallo si se activa el stream para permitir un nuevo intento.
                // Solo si queremos que un fallo anterior no bloquee el play al activar en settings.
                // Si quieres que siga bloqueado hasta pulsar el botón, quita esta línea.
                // hasStreamLoadFailed = false // Decide si quieres esto o no
                andainaStreamPlayer.play()
            }
        } else {
            // Si se desactiva, detener el stream
            if (andainaStreamPlayer.isPlaying()) {
                andainaStreamPlayer.stop()
            }
            // Opcional: Si desactivas el stream, considera si quieres resetear hasStreamLoadFailed
            // hasStreamLoadFailed = false // Decide si quieres esto o no
        }
        saveIsStreamActive(context, isAndainaStreamActive)
    }


    // Limpieza al destruir el composable
    DisposableEffect(Unit) {
        onDispose {
            // Guardar progreso actual antes de liberar recursos
            if (currentPodcast != null && podcastExoPlayer.playbackState != Player.STATE_IDLE && podcastExoPlayer.playbackState != Player.STATE_ENDED) {
                // Asegurarse de que el MediaItem actual en el player coincide con el podcast que estamos guardando
                if (podcastExoPlayer.currentMediaItem?.localConfiguration?.uri.toString() == currentPodcast?.url) {
                    savePlaybackProgress(context, currentPodcast?.url, podcastExoPlayer.currentPosition)
                    Timber.d("Progress saved on dispose: ${podcastExoPlayer.currentPosition}")
                } else {
                    Timber.w("Media item changed before dispose, skipping progress save.")
                    // Opcional: Limpiar progreso si el MediaItem no coincide
                    // savePlaybackProgress(context, null, 0L)
                }
            } else {
                // Si no hay podcast seleccionado o el player está en estado IDLE/ENDED,
                // asegurarse de que no hay progreso guardado para un podcast que ya terminó.
                savePlaybackProgress(context, null, 0L)
                Timber.d("Cleared saved progress on dispose (no podcast or state IDLE/ENDED).")
            }

            podcastExoPlayer.release()
            andainaStreamPlayer.release()
            Timber.i("Players released on dispose")
        }
    }

    // Layout principal de la pantalla
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SearchBar(
                searchText = searchText,
                onSearchTextChanged = { newText -> searchText = newText },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            UserIcon(
                onClick = { showSettingsDialog = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        }

        when {
            isLoadingInitial -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            initialPodcasts.isEmpty() && searchText.isBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron podcasts subidos por mario011.",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            filteredPodcasts.isEmpty() && searchText.isNotBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron podcasts que coincidan con \"$searchText\".",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            else -> {
                PodcastList(
                    podcasts = filteredPodcasts,
                    onPodcastSelected = { podcast ->
                        currentPodcast = podcast // Al seleccionar, actualizamos el estado
                        // La reproducción se inicia en el LaunchedEffect(currentPodcast)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )
            }
        }

        // Mostrar mensaje de stream si ha fallado la carga y no hay podcast activo
        if (currentPodcast == null && hasStreamLoadFailed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay Podcast en directo ahora mismo",
                    color = MaterialTheme.colorScheme.error, // Opcional: usar color de error
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            AudioPlayer(
                player = activePlayer,
                isPlaying = isPlaying,
                onPlayPauseClick = {
                    if (currentPodcast != null) {
                        // Lógica para podcast
                        if (podcastExoPlayer.isPlaying) {
                            podcastExoPlayer.pause()
                            savePlaybackProgress(context, currentPodcast?.url, podcastExoPlayer.currentPosition)
                            Timber.d("Progress saved on pause: ${podcastExoPlayer.currentPosition}")
                        } else {
                            // Si se intenta reproducir podcast mientras stream está activo, detener stream
                            if (andainaStreamPlayer.isPlaying()) {
                                andainaStreamPlayer.stop()
                                hasStreamLoadFailed = false // Resetear el estado de fallo si cambiamos de stream
                            }
                            podcastExoPlayer.play()
                        }
                    } else {
                        // Lógica para stream
                        if (isAndainaStreamActive) {
                            if (andainaStreamPlayer.isPlaying()) {
                                andainaStreamPlayer.pause()
                            } else {
                                // Si se intenta reproducir stream mientras podcast está activo, detener podcast y guardar progreso
                                if (podcastExoPlayer.isPlaying) {
                                    podcastExoPlayer.pause()
                                    // Guardar progreso del podcast antes de cambiar a stream
                                    savePlaybackProgress(context, currentPodcast?.url, podcastExoPlayer.currentPosition)
                                    Timber.d("Progress saved on pause (switching to stream): ${podcastExoPlayer.currentPosition}")
                                }
                                // Intentar reproducir stream
                                andainaStreamPlayer.play()
                            }
                        } else {
                            Toast.makeText(context, "El streaming de Andaina no está activo en los ajustes.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                progress = if (currentPodcast != null && podcastDuration > 0) podcastProgress else 0f,
                duration = if (currentPodcast != null) podcastDuration else 0L,
                onProgressChange = { newProgress ->
                    if (currentPodcast != null && podcastDuration > 0) {
                        seekJob?.cancel()
                        seekJob = coroutineScope.launch {
                            delay(100)
                            val newPosition = (newProgress * podcastDuration).toLong()
                            podcastExoPlayer.seekTo(newPosition)
                            // Guardar progreso inmediatamente después de un seek
                            savePlaybackProgress(context, currentPodcast?.url, podcastExoPlayer.currentPosition)
                            Timber.d("Progress saved after seek: ${podcastExoPlayer.currentPosition}")
                        }
                    }
                },
                isLiveStream = isLiveStream,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // El botón de PlayStreaming es siempre visible en esta posición
            PlayStreaming(
                onClick = {
                    // Al pulsar el botón de streaming:
                    // Si está reproduciendo el stream, pausar.
                    // Si NO está reproduciendo el stream:
                    //   - Detener cualquier podcast reproduciendo y limpiar su estado.
                    //   - Si el stream está activo en ajustes, intentar reproducirlo.
                    //   - Resetear el estado de fallo si se pulsa el botón para permitir un nuevo intento.

                    if (andainaStreamPlayer.isPlaying()) {
                        andainaStreamPlayer.pause()
                    } else {
                        // Detener podcast si está reproduciendo al cambiar a stream
                        if (currentPodcast != null || podcastExoPlayer.isPlaying) {
                            podcastExoPlayer.stop()
                            podcastExoPlayer.clearMediaItems()
                            currentPodcast = null
                            savePlaybackProgress(context, null, 0L)
                            Timber.d("Cleared saved progress because switched to stream.")
                        }
                        // Intentar reproducir el stream solo si está activo en los ajustes
                        if (isAndainaStreamActive) {
                            hasStreamLoadFailed = false // Permitir un nuevo intento de carga del stream al pulsar el botón
                            andainaStreamPlayer.play()
                        } else {
                            Toast.makeText(context, "El streaming de Andaina no está activo en los ajustes.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 72.dp) // Ajusta el padding si es necesario
            )
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismissRequest = { showSettingsDialog = false }
        )
    }
}
