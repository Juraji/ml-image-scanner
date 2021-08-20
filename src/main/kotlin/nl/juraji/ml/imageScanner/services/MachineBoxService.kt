package nl.juraji.ml.imageScanner.services

import nl.juraji.ml.imageScanner.configuration.MachineBoxConfiguration
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.filePart
import nl.juraji.ml.imageScanner.util.multiPartBody
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpCookie
import org.springframework.http.MediaType
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.file.Path
import java.nio.file.Paths

abstract class MachineBoxService(
    private val fileService: FileService,
    private val machineBoxWebClient: WebClient,
    configuration: MachineBoxConfiguration
) {
    private val stateFilePath: Path = Paths.get(configuration.stateFile)

    protected fun <R> withReadStateManagement(supplier: StateMgmtBlock.() -> Mono<R>): Mono<R> =
        withStateManagement(true, supplier)

    protected fun <R> withStateManagement(supplier: StateMgmtBlock.() -> Mono<R>): Mono<R> =
        withStateManagement(false, supplier)

    private fun <R> withStateManagement(preflightOnly: Boolean, supplier: StateMgmtBlock.() -> Mono<R>): Mono<R> =
        uploadState()
            .map { StateMgmtBlock(it, stateFilePath) }
            .flatMap { state ->
                val response = supplier.invoke(state)

                if (preflightOnly) response
                else response.flatMap { res -> saveState(state).thenReturn(res!!) }
            }

    private fun uploadState(): Mono<MultiValueMap<String, HttpCookie>> = Mono.just(stateFilePath)
        .flatMap { fileService.fileExists(it) }
        .flatMap { stateExists ->
            @Suppress("UNCHECKED_CAST")
            val toCookiesMono: (ClientResponse) -> Mono<MultiValueMap<String, HttpCookie>> = {
                Mono.just(it.cookies() as MultiValueMap<String, HttpCookie>)
            }

            if (stateExists) machineBoxWebClient
                .post()
                .uri("/state")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .multiPartBody { filePart("file", stateFilePath) }
                .exchangeToMono(toCookiesMono)
                .doOnNext { logger.info("Pre-flight: Restored $stateFilePath as machine state, continuing with cookies: $it") }
            else machineBoxWebClient
                .get()
                .uri("/liveness")
                .exchangeToMono(toCookiesMono)
                .doOnNext { logger.info("Pre-flight: No state persisted, continuing with cookies: $it") }
        }

    private fun saveState(state: StateMgmtBlock): Mono<Void> {
        val dataBuffers: Flux<DataBuffer> = machineBoxWebClient
            .get()
            .uri("/state")
            .cookies(state::applyStateCookies)
            .retrieve()
            .bodyToFlux()

        return fileService.writeDataBuffersTo(dataBuffers, stateFilePath)
            .doOnSuccess { logger.info("Post-flight: Persisted machine state in $stateFilePath") }
    }

    companion object : LoggerCompanion(MachineBoxService::class)

    data class StateMgmtBlock(
        val cookies: MultiValueMap<String, HttpCookie>,
        val stateFilePath: Path,
    ) {
        fun applyStateCookies(target: MultiValueMap<String, String>) =
            cookies.forEach { (key, cookies) ->
                target.merge(key, cookies.map(HttpCookie::getValue)) { l, r -> l + r }
            }
    }
}
