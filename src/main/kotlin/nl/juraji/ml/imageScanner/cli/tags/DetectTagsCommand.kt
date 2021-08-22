package nl.juraji.ml.imageScanner.cli.tags

import kotlinx.cli.required
import nl.juraji.ml.imageScanner.cli.AsyncCommand
import nl.juraji.ml.imageScanner.configuration.OutputConfiguration
import nl.juraji.ml.imageScanner.configuration.TagBoxConfiguration
import nl.juraji.ml.imageScanner.model.tag.Tag
import nl.juraji.ml.imageScanner.model.tag.TagResult
import nl.juraji.ml.imageScanner.services.FileService
import nl.juraji.ml.imageScanner.services.TagBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.cli.pathOption
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.isRegularFile

@Component
class DetectTagsCommand(
    private val outputConfiguration: OutputConfiguration,
    private val tagBoxConfiguration: TagBoxConfiguration,
    private val tagBoxService: TagBoxService,
    private val fileService: FileService,
) : AsyncCommand("detect-tags", "Detect tags in an image") {
    private val file by pathOption(
        fullName = "file",
        shortName = "f",
        description = "Path to image file or folder with images to detect"
    ).required()

    override fun executeAsync(): Publisher<*> {
        val outputPath = outputConfiguration.dataOutputDirectory.resolve("detected-tags.json")

        logger.info("Detecting tags in $file...")

        return this.fileService.walkDirectory(file)
            .filter { it.isRegularFile() }
            .parallel()
            .doOnNext { logger.info("Detecting tags in \"$it\"...") }
            .flatMap { p ->
                tagBoxService.detect(p)
                    .onErrorContinue { _, _ -> TagResult(emptyList(), emptyList()) }
                    .map { it.tags + it.customTags }
                    .map { p to it.filter { (_, tag) -> !tagBoxConfiguration.blacklist.contains(tag) } }
            }
            .doOnNext { (p, tags) -> logger.info("Detected ${tags.size} tags in \"$p\"") }
            .sequential()
            .reduce<Map<Path, List<Tag>>>(emptyMap()) { prev, next -> prev + next }
            .flatMap { fileService.serialize(it) }
            .flatMap { fileService.writeBytesTo(it, outputPath) }
            .doOnSuccess { logger.info("Tag detection completed, check $it for the results.") }
    }

    companion object : LoggerCompanion(DetectTagsCommand::class)
}