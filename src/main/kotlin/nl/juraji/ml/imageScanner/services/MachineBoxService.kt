package nl.juraji.ml.imageScanner.services

import nl.juraji.ml.imageScanner.configuration.MachineBoxConfiguration
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.filePart
import nl.juraji.ml.imageScanner.util.iif
import nl.juraji.ml.imageScanner.util.multiPartBody
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.file.Path

abstract class MachineBoxService(
    private val fileService: FileService,
    private val machineBoxWebClient: WebClient,
    configuration: MachineBoxConfiguration
) {
    private val stateFilePath: Path = configuration.stateFile

    protected fun <R> withManagedStateClient(supplier: WebClient.() -> Mono<R>): Mono<R> =
        withManagedStateClient(true, supplier)

    protected fun <R> withManagedStatePersistingClient(supplier: WebClient.() -> Mono<R>): Mono<R> =
        withManagedStateClient(false, supplier)

    private fun <R> withManagedStateClient(readOnly: Boolean, supplier: WebClient.() -> Mono<R>): Mono<R> =
        uploadStateAndGetSessionCookies()
            .map { cookies ->
                machineBoxWebClient.mutate()
                    .defaultCookies { target -> target.addAll(cookies) }
                    .build()
            }
            .flatMap { client ->
                val response = supplier.invoke(client)

                if (readOnly) response
                else response.flatMap { res -> saveState(client).thenReturn(res!!) }
            }

    private fun uploadStateAndGetSessionCookies(): Mono<MultiValueMap<String, String>> = Mono.just(stateFilePath)
        .flatMap(fileService::fileExists)
        .iif(
            machineBoxWebClient
                .post()
                .uri(STATE_ENDPOINT)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .multiPartBody { filePart("file", stateFilePath) }
                .exchangeToMono(this::mapSessionCookies)
                .doOnNext { logger.debug("Pre-flight: Restored $stateFilePath as machine state, continuing with cookies: $it") },
            machineBoxWebClient
                .get()
                .uri(NOOP_ENDPOINT)
                .exchangeToMono(this::mapSessionCookies)
                .doOnNext { logger.debug("Pre-flight: No state persisted, continuing with cookies: $it") }
        )

    private fun saveState(client: WebClient): Mono<Void> {
        val dataBuffers: Flux<DataBuffer> = client
            .get()
            .uri(STATE_ENDPOINT)
            .retrieve()
            .bodyToFlux()

        return fileService
            .writeDataBuffersTo(dataBuffers, stateFilePath)
            .doOnSuccess { logger.debug("Post-flight: Persisted machine state in $stateFilePath") }
    }

    private fun mapSessionCookies(response: ClientResponse): Mono<MultiValueMap<String, String>> = Mono
        .just(response)
        .map(ClientResponse::cookies)
        .map { responseCookies ->
            responseCookies
                .map { (key, values) -> key to values.map(ResponseCookie::getValue) }
                .fold(LinkedMultiValueMap()) { acc, (key, values) -> acc.apply { addAll(key, values) } }
        }

    companion object : LoggerCompanion(MachineBoxService::class) {
        private const val STATE_ENDPOINT = "/state"
        private const val NOOP_ENDPOINT = "/liveness"
    }
}
