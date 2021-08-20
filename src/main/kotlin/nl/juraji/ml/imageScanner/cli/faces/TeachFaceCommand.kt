package nl.juraji.ml.imageScanner.cli.faces

import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.required
import nl.juraji.ml.imageScanner.services.FaceBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import org.springframework.stereotype.Component
import java.nio.file.Paths

@Component
class TeachFaceCommand(
    private val faceBoxService: FaceBoxService,
) : Subcommand("teach-face", "Teach a new face") {
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

    override fun execute() {
        logger.info("Detecting and imprinting face in \"$file\" with name \"$faceName\"...")

        faceBoxService.teach(faceName, Paths.get(file))
            .doOnError { logger.error("Failed detecting faces in $file", it) }
            .doOnSuccess { logger.info("Face with name \"$faceName\" has been saved") }
            .block()
    }

    companion object : LoggerCompanion(TeachFaceCommand::class)
}