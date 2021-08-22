package nl.juraji.ml.imageScanner.util

import reactor.core.publisher.Mono

// Boolean based path switching
fun <T : Any, R> Mono<T>.iif(
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
