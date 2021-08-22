package nl.juraji.ml.imageScanner.cli.tags

import kotlinx.cli.required
import nl.juraji.ml.imageScanner.cli.AsyncCommand
import nl.juraji.ml.imageScanner.services.TagBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.cli.ArgTypes
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class DeleteTagCommand(
    private val tagBoxService: TagBoxService
) : AsyncCommand("delete-tag", "Delete an existing tag") {
    private val tagId by option(
        ArgTypes.String,
        fullName = "id",
        description = "Id with which the tag was created"
    ).required()

    override fun executeAsync(): Publisher<*> {
        logger.info("Deleting tag with id $tagId...")

        return tagBoxService.delete(tagId)
            .doOnSuccess { logger.info("Tag with id $tagId has been deleted") }
    }

    companion object : LoggerCompanion(DetectTagsCommand::class)
}