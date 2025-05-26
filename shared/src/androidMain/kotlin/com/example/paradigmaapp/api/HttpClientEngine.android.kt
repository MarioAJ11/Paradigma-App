package com.example.paradigmaapp.api

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Proporciona el motor OkHttp para la plataforma Android.
 * @author Mario Alguacil Juárez
 */
internal actual fun provideHttpClientEngine(): HttpClientEngineFactory<*> = OkHttp