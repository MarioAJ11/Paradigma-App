package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.repository.contracts.EpisodioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * ViewModel responsable de gestionar la lógica y el estado de los episodios descargados.
 * Interactúa con [AppPreferences] para persistir la lista de episodios descargados y
 * con [EpisodioRepository] para obtener detalles de los episodios si es necesario.
 * También maneja las operaciones de descarga y eliminación de archivos físicos.
 *
 * @property appPreferences Instancia de [AppPreferences] para acceder a las preferencias guardadas.
 * @property episodioRepository Instancia de [EpisodioRepository] para obtener datos de episodios.
 * @property applicationContext El [Context] de la aplicación, necesario para operaciones de sistema de archivos.
 *
 * @author Mario Alguacil Juárez
 */
class DownloadedEpisodioViewModel(
    private val appPreferences: AppPreferences,
    private val episodioRepository: EpisodioRepository, //
    private val applicationContext: Context
) : ViewModel() {

    // StateFlow para la lista de IDs de episodios descargados.
    private val _downloadedEpisodeIds = MutableStateFlow<List<Int>>(emptyList())
    val downloadedEpisodeIds: StateFlow<List<Int>> = _downloadedEpisodeIds.asStateFlow()

    // StateFlow para la lista completa de objetos Episodio que están descargados, para la UI.
    private val _downloadedEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val downloadedEpisodios: StateFlow<List<Episodio>> = _downloadedEpisodios.asStateFlow()

    // Caché de todos los episodios disponibles; se establece externamente (ej. desde MainViewModel).
    // Ayuda a evitar llamadas innecesarias a la red si el episodio ya está en esta lista.
    private var allAvailableEpisodesCache: List<Episodio> = emptyList()

    // Límite máximo de episodios que se permiten descargar.
    private val MAX_DOWNLOADS = 10 //

    init {
        loadDownloadedState() // Carga el estado de las descargas al inicializar el ViewModel.
    }

    /**
     * Establece la lista de todos los episodios disponibles en la aplicación.
     * Esta caché se utiliza para construir eficientemente la lista de [downloadedEpisodios]
     * sin necesidad de obtener siempre los detalles del episodio desde el repositorio.
     *
     * @param episodes Lista de todos los [Episodio]s disponibles.
     */
    fun setAllAvailableEpisodes(episodes: List<Episodio>) {
        allAvailableEpisodesCache = episodes
        // Si la caché de episodios se actualiza, es necesario reconstruir la lista de objetos descargados.
        viewModelScope.launch(Dispatchers.IO) {
            updateDownloadedEpisodiosListFromIds()
        }
    }

    /**
     * Carga la lista de IDs de episodios descargados desde [AppPreferences]
     * y luego actualiza la lista de objetos [Episodio] (`_downloadedEpisodios`).
     */
    private fun loadDownloadedState() {
        viewModelScope.launch(Dispatchers.IO) { // Operaciones de SharedPreferences en hilo de IO.
            _downloadedEpisodeIds.value = appPreferences.loadDownloadedEpisodeIds() //
            updateDownloadedEpisodiosListFromIds()
        }
    }

    /**
     * Guarda la lista actual de IDs de episodios descargados en [AppPreferences].
     */
    private fun saveDownloadedState() {
        viewModelScope.launch(Dispatchers.IO) { // Operaciones de SharedPreferences en hilo de IO.
            appPreferences.saveDownloadedEpisodeIds(_downloadedEpisodeIds.value) //
        }
    }

    /**
     * Actualiza la lista `_downloadedEpisodios` (objetos [Episodio] completos)
     * basándose en la lista actual de `_downloadedEpisodeIds`.
     * Intenta obtener los detalles del episodio desde `allAvailableEpisodesCache` primero;
     * si no se encuentra, lo busca a través del [episodioRepository].
     */
    private suspend fun updateDownloadedEpisodiosListFromIds() {
        val episodeDetailsList = mutableListOf<Episodio>()
        for (id in _downloadedEpisodeIds.value) {
            val cachedEpisodio = allAvailableEpisodesCache.find { it.id == id }
            if (cachedEpisodio != null) {
                episodeDetailsList.add(cachedEpisodio)
            } else {
                try {
                    episodioRepository.getEpisodio(id)?.let { fetchedEpisodio ->
                        episodeDetailsList.add(fetchedEpisodio)
                    }
                    // Si no se encuentra el episodio (getEpisodio devuelve null), no se añade.
                    // Esto podría ocurrir si un ID descargado ya no existe en el backend.
                } catch (e: Exception) {
                    // Considerar registrar este error en un sistema de monitoreo.
                    // No se añade el episodio a la lista si hay un error al obtener sus detalles.
                }
            }
        }
        _downloadedEpisodios.value = episodeDetailsList
    }

    /**
     * Inicia la descarga de un episodio.
     * Verifica si el episodio ya está descargado o si se ha alcanzado el límite de descargas.
     * El archivo se guarda en el directorio de archivos interno de la aplicación.
     *
     * @param episodio El [Episodio] a descargar.
     * @param onMessage Callback para notificar a la UI sobre el progreso o resultado de la descarga (éxito/error).
     * Recibe un [String] con el mensaje.
     */
    fun downloadEpisodio(episodio: Episodio, onMessage: (String) -> Unit) {
        if (episodio.archiveUrl == null) {
            onMessage("El episodio '${episodio.title}' no tiene URL de descarga.")
            return
        }
        if (_downloadedEpisodeIds.value.contains(episodio.id)) {
            onMessage("El episodio '${episodio.title}' ya está descargado.")
            return
        }
        if (_downloadedEpisodeIds.value.size >= MAX_DOWNLOADS) {
            onMessage("Máximo de $MAX_DOWNLOADS episodios descargados alcanzado.")
            return
        }

        onMessage("Descargando '${episodio.title}'...")
        viewModelScope.launch(Dispatchers.IO) { // Operaciones de red y de sistema de archivos en hilo de IO.
            val sanitizedSlug = episodio.slug.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val fileName = "${episodio.id}_$sanitizedSlug.mp3" // Asume extensión .mp3.
            val file = File(applicationContext.filesDir, fileName) //

            try {
                val url = URL(episodio.archiveUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000 // 15 segundos
                connection.readTimeout = 60000    // 60 segundos (descargas largas)
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Error del servidor al descargar: ${connection.responseCode} ${connection.responseMessage}")
                }

                FileOutputStream(file).use { output ->
                    connection.inputStream.use { input ->
                        input.copyTo(output)
                    }
                }

                _downloadedEpisodeIds.value = _downloadedEpisodeIds.value + episodio.id
                saveDownloadedState()
                // Actualiza la lista de objetos Episodio para reflejar la nueva descarga.
                updateDownloadedEpisodiosListFromIds() // O añadir directamente y luego ordenar si es necesario

                withContext(Dispatchers.Main) {
                    onMessage("Descarga de '${episodio.title}' completada.")
                }
            } catch (e: Exception) {
                file.delete() // Intenta eliminar el archivo parcial si la descarga falló.
                withContext(Dispatchers.Main) {
                    onMessage("Error al descargar '${episodio.title}'.")
                }
                // Considerar registrar `e` en un sistema de monitoreo.
            }
        }
    }

    /**
     * Elimina un episodio previamente descargado.
     * Esto incluye borrar el archivo físico del almacenamiento y actualizar el estado persistido.
     *
     * @param episodio El [Episodio] a eliminar de las descargas.
     */
    fun deleteDownloadedEpisodio(episodio: Episodio) {
        viewModelScope.launch(Dispatchers.IO) { // Operaciones de sistema de archivos en hilo de IO.
            val sanitizedSlug = episodio.slug.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val fileName = "${episodio.id}_$sanitizedSlug.mp3"
            val file = File(applicationContext.filesDir, fileName)

            var fileDeleted = false
            if (file.exists()) {
                fileDeleted = file.delete()
                // Si file.delete() es false, podría loggearse en producción.
            } else {
                // El archivo no existía, lo cual puede ser normal si ya se borró o la ruta es incorrecta.
                // Consideramos la operación "exitosa" en términos de estado si el ID estaba en la lista.
                fileDeleted = true // Para fines de actualizar el estado, si no existía es como si se hubiera borrado.
            }

            // Actualizar el estado solo si el archivo se eliminó (o no existía) y el ID está en la lista.
            if (fileDeleted && _downloadedEpisodeIds.value.contains(episodio.id)) {
                _downloadedEpisodeIds.value = _downloadedEpisodeIds.value - episodio.id
                saveDownloadedState()
                // Actualiza la lista de objetos Episodio para reflejar la eliminación.
                _downloadedEpisodios.value = _downloadedEpisodios.value.filterNot { it.id == episodio.id }
            }
            // Se podría añadir un callback `onMessage` si se quiere notificar a la UI.
        }
    }

    /**
     * Obtiene la ruta del archivo local para un episodio descargado.
     * Esta ruta es necesaria para que el reproductor de audio pueda acceder al archivo.
     *
     * @param episodeId El ID del episodio.
     * @return La ruta absoluta al archivo si está descargado y existe en el sistema de archivos;
     * `null` en caso contrario o si no se puede determinar el nombre del archivo.
     */
    fun getDownloadedFilePathByEpisodeId(episodeId: Int): String? {
        // Para construir el nombre del archivo, se necesita el slug del episodio.
        // Intenta encontrar el episodio en la lista de descargados o en la caché global.
        val episodio = _downloadedEpisodios.value.find { it.id == episodeId }
            ?: allAvailableEpisodesCache.find { it.id == episodeId }

        return if (episodio != null) {
            val sanitizedSlug = episodio.slug.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val fileName = "${episodio.id}_$sanitizedSlug.mp3"
            val file = File(applicationContext.filesDir, fileName)
            if (file.exists()) file.absolutePath else null
        } else {
            null // No se pudo encontrar el episodio para determinar el nombre del archivo.
        }
    }

    /**
     * Verifica si un episodio específico está actualmente marcado como descargado.
     * Nota: Esto no verifica si el archivo físico existe, solo si el ID está en la lista de seguimiento.
     *
     * @param episodeId El ID del episodio a verificar.
     * @return `true` si el episodio está en la lista de IDs descargados, `false` en caso contrario.
     */
    fun isEpisodeDownloaded(episodeId: Int): Boolean {
        return _downloadedEpisodeIds.value.contains(episodeId)
    }
}