package nl.juraji.ml.imageScanner.cli

import kotlinx.cli.Subcommand
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.ParallelFlux

abstract class AsyncCommand(
    name: String,
    actionDescription: String
) : Subcommand(name, actionDescription) {

    final override fun execute() {
        val result = when (val publisher = executeAsync()) {
            is Mono -> publisher.runCatching(Mono<*>::block)
            is Flux -> publisher.runCatching(Flux<*>::blockLast)
            is ParallelFlux -> publisher.sequential().runCatching(Flux<*>::blockLast)
            else -> throw UnsupportedOperationException("Publisher of type ${publisher.javaClass} is not supported!")
        }

        result.onFailure { logger.error("Command execution failed", it) }
    }

    abstract fun executeAsync(): Publisher<*>

    companion object : LoggerCompanion(AsyncCommand::class)
}