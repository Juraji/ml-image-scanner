package nl.juraji.ml.imageScanner.cli

import com.fasterxml.jackson.core.type.TypeReference
import kotlinx.cli.default
import nl.juraji.ml.imageScanner.model.face.Face
import nl.juraji.ml.imageScanner.model.tag.Tag
import nl.juraji.ml.imageScanner.services.FileService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.cli.booleanOption
import nl.juraji.ml.imageScanner.util.cli.pathOption
import nl.juraji.ml.imageScanner.util.unique
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
    private val force by booleanOption(
        fullName = "force",
        description = "Force writing tags (helps with images that will not update)"
    ).default(false)
    private val append by booleanOption(
        fullName = "append",
        description = "Append new tags to exising tags/user comment"
    ).default(false)

    override fun executeAsync(): Publisher<*> {
        val tagsFileTypeReference = object : TypeReference<Map<String, List<Tag>>>() {}
        val facesFileTypeReference = object : TypeReference<Map<String, List<Face>>>() {}

        val detectedTags = readDetectionFile(tagDetectionFile, tagsFileTypeReference)
            .map { (path, tags) -> Paths.get(path) to tags.map(Tag::tag) }

        val detectedFaces = readDetectionFile(faceDetectionFile, facesFileTypeReference)
            .map { (path, faces) -> path to faces.filter { it.matched } }
            .map { (path, faces) -> Paths.get(path) to faces.map(Face::name) }
            .filter { (_, faces) -> faces.isNotEmpty() }

        val tagAccumulator: (Map<Path, List<String>>, Pair<Path, List<String>>) -> Map<Path, List<String>> =
            { acc, (path, tags) -> acc + (path to ((acc[path] ?: emptyList()) + tags)) }

        val appendMap: (Map.Entry<Path, List<String>>) -> Mono<Pair<Path, List<String>>> =
            if (append) { (path, tags) ->
                fileService
                    .readExifUserComment(path)
                    .map { it.split(',') }
                    .map { path to (it + tags).unique() }
                    .defaultIfEmpty(path to tags)
            }
            else { (path, tags) -> Mono.just(path to tags) }

        logger.info("Setting tags as exif user comments, with force: $force and append: $append")

        return Flux.concat(detectedFaces, detectedTags)
            .reduce(emptyMap(), tagAccumulator)
            .flatMapMany { Flux.fromIterable(it.entries) }
            .flatMap(appendMap)
            .map { (path, tags) -> path to tags.joinToString(",") }
            .doOnNext { (p, t) -> logger.info("Tags for $p: $t") }
            .flatMap { (path, tags) -> fileService.setExifUserComment(path, tags, force) }
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