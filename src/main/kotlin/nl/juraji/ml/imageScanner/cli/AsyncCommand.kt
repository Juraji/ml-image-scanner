package nl.juraji.ml.imageScanner.cli

import kotlinx.cli.Subcommand
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.blockAndCatch
import nl.juraji.ml.imageScanner.util.blockLastAndCatch
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.ParallelFlux

abstract class AsyncCommand(
    name: String, actionDescription: String
) : Subcommand(name, actionDescription) {

    final override fun execute() {
        val result = when (val publisher = executeAsync()) {
            is Mono -> publisher.blockAndCatch()
            is Flux -> publisher.blockLastAndCatch()
            is ParallelFlux -> publisher.blockLastAndCatch()
            else -> throw UnsupportedOperationException("Publisher of type ${publisher.javaClass} is not supported!")
        }

        result.onFailure { logger.error("Command execution failed", it) }
    }

    abstract fun executeAsync(): Publisher<*>

    companion object : LoggerCompanion(AsyncCommand::class)
}