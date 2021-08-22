package nl.juraji.ml.imageScanner.cli.faces

import kotlinx.cli.required
import nl.juraji.ml.imageScanner.cli.AsyncCommand
import nl.juraji.ml.imageScanner.services.FaceBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.cli.pathOption
import nl.juraji.ml.imageScanner.util.cli.stringOption
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class TeachFaceCommand(
    private val faceBoxService: FaceBoxService,
) : AsyncCommand("teach-face", "Teach a new face") {
    private val faceName by stringOption(
        fullName = "name",
        shortName = "n",
        description = "Name of face"
    ).required()

    private val file by pathOption(
        fullName = "file",
        shortName = "f",
        description = "Path to image file of face"
    ).required()

    override fun executeAsync(): Publisher<*> {
        logger.info("Detecting and imprinting face in \"$file\" with name \"$faceName\"...")

        return faceBoxService.teach(faceName, file)
            .doOnSuccess { logger.info("Face with name \"$faceName\" has been saved") }
    }

    companion object : LoggerCompanion(TeachFaceCommand::class)
}