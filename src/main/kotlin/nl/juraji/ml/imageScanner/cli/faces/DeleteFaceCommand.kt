package nl.juraji.ml.imageScanner.cli.faces

import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.required
import nl.juraji.ml.imageScanner.services.FaceBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import org.springframework.stereotype.Component

@Component
class DeleteFaceCommand(
    private val faceBoxService: FaceBoxService
) : Subcommand("delete-face", "Delete face from model") {
    private val id by option(
        type = ArgType.String,
        fullName = "id",
        description = "Id/filename with which the face was taught"
    ).required()

    override fun execute() {
        logger.info("Deleting face by id \"$id\"...")

        faceBoxService.delete(id)
            .doOnError { logger.error("Failed deleting face with id $id", it) }
            .doOnSuccess { logger.info("Face with id \"$id\" has been deleted") }
            .block()
    }

    companion object : LoggerCompanion(DeleteFaceCommand::class)
}