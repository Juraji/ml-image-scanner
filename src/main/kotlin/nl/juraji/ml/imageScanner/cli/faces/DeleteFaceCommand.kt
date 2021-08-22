package nl.juraji.ml.imageScanner.cli.faces

import kotlinx.cli.required
import nl.juraji.ml.imageScanner.cli.AsyncCommand
import nl.juraji.ml.imageScanner.services.FaceBoxService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.cli.ArgTypes
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class DeleteFaceCommand(
    private val faceBoxService: FaceBoxService
) : AsyncCommand("delete-face", "Delete face from model") {
    private val id by option(
        type = ArgTypes.String,
        fullName = "id",
        description = "Id/filename with which the face was taught"
    ).required()

    override fun executeAsync(): Publisher<*> {
        logger.info("Deleting face by id \"$id\"...")

        return faceBoxService.delete(id)
            .doOnSuccess { logger.info("Face with id \"$id\" has been deleted") }
    }

    companion object : LoggerCompanion(DeleteFaceCommand::class)
}