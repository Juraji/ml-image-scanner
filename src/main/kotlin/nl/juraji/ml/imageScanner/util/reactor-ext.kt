package nl.juraji.ml.imageScanner.util

import reactor.core.publisher.Mono

fun <T> Mono<T>.blockAndCatch(): Result<T?> = runCatching(this::block)