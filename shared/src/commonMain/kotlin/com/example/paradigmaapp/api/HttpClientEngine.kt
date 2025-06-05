package com.example.paradigmaapp.api

import io.ktor.client.engine.HttpClientEngineFactory

/**
 * Proporciona una factoría para el motor HTTP (`HttpClientEngineFactory`) de Ktor,
 * permitiendo implementaciones específicas por plataforma (Android, iOS).
 * La implementación real (`actual`) se encuentra en los módulos `androidMain` e `iosMain`.
 * Esto permite utilizar motores HTTP optimizados para cada plataforma
 * (ej. OkHttp en Android, Darwin en iOS) sin modificar el código común que consume `ktorClient`.
 *
 * @author Mario Alguacil Juárez
 */
internal expect fun provideHttpClientEngine(): HttpClientEngineFactory<*>