package nl.juraji.ml.imageScanner.cli.faces

import kotlinx.cli.ArgType
import kotlinx.cli.required
import nl.juraji.ml.imageScanner.cli.AsyncCommand
import nl.juraji.ml.imageScanner.services.FaceBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component
import java.nio.file.Paths

@Component
class TeachFaceCommand(
    private val faceBoxService: FaceBoxService,
) : AsyncCommand("teach-face", "Teach a new face") {
    private val faceName by option(
        type = ArgType.String,
        fullName = "name",
        shortName = "n",
        description = "Name of face"
    ).required()

    private val file by option(
        type = ArgType.String,
        fullName = "file",
        shortName = "f",
        description = "Path to image file of face"
    ).required()

    override fun executeAsync(): Publisher<*> {
        logger.info("Detecting and imprinting face in \"$file\" with name \"$faceName\"...")

        return faceBoxService.teach(faceName, Paths.get(file))
            .doOnSuccess { logger.info("Face with name \"$faceName\" has been saved") }
    }

    companion object : LoggerCompanion(TeachFaceCommand::class)
}