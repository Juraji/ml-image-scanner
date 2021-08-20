package nl.juraji.ml.imageScanner.util

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.ParallelFlux

fun <T> Mono<T>.blockAndCatch(): Result<T?> = runCatching(this::block)
fun <T> ParallelFlux<T>.blockLastAndCatch(): Result<T?> = runCatching { sequential().blockLast() }
fun <T> Flux<T>.blockLastAndCatch(): Result<T?> = runCatching(this::blockLast)