package nl.juraji.ml.imageScanner.cli

import kotlinx.cli.required
import nl.juraji.ml.imageScanner.services.FileService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.cli.pathOption
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class ReadTagsCommand(
    private val fileService: FileService
) : AsyncCommand("read-tags", "Read tags from a file, applied by apply-tags") {
    val file by pathOption(
        fullName = "file",
        shortName = "f",
        description = "The file to read the tags from"
    ).required()

    override fun executeAsync(): Publisher<*> {
        logger.info("Reading tags/user comment from $file")
        return fileService.readExifUserComment(file)
            .defaultIfEmpty("NO META DATA")
            .doOnNext { logger.info("Found tag data: $it") }
    }

    companion object : LoggerCompanion(ReadTagsCommand::class)
}