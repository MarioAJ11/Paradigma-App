package com.example.paradigmaapp.api

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

/**
 * Implementación `actual` de [provideHttpClientEngine] para la plataforma iOS.
 * Proporciona el motor [Darwin] de Ktor, que utiliza las capacidades de red nativas
 * de iOS (NSURLSession) para un rendimiento y comportamiento óptimos.
 *
 * @author Mario Alguacil Juárez
 */
internal actual fun provideHttpClientEngine(): HttpClientEngineFactory<*> = Darwin