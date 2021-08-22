package nl.juraji.ml.imageScanner.cli.tags

import kotlinx.cli.ArgType
import kotlinx.cli.required
import nl.juraji.ml.imageScanner.cli.AsyncCommand
import nl.juraji.ml.imageScanner.services.TagBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class RenameTagCommand(
    private val tagBoxService: TagBoxService
) : AsyncCommand("rename-tag", "Rename an existing tag") {
    private val tagId by option(
        ArgType.String,
        fullName = "id",
        description = "Id with which the tag was created"
    ).required()
    private val newName by option(
        ArgType.String,
        fullName = "new-name",
        description = "New name for tag"
    ).required()

    override fun executeAsync(): Publisher<*> {
        logger.info("Renaming tag with id $tagId to $newName")

        return tagBoxService.rename(tagId, newName)
            .doOnSuccess { logger.info("Tag with id $tagId has been renamed to $newName") }
    }

    companion object : LoggerCompanion(RenameTagCommand::class)
}