package nl.juraji.ml.imageScanner.cli.tags

import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.required
import nl.juraji.ml.imageScanner.services.TagBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import org.springframework.stereotype.Component

@Component
class RenameTagCommand(
    private val tagBoxService: TagBoxService
) : Subcommand("rename-tag", "Rename an existing tag") {
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

    override fun execute() {
        logger.info("Renaming tag with id $tagId to $newName")

        tagBoxService.rename(tagId, newName)
            .doOnError { logger.error("Failed renaming tag $tagId to $newName", it) }
            .doOnSuccess { logger.info("Tag with id $tagId has been renamed to $newName") }
            .block()
    }

    companion object : LoggerCompanion(RenameTagCommand::class)
}