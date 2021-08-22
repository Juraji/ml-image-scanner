package nl.juraji.ml.imageScanner.cli

import com.fasterxml.jackson.core.type.TypeReference
import nl.juraji.ml.imageScanner.model.face.Face
import nl.juraji.ml.imageScanner.model.tag.Tag
import nl.juraji.ml.imageScanner.services.FileService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.cli.pathOption
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.file.Path
import java.nio.file.Paths

@Component
class ApplyTagsCommand(
    private val fileService: FileService
) : AsyncCommand("apply", "Apply tags and faces from detection results") {
    private val tagDetectionFile by pathOption(
        fullName = "tags",
        shortName = "t",
        description = "Detection result file path for tags"
    )
    private val faceDetectionFile by pathOption(
        fullName = "faces",
        shortName = "f",
        description = "Detection result file path for faces"
    )

    override fun executeAsync(): Publisher<*> {
        val tagsFileTypeReference = object : TypeReference<Map<String, List<Tag>>>() {}
        val facesFileTypeReference = object : TypeReference<Map<String, List<Face>>>() {}

        val tags = readDetectionFile(tagDetectionFile, tagsFileTypeReference)
            .map { (path, tags) -> Paths.get(path) to tags.map(Tag::tag) }

        val faces = readDetectionFile(faceDetectionFile, facesFileTypeReference)
            .map { (path, faces) -> path to faces.filter { it.matched } }
            .map { (path, faces) -> Paths.get(path) to faces.map(Face::name) }
            .filter { (_, faces) -> faces.isNotEmpty() }

        return Flux.concat(tags, faces)
            .reduce<Map<Path, List<String>>>(emptyMap()) { prev, (path, list) ->
                val mergedList = (prev[path] ?: emptyList()) + list
                prev + (path to mergedList)
            }
            .flatMapMany { Flux.fromIterable(it.entries) }
            .map { (path, tList) -> path to tList.joinToString(",") }
            .doOnNext { (p, t) -> logger.info("Tags for $p: $t") }
            .flatMap { (path, tags) -> fileService.setExifUserComment(path, tags) }
            .doOnComplete { logger.info("Tags merged and applied!") }
    }

    private fun <K : Any, V : Any> readDetectionFile(
        filePath: Path?,
        typeReference: TypeReference<Map<K, List<V>>>
    ): Flux<Map.Entry<K, List<V>>> = Mono
        .justOrEmpty(filePath)
        .filterWhen(fileService::fileExists)
        .flatMap(fileService::readBytes)
        .flatMap { fileService.deserialize(it, typeReference) }
        .flatMapMany { Flux.fromIterable(it.entries) }
        .filter { (_, list) -> list.isNotEmpty() }

    companion object : LoggerCompanion(ApplyTagsCommand::class)
}