package nl.juraji.ml.imageScanner.util

import reactor.core.publisher.Mono

// Boolean based path switching
fun <T : Any, R> Mono<T>.iif(
    onTrue: Mono<R>,
    onFalse: Mono<R>,
    predicate: (T) -> Boolean,
): Mono<R> = flatMap {
    if (predicate(it)) onTrue
    else onFalse
}

fun <R> Mono<Boolean>.iif(
    onTrue: Mono<R>,
    onFalse: Mono<R>,
) = iif(onTrue, onFalse) { it }
