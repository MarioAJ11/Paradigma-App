package com.example.paradigmaapp.cache

/**
 * Clase de ayuda que inicializa la base de datos usando la factoría de drivers
 * y proporciona acceso a las consultas generadas por SQLDelight.
 *
 * @param databaseDriverFactory La factoría específica de la plataforma para crear el driver.
 */
class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = AppDatabase(databaseDriverFactory.createDriver())

    // Expone las consultas para que el resto del código pueda usarlas.
    // SQLDelight crea estos nombres a partir de los nombres de tus tablas.
    val programaQueries = database.programaEntityQueries
    val episodioQueries = database.episodioEntityQueries
    val keyValueStoreQueries = database.keyValueStoreQueries
}