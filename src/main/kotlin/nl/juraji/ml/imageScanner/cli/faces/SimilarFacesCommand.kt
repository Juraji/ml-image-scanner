package nl.juraji.ml.imageScanner.cli.faces

import kotlinx.cli.required
import nl.juraji.ml.imageScanner.cli.AsyncCommand
import nl.juraji.ml.imageScanner.model.face.SimilarFace
import nl.juraji.ml.imageScanner.services.FaceBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.cli.pathOption
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class SimilarFacesCommand(
    private val faceBoxService: FaceBoxService
) : AsyncCommand("similar-faces", "Find taught faces similar to the given image") {
    private val file by pathOption(
        fullName = "file",
        shortName = "f",
        description = "Path to image file"
    ).required()

    override fun executeAsync(): Publisher<*> {
        return faceBoxService.similarFaces(file)
            .map { (faces) -> faces }
            .filter { it.isNotEmpty() }
            .doOnNext { logger.info(renderOutput(it)) }
    }

    private fun renderOutput(all: List<SimilarFace>): String =
        "Found ${all.size} similar faces:\n\t" + all.joinToString(separator = "\n\t") { (rect, faces) ->
            "At (x,y): ${rect.left},${rect.top}:\n\t\t" + faces.joinToString(separator = "\n\t\t") { face ->
                "${face.name}, id: ${face.id}, confidence: ${face.confidence}"
            }
        }

    companion object : LoggerCompanion(SimilarFacesCommand::class)
}