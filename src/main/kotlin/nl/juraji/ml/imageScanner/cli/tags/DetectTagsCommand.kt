package nl.juraji.ml.imageScanner.cli.tags

import nl.juraji.ml.imageScanner.cli.AbstractDetectionCommand
import nl.juraji.ml.imageScanner.configuration.OutputConfiguration
import nl.juraji.ml.imageScanner.configuration.TagBoxConfiguration
import nl.juraji.ml.imageScanner.model.tag.Tag
import nl.juraji.ml.imageScanner.model.tag.TagResult
import nl.juraji.ml.imageScanner.services.FileService
import nl.juraji.ml.imageScanner.services.TagBoxService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.nio.file.Path

@Component
class DetectTagsCommand(
    private val tagBoxConfiguration: TagBoxConfiguration,
    private val tagBoxService: TagBoxService,
    outputConfiguration: OutputConfiguration,
    fileService: FileService,
) : AbstractDetectionCommand<Tag>(
    "detect-tags",
    "Detect tags in an image",
    outputConfiguration,
    fileService
) {
    override fun logOnBoot(inputFile: Path): String = "Start detecting tags (recursively) in $inputFile..."
    override fun logOnDetected(path: Path, items: List<Tag>): String = "Detected ${items.size} in $path"
    override fun logOnComplete(outputFile: Path): String = "Tag detection completed, check $outputFile for the results."

    override fun getItemsForPath(path: Path): Mono<List<Tag>> =
        tagBoxService.detect(path)
            .onErrorContinue { _, _ -> TagResult(emptyList(), emptyList()) }
            .map { it.tags + it.customTags }
            .map { it.filter { (_, tag) -> !tagBoxConfiguration.blacklist.contains(tag) } }
}