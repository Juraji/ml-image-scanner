package nl.juraji.ml.imageScanner.cli.tags

import kotlinx.cli.required
import nl.juraji.ml.imageScanner.cli.AsyncCommand
import nl.juraji.ml.imageScanner.services.TagBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.cli.pathOption
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class SimilarByTagsCommand(
    private val tagBoxService: TagBoxService
) : AsyncCommand("similar-by-tags", "Find taught tag images that are similar to the given file") {
    private val file by pathOption(
        fullName = "file",
        shortName = "f",
        description = "Path to image file"
    ).required()

    override fun executeAsync(): Publisher<*> {
        return tagBoxService.similarImages(file)
            .doOnNext { (tagsCount, similar) ->
                logger.info("Found $tagsCount similar tags:\n\t" + similar.joinToString("\n\t"))
            }
    }

    companion object : LoggerCompanion(SimilarByTagsCommand::class)
}