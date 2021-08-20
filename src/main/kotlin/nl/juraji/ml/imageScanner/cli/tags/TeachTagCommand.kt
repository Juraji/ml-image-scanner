package nl.juraji.ml.imageScanner.cli.tags

import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.required
import nl.juraji.ml.imageScanner.services.TagBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import org.springframework.stereotype.Component
import java.nio.file.Paths

@Component
class TeachTagCommand(
    private val tagBoxService: TagBoxService
): Subcommand("teach-tag", "Teach a new tag") {
    private val tagName by option(
        type = ArgType.String,
        fullName = "name",
        shortName = "n",
        description = "Name of tag"
    ).required()

    private val file by option(
        type = ArgType.String,
        fullName = "file",
        shortName = "f",
        description = "Path to example image file of tag"
    ).required()

    override fun execute() {
        logger.info("Detecting and imprinting tag on \"$file\" with name \"$tagName\"...")

        tagBoxService.teach(Paths.get(file), tagName)
            .doOnError { logger.error("Failed teaching tag for file $file", it) }
            .doOnSuccess { logger.info("Tag with id \"${it.id}\" has been saved") }
            .block()
    }

    companion object : LoggerCompanion(TeachTagCommand::class)
}