package nl.juraji.ml.imageScanner.cli.tags

import kotlinx.cli.required
import nl.juraji.ml.imageScanner.cli.AsyncCommand
import nl.juraji.ml.imageScanner.model.tag.Tag
import nl.juraji.ml.imageScanner.services.TagBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.cli.pathOption
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class SimilarTagsCommand(
    private val tagBoxService: TagBoxService
) : AsyncCommand("similar-tags", "Find taught tag images that are similar to the given file") {
    private val file by pathOption(
        fullName = "file",
        shortName = "f",
        description = "Path to image file"
    ).required()

    override fun executeAsync(): Publisher<*> {
        return tagBoxService.similarImages(file)
            .doOnNext { (tagsCount, tags) ->
                logger.info(renderOutput(tagsCount, tags))
            }
    }

    private fun renderOutput(tagsCount: Int, tags: List<Tag>): String =
        "Found $tagsCount similar tags:\n\t" + tags.joinToString("\n\t")

    companion object : LoggerCompanion(SimilarTagsCommand::class)
}