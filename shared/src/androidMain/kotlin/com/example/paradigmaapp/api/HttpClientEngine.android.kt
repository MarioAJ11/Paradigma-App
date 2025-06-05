package com.example.paradigmaapp.api

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Implementación `actual` de [provideHttpClientEngine] para la plataforma Android.
 * Proporciona el motor [OkHttp] de Ktor, una opción robusta y popular
 * para peticiones HTTP en Android, basada en la librería OkHttp de Square.
 *
 * @author Mario Alguacil Juárez
 */
internal actual fun provideHttpClientEngine(): HttpClientEngineFactory<*> = OkHttp