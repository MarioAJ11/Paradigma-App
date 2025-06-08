package com.example.paradigmaapp.cache

import app.cash.sqldelight.db.SqlDriver

/**
 * Define la "promesa" o el contrato para una factoría de drivers de base de datos.
 * Cada plataforma (Android, iOS) deberá proporcionar una implementación 'actual' de esta clase.
 */
expect class DatabaseDriverFactory {
    /**
     * Crea y devuelve un driver de base de datos SQLDelight específico para la plataforma.
     */
    fun createDriver(): SqlDriver
}