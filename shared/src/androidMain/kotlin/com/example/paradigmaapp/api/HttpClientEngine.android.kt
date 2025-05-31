package com.example.paradigmaapp.api

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Esta es mi implementación `actual` de [provideHttpClientEngine] para la plataforma Android.
 * Proporciono el motor [OkHttp] de Ktor, que es una opción robusta y popular
 * para realizar peticiones HTTP en Android, ya que se basa en la librería OkHttp de Square.
 *
 * @author Mario Alguacil Juárez
 */
internal actual fun provideHttpClientEngine(): HttpClientEngineFactory<*> = OkHttp