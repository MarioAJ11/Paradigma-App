package com.example.paradigmaapp.api

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

/**
 * Proporciona el motor Darwin para la plataforma iOS. (Placeholder para foco Android)
 * @author Mario Alguacil Juárez
 */
internal actual fun provideHttpClientEngine(): HttpClientEngineFactory<*> = Darwin