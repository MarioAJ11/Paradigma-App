package com.example.paradigmaapp.api

import io.ktor.client.engine.HttpClientEngineFactory

/**
 * Proporciona una factoría para el motor HTTP específico de la plataforma.
 * @author Mario Alguacil Juárez
 */
internal expect fun provideHttpClientEngine(): HttpClientEngineFactory<*>