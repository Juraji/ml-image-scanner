package nl.juraji.ml.imageScanner.util

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.ParallelFlux

// Boolean based path switching
fun <T, R> Mono<T>.iif(
    predicate: (T) -> Boolean,
    onTrue: Mono<R>,
    onFalse: Mono<R>,
): Mono<R> = flatMap {
    if (predicate(it)) onTrue
    else onFalse
}

fun <R> Mono<Boolean>.iif(
    onTrue: Mono<R>,
    onFalse: Mono<R>,
) = iif({ it }, onTrue, onFalse)

// Blocking termination
fun <T> Mono<T>.blockAndCatch(): Result<T?> = runCatching(this::block)
fun <T> Flux<T>.blockLastAndCatch(): Result<T?> = runCatching(this::blockLast)
fun <T> ParallelFlux<T>.blockLastAndCatch(): Result<T?> = runCatching { sequential().blockLast() }
