package com.example.paradigmaapp.cache

import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.ImagenProgramaInfo
import com.example.paradigmaapp.model.Programa

// --- MAPPER PARA PROGRAMA ---

// Convierte desde la entidad de la base de datos al modelo de la app
fun ProgramaEntity.toDomain(): Programa {
    return Programa(
        id = this.id.toInt(),
        name = this.name,
        slug = this.slug,
        description = this.description,
        count = this.count?.toInt(),
        // Reconstruimos el objeto de la imagen
        imagenDelPrograma = this.imageUrl?.let { ImagenProgramaInfo(guid = it) }
    )
}

// Convierte desde el modelo de la app a la entidad de la base de datos
fun Programa.toEntity(): ProgramaEntity {
    return ProgramaEntity(
        id = this.id.toLong(),
        name = this.name,
        slug = this.slug,
        description = this.description,
        count = this.count?.toLong(),
        imageUrl = this.imageUrl
    )
}


// --- MAPPER PARA EPISODIO ---

fun EpisodioEntity.toDomain(): Episodio {
    return Episodio(
        id = this.id.toInt(),
        // Para el título y contenido, creamos el objeto RenderedContent que espera el modelo
        renderedTitle = com.example.paradigmaapp.model.RenderedContent(rendered = this.title),
        renderedContent = this.content?.let { com.example.paradigmaapp.model.RenderedContent(rendered = it) },
        slug = "", // El slug no se guarda en la caché, no es crítico para la UI
        date = this.date,
        duration = this.duration,
        urlDelPodcast = this.archiveUrl,
        // Los IDs de programa y la imagen se reconstruyen a partir de los datos guardados
        programaIds = listOf(this.programaId.toInt()),
        embedded = this.imageUrl?.let {
            com.example.paradigmaapp.model.Embedded(
                featuredMedia = listOf(
                    com.example.paradigmaapp.model.FeaturedMedia(id = 0, sourceUrl = it)
                )
            )
        }
    )
}

// Fíjate que esta función necesita el programaId como parámetro, ya que el objeto Episodio no lo contiene.
fun Episodio.toEntity(programaId: Int): EpisodioEntity {
    return EpisodioEntity(
        id = this.id.toLong(),
        title = this.title,
        content = this.content,
        archiveUrl = this.archiveUrl,
        imageUrl = this.imageUrl,
        date = this.date,
        duration = this.duration,
        programaId = programaId.toLong()
    )
}
