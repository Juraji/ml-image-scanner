package nl.juraji.ml.imageScanner.services

import nl.juraji.ml.imageScanner.configuration.FaceBoxConfiguration
import nl.juraji.ml.imageScanner.model.face.DetectFacesResult
import nl.juraji.ml.imageScanner.model.face.Face
import nl.juraji.ml.imageScanner.util.filePart
import nl.juraji.ml.imageScanner.util.multiPartBody
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.nio.file.Path
import java.time.Duration

@Service
class FaceBoxService(
    @Qualifier("faceBoxWebClient") private val faceBoxWebClient: WebClient,
    fileService: FileService,
    faceBoxConfiguration: FaceBoxConfiguration,
) : MachineBoxService(fileService, faceBoxWebClient, faceBoxConfiguration) {

    fun detect(file: Path): Mono<DetectFacesResult> = withReadStateManagement {
        faceBoxWebClient
            .post()
            .uri("/check")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .cookies(::applyStateCookies)
            .multiPartBody {
                filePart("file", file)
            }
            .retrieve()
            .bodyToMono<DetectFacesResult>()
    }
        .retryWhen(Retry.backoff(6, Duration.ofSeconds(10)))

    fun teach(name: String, file: Path): Mono<Face> = withStateManagement {
        val id = file.fileName.toString()
        val faceResult = Face(
            id = id,
            name = name,
            matched = true,
            confidence = 1.0,
        )

        faceBoxWebClient
            .post()
            .uri("/teach")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .cookies(::applyStateCookies)
            .multiPartBody {
                filePart("file", file, id)
                part("name", name)
                part("id", id)
            }
            .retrieve()
            .bodyToMono<Unit>()
            .thenReturn(faceResult)
    }

    fun delete(id: String): Mono<Unit> = withStateManagement {
        faceBoxWebClient
            .delete()
            .uri("/teach/{id}", mapOf("id" to id))
            .cookies(::applyStateCookies)
            .retrieve()
            .bodyToMono()
    }
}