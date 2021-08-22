package nl.juraji.ml.imageScanner.cli.tags

import kotlinx.cli.required
import nl.juraji.ml.imageScanner.cli.AsyncCommand
import nl.juraji.ml.imageScanner.services.TagBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.cli.pathOption
import nl.juraji.ml.imageScanner.util.cli.stringOption
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class TeachTagCommand(
    private val tagBoxService: TagBoxService
) : AsyncCommand("teach-tag", "Teach a new tag") {
    private val tagName by stringOption(
        fullName = "name",
        shortName = "n",
        description = "Name of tag"
    ).required()

    private val file by pathOption(
        fullName = "file",
        shortName = "f",
        description = "Path to example image file of tag"
    ).required()

    override fun executeAsync(): Publisher<*> {
        logger.info("Detecting and imprinting tag on \"$file\" with name \"$tagName\"...")

        return tagBoxService.teach(file, tagName)
            .doOnSuccess { logger.info("Tag with id \"${it.id}\" has been saved") }
    }

    companion object : LoggerCompanion(TeachTagCommand::class)
}