package com.example.paradigmaapp.android

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.paradigmaapp.android.api.AndainaStream
import com.example.paradigmaapp.android.api.ArchiveService
import com.example.paradigmaapp.android.audio.AudioPlayer
import com.example.paradigmaapp.android.audio.VolumeControl
import com.example.paradigmaapp.android.podcast.Podcast
import com.example.paradigmaapp.android.podcast.PodcastList
import com.example.paradigmaapp.android.ui.SearchBar
import com.example.paradigmaapp.android.ui.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.focus.onFocusChanged
import com.example.paradigmaapp.android.navigation.BottomNavigationBar
import com.example.paradigmaapp.android.screens.DownloadedPodcastScreen
import com.example.paradigmaapp.android.screens.OnGoingPodcastScreen
import com.example.paradigmaapp.android.screens.QueueScreen
import com.example.paradigmaapp.android.viewmodel.DownloadedPodcastViewModel
import com.example.paradigmaapp.android.viewmodel.OnGoingPodcastViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel


/**
 * Constantes para SharedPreferences utilizadas en la aplicación.
 */
private const val PREFS_NAME = "PodcastPlaybackPrefs"
private const val PREF_CURRENT_PODCAST_URL = "currentPodcastUrl"
private const val PREF_IS_STREAM_ACTIVE = "isStreamActive"
private const val PREF_PODCAST_POSITIONS = "podcastPositions"
// Constante para guardar la cola de reproducción
private const val PREF_PODCAST_QUEUE = "podcastQueue"
// Constante para guardar los podcasts descargados
private const val PREF_DOWNLOADED_PODCASTS = "downloadedPodcasts"


/**
 * Obtiene la instancia de [SharedPreferences] con el nombre específico para la aplicación.
 *
 * @param context El contexto de la aplicación.
 * @return Una instancia de [SharedPreferences].
 */
fun getSharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

/**
 * Guarda la posición de reproducción actual de un podcast específico en [SharedPreferences].
 *
 * La posición se guarda asociada a la URL del podcast para poder restaurarla posteriormente.
 *
 * @param context El contexto de la aplicación.
 * @param podcastUrl La URL del podcast del que se guarda la posición.
 * @param positionMillis La posición de reproducción en milisegundos.
 */
fun savePodcastPosition(context: Context, podcastUrl: String, positionMillis: Long) {
    val prefs = getSharedPreferences(context)
    val positions = prefs.getStringSet(PREF_PODCAST_POSITIONS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

    // Guardamos la posición en formato "url|posición", eliminando cualquier entrada anterior para la misma URL.
    positions.removeAll { it.startsWith("$podcastUrl|") }
    positions.add("$podcastUrl|$positionMillis")

    prefs.edit().apply {
        putStringSet(PREF_PODCAST_POSITIONS, positions)
        apply()
    }
    Timber.d("Saved position for podcast: $podcastUrl at $positionMillis")
}

/**
 * Obtiene la posición de reproducción guardada para un podcast específico desde [SharedPreferences].
 *
 * @param context El contexto de la aplicación.
 * @param podcastUrl La URL del podcast del que se quiere obtener la posición.
 * @return La posición de reproducción en milisegundos, o 0L si no se encuentra o hay un error.
 */
fun getPodcastPosition(context: Context, podcastUrl: String): Long {
    val prefs = getSharedPreferences(context)
    val positions = prefs.getStringSet(PREF_PODCAST_POSITIONS, mutableSetOf()) ?: return 0L

    return positions.find { it.startsWith("$podcastUrl|") }
        ?.split("|")?.get(1)?.toLongOrNull() ?: 0L
}

/**
 * Guarda la URL del podcast actualmente seleccionado en [SharedPreferences].
 *
 * Esto permite recordar qué podcast estaba en reproducción la última vez que se usó la aplicación.
 *
 * @param context El contexto de la aplicación.
 * @param podcastUrl La URL del podcast actualmente seleccionado, o null si no hay ninguno.
 */
fun saveCurrentPodcast(context: Context, podcastUrl: String?) {
    getSharedPreferences(context).edit().apply {
        putString(PREF_CURRENT_PODCAST_URL, podcastUrl)
        apply()
    }
    Timber.d("Saved current podcast: $podcastUrl")
}

/**
 * Carga la URL del podcast actualmente seleccionado desde [SharedPreferences].
 *
 * @param context El contexto de la aplicación.
 * @return La URL del podcast actualmente seleccionado, o null si no se guardó ninguno.
 */
fun loadCurrentPodcast(context: Context): String? {
    return getSharedPreferences(context).getString(PREF_CURRENT_PODCAST_URL, null)
}

/**
 * Guarda el estado de activación del stream de Andaina en [SharedPreferences].
 *
 * @param context El contexto de la aplicación.
 * @param isActive `true` si el stream debe estar activo, `false` en caso contrario.
 */
fun saveIsStreamActive(context: Context, isActive: Boolean) {
    getSharedPreferences(context).edit().putBoolean(PREF_IS_STREAM_ACTIVE, isActive).apply()
    Timber.d("Saved stream active state: $isActive")
}

/**
 * Carga el estado de activación del stream de Andaina desde [SharedPreferences].
 *
 * Por defecto, el stream se considera activo si no se ha guardado ningún estado previamente.
 *
 * @param context El contexto de la aplicación.
 * @return `true` si el stream debe estar activo, `false` en caso contrario.
 */
fun loadIsStreamActive(context: Context): Boolean {
    return getSharedPreferences(context).getBoolean(PREF_IS_STREAM_ACTIVE, true)
}

/**
 * Guarda la cola de reproducción de podcasts en SharedPreferences.
 *
 * @param context El contexto de la aplicación.
 * @param queue La lista de URLs de podcasts en la cola.
 */
fun savePodcastQueue(context: Context, queue: List<String>) {
    getSharedPreferences(context).edit().apply {
        putStringSet(PREF_PODCAST_QUEUE, queue.toSet())
        apply()
    }
    Timber.d("Saved podcast queue: $queue")
}

/**
 * Carga la cola de reproducción de podcasts desde SharedPreferences.
 *
 * @param context El contexto de la aplicación.
 * @return La lista de URLs de podcasts en la cola.
 */
fun loadPodcastQueue(context: Context): List<String> {
    return getSharedPreferences(context).getStringSet(PREF_PODCAST_QUEUE, emptySet())?.toList() ?: emptyList()
}

/**
 * Guarda los identificadores de los podcasts descargados en SharedPreferences.
 *
 * @param context El contexto de la aplicación.
 * @param downloadedIdentifiers La lista de identificadores de podcasts descargados.
 */
fun saveDownloadedPodcastIdentifiers(context: Context, downloadedIdentifiers: List<String>) {
    getSharedPreferences(context).edit().apply {
        putStringSet(PREF_DOWNLOADED_PODCASTS, downloadedIdentifiers.toSet())
        apply()
    }
    Timber.d("Saved downloaded podcast identifiers: $downloadedIdentifiers")
}

/**
 * Carga los identificadores de los podcasts descargados desde SharedPreferences.
 *
 * @param context El contexto de la aplicación.
 * @return La lista de identificadores de podcasts descargados.
 */
fun loadDownloadedPodcastIdentifiers(context: Context): List<String> {
    return getSharedPreferences(context).getStringSet(PREF_DOWNLOADED_PODCASTS, emptySet())?.toList() ?: emptyList()
}


/**
 * [Composable] principal de la aplicación que muestra la lista de podcasts,
 * controles de reproducción y ajustes.
 *
 * Esta pantalla se encarga de cargar los podcasts desde el servicio, filtrarlos
 * según la búsqueda del usuario, gestionar la reproducción de podcasts y el stream
 * de Andaina, y permitir la configuración de la aplicación.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AppScreen() {
    // Contexto y servicios
    val context = LocalContext.current
    val archiveService = remember { ArchiveService() }
    val andainaStreamPlayer = remember(context) { AndainaStream(context) }

    // ViewModels para las nuevas pantallas
    // Se pasa una lambda adaptada a getPodcastPosition para que el ViewModel solo reciba la URL
    val onGoingPodcastViewModel = remember {
        OnGoingPodcastViewModel { podcastUrl ->
            getPodcastPosition(context, podcastUrl)
        }
    }
    val downloadedPodcastViewModel = remember { DownloadedPodcastViewModel(context, archiveService) }
    val queueViewModel = remember { QueueViewModel(context) } // Inicializar con contexto

    // Estados de la UI y datos
    var initialPodcasts by remember { mutableStateOf<List<Podcast>>(emptyList()) }
    var filteredPodcasts by remember { mutableStateOf<List<Podcast>>(emptyList()) }
    var isLoadingInitial by remember { mutableStateOf(true) }
    var currentPodcast by remember { mutableStateOf<Podcast?>(null) }
    var searchText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Estados de reproducción del podcast
    var podcastProgress by remember { mutableStateOf(0f) }
    var podcastDuration by remember { mutableStateOf(0L) }
    var isPodcastPlaying by remember { mutableStateOf(false) }

    // Estados de reproducción del stream de Andaina
    var isAndainaStreamActive by remember { mutableStateOf(loadIsStreamActive(context)) }
    var isAndainaPlaying by remember { mutableStateOf(false) }
    var hasStreamLoadFailed by remember { mutableStateOf(false) }

    // Estado para controlar la búsqueda de podcasts (originalmente seekJob, se renombró o se puede eliminar si no se usa)
    var searchJob by remember { mutableStateOf<Job?>(null) } // Renombrado de seekJob para claridad

    // Estados para controlar la visibilidad de las nuevas pantallas
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showOnGoingPodcastsScreen by remember { mutableStateOf(false) }
    var showDownloadedPodcastsScreen by remember { mutableStateOf(false) }
    var showQueueScreen by remember { mutableStateOf(false) }

    // Estado para el foco del buscador
    val searchBarFocusRequester = remember { FocusRequester() }
    var isSearchBarFocused by remember { mutableStateOf(false) }

    // Estado para la lista de dispositivos Bluetooth
    val bluetoothDevices = remember { mutableStateListOf<String>() }
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // Lanzador para solicitar permisos de Bluetooth en tiempo de ejecución
    val requestBluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions.all { it.value }) {
                // Permisos concedidos, obtener la lista de dispositivos Bluetooth emparejados.
                if (bluetoothAdapter?.isEnabled == true) {
                    bluetoothDevices.clear()
                    bluetoothAdapter.bondedDevices.forEach { device ->
                        bluetoothDevices.add(device.name)
                    }
                }
            } else {
                Timber.d("Permisos de Bluetooth denegados")
            }
        }
    )

    /**
     * Función para verificar si se tienen los permisos de Bluetooth necesarios
     * y solicitarlos si no se tienen. Si los permisos ya están concedidos
     * y Bluetooth está activado, se obtiene la lista de dispositivos emparejados.
     */
    fun checkBluetoothPermissions() {
        val permissionsToRequest = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        val areAllGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!areAllGranted) {
            requestBluetoothPermissionLauncher.launch(permissionsToRequest)
        } else {
            // Permisos ya concedidos, obtener lista de dispositivos si Bluetooth está activado.
            if (bluetoothAdapter?.isEnabled == true) {
                bluetoothDevices.clear()
                bluetoothAdapter.bondedDevices.forEach { device ->
                    bluetoothDevices.add(device.name)
                }
            }
        }
    }

    // Se llama a la función para verificar los permisos de Bluetooth al iniciar la pantalla.
    LaunchedEffect(Unit) {
        checkBluetoothPermissions()
    }

    // Instancia del reproductor ExoPlayer para los podcasts.
    val podcastExoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Timber.e("Podcast ExoPlayer error: ${error.errorCodeName}")
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_ENDED -> {
                                currentPodcast?.let { podcast ->
                                    // Cuando un podcast termina, guardar su posición en 0L para permitir la reproducción de nuevo
                                    savePodcastPosition(context, podcast.url, 0L)

                                    // Intentar reproducir el siguiente podcast de la cola
                                    coroutineScope.launch {
                                        val nextPodcast = queueViewModel.dequeuePodcast(podcast.url)
                                        if (nextPodcast != null) {
                                            Timber.d("Playing next podcast from queue: ${nextPodcast.title}")
                                            currentPodcast = nextPodcast // Esto disparará el LaunchedEffect(currentPodcast)
                                        } else {
                                            Timber.d("Queue is empty. Stopping playback and keeping current podcast selected.")
                                            // Si no hay más podcasts en cola, no deseleccionar el podcast actual.
                                            // Esto permite que el usuario lo reproduzca de nuevo si lo desea.
                                        }
                                    }
                                }
                            }
                            // Si el reproductor está en estado READY y NO está reproduciendo, se considera pausado.
                            // Esto cubre el escenario de haber pausado manualmente.
                            Player.STATE_READY -> {
                                if (!isPlaying) { // !isPlaying significa que playWhenReady es false
                                    currentPodcast?.let { podcast ->
                                        savePodcastPosition(context, podcast.url, currentPosition)
                                        onGoingPodcastViewModel.updatePodcastProgress(podcast.url, currentPosition, duration)
                                    }
                                }
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        isPodcastPlaying = isPlaying
                        // Guardar la posición cuando se pausa, no solo cuando termina.
                        if (!isPlaying) { // Si ya no está reproduciendo, podría ser por pausa o por fin de reproducción.
                            currentPodcast?.let { podcast ->
                                savePodcastPosition(context, podcast.url, currentPosition)
                            }
                        }
                    }
                })
            }
    }

    // Estados y control del BottomSheet para el control de volumen y dispositivos de audio.
    val scaffoldState = rememberBottomSheetScaffoldState()
    var showVolumeBottomSheet by remember { mutableStateOf(false) }
    var currentVolumeState by remember { mutableFloatStateOf(podcastExoPlayer.volume) }

    // Listener para el reproductor del stream de Andaina.
    LaunchedEffect(andainaStreamPlayer.exoPlayer) {
        andainaStreamPlayer.exoPlayer?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                hasStreamLoadFailed = true
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isAndainaPlaying = isPlaying
            }
        })
    }

    // Efecto lanzado al iniciar y cuando cambia el texto de búsqueda.
    LaunchedEffect(Unit, searchText) {
        // Cargar la lista inicial de podcasts solo la primera vez.
        if (initialPodcasts.isEmpty()) {
            try {
                val allPodcasts = withContext(Dispatchers.IO) {
                    archiveService.fetchAllPodcasts("mario011")
                }
                initialPodcasts = allPodcasts
                isLoadingInitial = false

                // Inicializar ViewModels con la lista de podcasts
                onGoingPodcastViewModel.setAllPodcasts(allPodcasts)
                downloadedPodcastViewModel.setAllPodcasts(allPodcasts)
                queueViewModel.setAllPodcasts(allPodcasts) // Pasa la lista de todos los podcasts

                // Cargar el podcast actualmente guardado y su posición.
                val savedPodcastUrl = loadCurrentPodcast(context)
                Timber.d("Loaded saved podcast URL: $savedPodcastUrl")
                if (savedPodcastUrl != null) {
                    currentPodcast = allPodcasts.find { it.url == savedPodcastUrl }
                    Timber.d("Found podcast in list: ${currentPodcast?.title}")
                    // El LaunchedEffect(currentPodcast) se encargará de cargarlo y reproducirlo.
                    // Si no está en cola y solo se restauró el último, se reproducirá
                    // desde la posición guardada.
                }
            } catch (e: Exception) {
                Timber.e("Error loading podcasts: $e")
                isLoadingInitial = false
            }
        }

        // Filtrar la lista de podcasts según el texto de búsqueda.
        filteredPodcasts = initialPodcasts.filter { podcast ->
            podcast.title.contains(searchText, ignoreCase = true)
        }
    }

    // Efecto para actualizar periódicamente el estado de reproducción y el progreso del podcast.
    LaunchedEffect(podcastExoPlayer, andainaStreamPlayer) {
        while (isActive) {
            isPodcastPlaying = podcastExoPlayer.isPlaying
            isAndainaPlaying = andainaStreamPlayer.isPlaying()

            if (currentPodcast != null) {
                val duration = podcastExoPlayer.duration
                val currentPos = podcastExoPlayer.currentPosition
                podcastDuration = if (duration > 0 && duration != C.TIME_UNSET) duration else 0L
                podcastProgress =
                    if (podcastDuration > 0) currentPos.toFloat() / podcastDuration.toFloat() else 0f

                // Actualizar la posición de reproducción en el ViewModel de "Seguir Viendo"
                if (isPodcastPlaying || podcastExoPlayer.playbackState == Player.STATE_READY) { // Verificar si está reproduciendo O pausado (STATE_READY y no isPlaying)
                    onGoingPodcastViewModel.updatePodcastProgress(currentPodcast!!.url, currentPos, podcastDuration)
                }
            } else {
                podcastProgress = 0f
                podcastDuration = 0L
            }

            delay(100)
        }
    }

    // Efecto para manejar los cambios en el podcast seleccionado.
    LaunchedEffect(currentPodcast) {
        val podcast = currentPodcast

        if (podcast != null) {
            // Detener la reproducción del stream si está activo al seleccionar un podcast.
            if (andainaStreamPlayer.isPlaying()) {
                andainaStreamPlayer.stop()
            }

            // Cargar el podcast seleccionado con su posición guardada y comenzar la reproducción.
            val savedPosition = getPodcastPosition(context, podcast.url)
            podcastExoPlayer.clearMediaItems()
            podcastExoPlayer.setMediaItem(MediaItem.fromUri(podcast.url), savedPosition)
            podcastExoPlayer.prepare()
            podcastExoPlayer.play() // Iniciar la reproducción al seleccionar.

            // Guardar la URL del podcast actual.
            saveCurrentPodcast(context, podcast.url)
            Timber.d("Loaded podcast: ${podcast.title} at position: $savedPosition")
        } else {
            // Detener y limpiar el reproductor de podcasts al deseleccionar.
            podcastExoPlayer.stop()
            podcastExoPlayer.clearMediaItems()
            saveCurrentPodcast(context, null) // Guardar que no hay podcast actual.

            // Iniciar el stream si está activo y no ha fallado la carga.
            if (isAndainaStreamActive && !hasStreamLoadFailed) {
                andainaStreamPlayer.play()
            }
        }
    }

    // Efecto para manejar los cambios en el estado de activación del stream de Andaina.
    LaunchedEffect(isAndainaStreamActive) {
        if (isAndainaStreamActive) {
            // Iniciar el stream si no hay ningún podcast seleccionado y no se está reproduciendo.
            if (currentPodcast == null && !andainaStreamPlayer.isPlaying()) {
                andainaStreamPlayer.play()
            }
        } else {
            // Detener el stream si se desactiva.
            andainaStreamPlayer.stop()
        }
        saveIsStreamActive(context, isAndainaStreamActive)
    }


    // Efecto para actualizar el estado del volumen actual cuando cambia el volumen del reproductor.
    LaunchedEffect(podcastExoPlayer.volume) {
        currentVolumeState = podcastExoPlayer.volume
    }

    // Efecto para liberar los recursos de los reproductores al salir del Composable.
    DisposableEffect(Unit) {
        onDispose {
            // Guardar la posición actual del podcast antes de liberar el reproductor.
            currentPodcast?.let { podcast ->
                savePodcastPosition(context, podcast.url, podcastExoPlayer.currentPosition)
            }

            podcastExoPlayer.release()
            andainaStreamPlayer.release()
        }
    }

    // UI principal con BottomSheetScaffold
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            VolumeControl(
                player = podcastExoPlayer,
                onVolumeChanged = { newVolume ->
                    currentVolumeState = newVolume
                    podcastExoPlayer.volume = newVolume
                },
                currentVolume = currentVolumeState,
                availableBluetoothDevices = bluetoothDevices, // Proporciona la lista de dispositivos Bluetooth disponibles al control de volumen.
                onBluetoothDeviceSelected = { deviceName ->
                    // TODO: Implementar la lógica para la selección y conexión del dispositivo de audio Bluetooth.
                    Timber.d("Dispositivo Bluetooth seleccionado: $deviceName")
                    coroutineScope.launch { scaffoldState.bottomSheetState.hide() } // Cierra el BottomSheet al seleccionar un dispositivo.
                }
            )
        },
        sheetPeekHeight = 0.dp // Oculta el BottomSheet del control de volumen de forma predeterminada.
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Sección superior de la pantalla con la barra de búsqueda
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
                    onSearchTextChanged = { newText ->
                        searchText = newText
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(searchBarFocusRequester)
                        .onFocusChanged { focusState ->
                            isSearchBarFocused = focusState.isFocused
                        }
                )
            }

            // Contenido principal de la pantalla
            when {
                showOnGoingPodcastsScreen -> {
                    OnGoingPodcastScreen(
                        onGoingPodcastViewModel = onGoingPodcastViewModel,
                        onPodcastSelected = { podcast ->
                            currentPodcast = podcast
                            showOnGoingPodcastsScreen = false // Regresar a la pantalla principal
                        },
                        onBackClick = { showOnGoingPodcastsScreen = false }
                    )
                }
                showDownloadedPodcastsScreen -> {
                    DownloadedPodcastScreen(
                        downloadedPodcastViewModel = downloadedPodcastViewModel,
                        onPodcastSelected = { podcast ->
                            currentPodcast = podcast
                            showDownloadedPodcastsScreen = false // Regresar a la pantalla principal
                        },
                        onBackClick = { showDownloadedPodcastsScreen = false }
                    )
                }
                showQueueScreen -> {
                    QueueScreen(
                        queueViewModel = queueViewModel,
                        onPodcastSelected = { podcast ->
                            currentPodcast = podcast
                            showQueueScreen = false // Regresar a la pantalla principal
                        },
                        onBackClick = { showQueueScreen = false }
                    )
                }
                isLoadingInitial -> {
                    // Muestra un indicador de carga mientras se obtienen los podcasts iniciales.
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary // Asegura que el color sea primario
                        )
                    }
                }

                filteredPodcasts.isEmpty() -> {
                    // Muestra un mensaje si no hay podcasts disponibles o no se encontraron resultados en la búsqueda.
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchText.isBlank()) "No hay podcasts disponibles"
                            else "No se encontraron resultados",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    // Muestra la lista de podcasts filtrados.
                    PodcastList(
                        podcasts = filteredPodcasts,
                        onPodcastSelected = { podcast ->
                            // Lógica al hacer clic en un podcast de la lista.
                            // Si se selecciona el mismo podcast:
                            if (currentPodcast?.url == podcast.url) {
                                if (isPodcastPlaying) {
                                    // Si ya está reproduciéndose, pausarlo
                                    podcastExoPlayer.pause()
                                } else {
                                    // Si está pausado (STATE_READY y !isPlaying) o al final, reanudarlo o reiniciarlo
                                    if (podcastExoPlayer.playbackState == Player.STATE_ENDED || podcastExoPlayer.currentPosition == 0L) {
                                        podcastExoPlayer.seekTo(0L) // Reiniciar si ha terminado o está al principio
                                        podcastExoPlayer.play()
                                    } else if (podcastExoPlayer.playbackState == Player.STATE_READY) { // Si está en pausa
                                        podcastExoPlayer.play() // Reanudar
                                    }
                                }
                            } else {
                                // Si se selecciona un podcast diferente, establece este como el podcast actual.
                                currentPodcast = podcast // Esto disparará el LaunchedEffect(currentPodcast) para cargarlo y reproducirlo
                            }
                        },
                        onAddToQueue = { podcast ->
                            queueViewModel.addPodcastToQueue(podcast)
                            Timber.d("Podcast ${podcast.title} añadido a la cola.")
                        },
                        onRemoveFromQueue = { podcast ->
                            queueViewModel.removePodcastFromQueue(podcast)
                            Timber.d("Podcast ${podcast.title} eliminado de la cola.")
                        },
                        // onDownloadPodcast ahora debe aceptar dos parámetros: el podcast y el callback para el mensaje
                        onDownloadPodcast = { podcast, onMessageReady ->
                            downloadedPodcastViewModel.downloadPodcast(podcast) { message ->
                                // Mostrar un Snackbar o Toast con el mensaje
                                coroutineScope.launch {
                                    scaffoldState.snackbarHostState.showSnackbar(message)
                                }
                                // No es necesario invocar onMessageReady si ya manejamos el mensaje aquí con Snackbar
                            }
                        },
                        onDeleteDownload = { podcast ->
                            downloadedPodcastViewModel.deleteDownloadedPodcast(podcast)
                        },
                        // Pasar las listas de podcasts descargados y en cola para saber el estado
                        downloadedPodcastIdentifiers = downloadedPodcastViewModel.downloadedPodcastIdentifiers.collectAsState().value,
                        queuePodcastUrls = queueViewModel.queuePodcastUrls.collectAsState().value,
                        modifier = Modifier.weight(1f) // Permite que la lista ocupe el espacio restante.
                    )
                }
            }

            // Controles de reproducción de audio, mostrados en la parte inferior.
            // Pasa el título y la imagen del podcast actual al AudioPlayer.
            AudioPlayer(
                player = if (currentPodcast != null) podcastExoPlayer else andainaStreamPlayer.exoPlayer,
                isPlaying = if (currentPodcast != null) isPodcastPlaying else isAndainaPlaying,
                onPlayPauseClick = {
                    // Controla la reproducción/pausa del podcast o del stream según cuál esté activo.
                    if (currentPodcast != null) {
                        if (isPodcastPlaying) {
                            podcastExoPlayer.pause()
                        } else {
                            podcastExoPlayer.play()
                        }
                    } else {
                        // Si no hay podcast seleccionado, controla el stream de Andaina
                        if (isAndainaStreamActive) {
                            if (isAndainaPlaying) {
                                andainaStreamPlayer.pause()
                            } else {
                                andainaStreamPlayer.play()
                            }
                        }
                    }
                },
                progress = podcastProgress, // Progreso del podcast
                onProgressChange = { newProgress ->
                    // Buscar en el reproductor de podcasts
                    val seekPosition = (newProgress * podcastDuration).toLong()
                    podcastExoPlayer.seekTo(seekPosition)
                },
                isLiveStream = currentPodcast == null, // Considera en vivo si no hay podcast seleccionado
                podcastTitle = currentPodcast?.title, // Pasa el título del podcast
                podcastImage = currentPodcast?.imageUrl, // Pasa la URL de la imagen del podcast
                isAndainaPlaying = isAndainaPlaying,
                onPlayStreamingClick = {
                    // Alternar el estado del stream de Andaina
                    isAndainaStreamActive = !isAndainaStreamActive
                    if (isAndainaStreamActive) {
                        // Si se activa el stream, detener cualquier podcast en reproducción
                        currentPodcast = null
                    }
                },
                onPodcastInfoClick = { /* No hay una acción específica aquí aún, se podría abrir una pantalla de detalles del podcast */ },
                onVolumeIconClick = {
                    coroutineScope.launch {
                        if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
                            scaffoldState.bottomSheetState.hide()
                        } else {
                            scaffoldState.bottomSheetState.expand()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho disponible.
            )
            // Barra de navegación inferior
            BottomNavigationBar(
                onSearchClick = {
                    searchJob?.cancel() // Cancela cualquier búsqueda anterior si hay
                    coroutineScope.launch {
                        delay(100) // Pequeño delay para que la UI se asiente
                        searchBarFocusRequester.requestFocus()
                        searchText = "" // Limpiar el texto de búsqueda al ir a la pantalla principal
                        showOnGoingPodcastsScreen = false
                        showDownloadedPodcastsScreen = false
                        showQueueScreen = false
                    }
                },
                onOnGoingClick = {
                    showOnGoingPodcastsScreen = true
                    showDownloadedPodcastsScreen = false
                    showQueueScreen = false
                    searchBarFocusRequester.freeFocus() // Liberar foco del buscador
                },
                onDownloadedClick = {
                    showDownloadedPodcastsScreen = true
                    showOnGoingPodcastsScreen = false
                    showQueueScreen = false
                    searchBarFocusRequester.freeFocus() // Liberar foco del buscador
                },
                onQueueClick = {
                    showQueueScreen = true
                    showOnGoingPodcastsScreen = false
                    showDownloadedPodcastsScreen = false
                    searchBarFocusRequester.freeFocus() // Liberar foco del buscador
                },
                onSettingsClick = { showSettingsDialog = true }
            )
        }
    }

    // Dialogo de ajustes
    if (showSettingsDialog) {
        if (showSettingsDialog) {
            SettingsScreen(
                onDismissRequest = { showSettingsDialog = false },
                isStreamActive = isAndainaStreamActive,
                onStreamActiveChanged = { newIsActive ->
                    isAndainaStreamActive = newIsActive
                    showSettingsDialog = false
                }
            )
        }
    }
}