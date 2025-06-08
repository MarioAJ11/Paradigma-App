package com.example.paradigmaapp.cache

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Implementación 'actual' de [DatabaseDriverFactory] para la plataforma Android.
 * Utiliza el [Context] de Android para crear una base de datos SQLite en el sistema de archivos de la app.
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        // AppDatabase.Schema fue generado automáticamente por SQLDelight a partir de nuestro archivo .sq
        return AndroidSqliteDriver(AppDatabase.Schema, context, "AppDatabase.db")
    }
}