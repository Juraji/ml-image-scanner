package nl.juraji.ml.imageScanner.services

import nl.juraji.ml.imageScanner.configuration.TagBoxConfiguration
import nl.juraji.ml.imageScanner.model.tag.RenameTagRequest
import nl.juraji.ml.imageScanner.model.tag.SimilarImagesResult
import nl.juraji.ml.imageScanner.model.tag.Tag
import nl.juraji.ml.imageScanner.model.tag.TagResult
import nl.juraji.ml.imageScanner.util.filePart
import nl.juraji.ml.imageScanner.util.multiPartBody
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.nio.file.Path

@Service
class TagBoxService(
    @Qualifier("tagBoxWebClient") tagBoxWebClient: WebClient,
    fileService: FileService,
    tagBoxConfiguration: TagBoxConfiguration
) : MachineBoxService(fileService, tagBoxWebClient, tagBoxConfiguration) {

    fun detect(file: Path): Mono<TagResult> = withManagedStateClient {
        post()
            .uri("/check")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .multiPartBody {
                filePart("file", file)
            }
            .retrieve()
            .bodyToMono()
    }

    fun teach(file: Path, tag: String): Mono<Tag> = withManagedStatePersistingClient {
        val id = file.fileName.toString()

        post()
            .uri("/teach")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .multiPartBody {
                filePart("file", file, id)
                part("tag", tag)
                part("id", id)
            }
            .retrieve()
            .bodyToMono<Unit>()
            .thenReturn(Tag(id, tag, 1.0))
    }

    fun similarImages(file: Path): Mono<SimilarImagesResult> = withManagedStateClient {
        post()
            .uri("/similar")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .multiPartBody {
                filePart("file", file)
            }
            .retrieve()
            .bodyToMono()
    }

    fun rename(tagId: String, newName: String): Mono<Unit> = withManagedStatePersistingClient {
        patch()
            .uri("/rename")
            .bodyValue(RenameTagRequest(newName))
            .retrieve()
            .bodyToMono()
    }

    fun delete(tagId: String): Mono<Unit> = withManagedStatePersistingClient {
        delete()
            .uri("/rename/{id}", mapOf("id" to tagId))
            .retrieve()
            .bodyToMono()
    }
}