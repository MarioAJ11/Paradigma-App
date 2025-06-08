package com.example.paradigmaapp.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * Implementación 'actual' de [DatabaseDriverFactory] para la plataforma iOS.
 * Utiliza el driver nativo de SQLDelight, que funciona con la implementación de SQLite en iOS.
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        // Para iOS, la implementación es más sencilla, ya que no requiere un 'contexto'.
        return NativeSqliteDriver(AppDatabase.Schema, "AppDatabase.db")
    }
}