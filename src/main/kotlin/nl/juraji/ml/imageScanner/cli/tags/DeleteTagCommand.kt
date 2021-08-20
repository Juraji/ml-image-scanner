package nl.juraji.ml.imageScanner.cli.tags

import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.required
import nl.juraji.ml.imageScanner.services.TagBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import org.springframework.stereotype.Component

@Component
class DeleteTagCommand(
    private val tagBoxService: TagBoxService
) : Subcommand("delete-tag", "Delete an existing tag") {
    private val tagId by option(
        ArgType.String,
        fullName = "id",
        description = "Id with which the tag was created"
    ).required()

    override fun execute() {
        logger.info("Deleting tag with id $tagId...")

        tagBoxService.delete(tagId)
            .doOnError { logger.error("Failed deleting tag $tagId", it) }
            .doOnSuccess { logger.info("Tag with id $tagId has been deleted") }
            .block()
    }

    companion object : LoggerCompanion(DetectTagsCommand::class)
}