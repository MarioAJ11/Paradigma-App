package com.example.paradigmaapp.android.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.repository.contracts.EpisodioRepository
import java.io.IOException

/**
 * PagingSource para cargar episodios de un programa específico desde el EpisodioRepository.
 * Se encarga de la lógica de paginación, solicitando páginas a la API a medida que el usuario se desplaza.
 *
 * @param episodioRepository El repositorio desde donde se obtendrán los episodios.
 * @param programaId El ID del programa para el cual se cargarán los episodios.
 *
 * @author Mario Alguacil Juárez
 */
class EpisodioPagingSource(
    private val episodioRepository: EpisodioRepository,
    private val programaId: Int
) : PagingSource<Int, Episodio>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Episodio> {
        val paginaActual = params.key ?: 1
        // Guardamos el tamaño de página solicitado para la comparación.
        val tamanoDePagina = params.loadSize

        return try {
            val episodios = episodioRepository.getEpisodiosPorPrograma(
                programaId = programaId,
                page = paginaActual,
                perPage = tamanoDePagina
            )

            // Si el número de episodios recibidos es menor que el que pedimos,
            // significa que esta es la última página. En ese caso, nextKey es null.
            val siguientePagina = if (episodios.size < tamanoDePagina) {
                null
            } else {
                paginaActual + 1
            }

            LoadResult.Page(
                data = episodios,
                prevKey = if (paginaActual == 1) null else paginaActual - 1,
                nextKey = siguientePagina
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Episodio>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}