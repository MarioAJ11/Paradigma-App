package com.example.paradigmaapp.api

import io.ktor.client.engine.HttpClientEngineFactory

/**
 * Declaro una función `expect` para proporcionar una factoría del motor HTTP (`HttpClientEngineFactory`)
 * que sea específica para cada plataforma (Android, iOS).
 * La implementación real (`actual`) de esta función se encuentra en los módulos
 * `androidMain` e `iosMain` respectivamente. Esto me permite usar diferentes motores
 * HTTP optimizados para cada plataforma (OkHttp en Android, Darwin en iOS)
 * sin cambiar el código común que usa el `ktorClient`.
 *
 * @author Mario Alguacil Juárez
 */
internal expect fun provideHttpClientEngine(): HttpClientEngineFactory<*>