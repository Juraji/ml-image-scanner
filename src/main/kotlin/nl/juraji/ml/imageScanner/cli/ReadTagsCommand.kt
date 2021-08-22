package nl.juraji.ml.imageScanner.cli

import kotlinx.cli.ArgType
import nl.juraji.ml.imageScanner.services.FileService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.nio.file.Paths

@Component
class ReadTagsCommand(
    private val fileService: FileService
) : AsyncCommand("read-tags", "Read tags from a file, applied by apply-tags") {
    val filePath by option(
        ArgType.String,
        fullName = "file",
        shortName = "f",
        description = "The file to read the tags from"
    )

    override fun executeAsync(): Publisher<*> {
        return Mono.justOrEmpty(filePath)
            .map(Paths::get)
            .map(fileService::readExifUserComment)
            .doOnNext { logger.info("Found tag data: $it") }
    }

    companion object : LoggerCompanion(ReadTagsCommand::class)
}