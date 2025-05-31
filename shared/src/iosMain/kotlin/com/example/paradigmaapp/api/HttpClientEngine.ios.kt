package com.example.paradigmaapp.api

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

/**
 * Esta es mi implementación `actual` de [provideHttpClientEngine] para la plataforma iOS.
 * Proporciono el motor [Darwin] de Ktor, que utiliza las capacidades de red nativas
 * de iOS (NSURLSession) para un rendimiento y comportamiento óptimos en esta plataforma.
 * (El comentario original "Placeholder para foco Android" ya no aplica si estoy desarrollando para iOS también).
 *
 * @author Mario Alguacil Juárez
 */
internal actual fun provideHttpClientEngine(): HttpClientEngineFactory<*> = Darwin