package nl.juraji.ml.imageScanner.cli.faces

import kotlinx.cli.ArgType
import kotlinx.cli.required
import nl.juraji.ml.imageScanner.cli.AsyncCommand
import nl.juraji.ml.imageScanner.configuration.OutputConfiguration
import nl.juraji.ml.imageScanner.model.face.DetectFacesResult
import nl.juraji.ml.imageScanner.model.face.Face
import nl.juraji.ml.imageScanner.services.FaceBoxService
import nl.juraji.ml.imageScanner.services.FileService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile

@Component
class DetectFacesCommand(
    private val outputConfiguration: OutputConfiguration,
    private val fileService: FileService,
    private val faceBoxService: FaceBoxService,
) : AsyncCommand("detect-faces", "Detect faces in an image") {
    private val file by option(
        type = ArgType.String,
        fullName = "file",
        shortName = "f",
        description = "Path to image file or folder with images to detect"
    ).required()

    override fun executeAsync(): Publisher<*> {
        val outputPath = Paths
            .get(outputConfiguration.dataOutputDirectory)
            .resolve("detected-faces.json")

        val path = Paths.get(file)

        logger.info("Detecting faces recursively in $path file(s)...")

        return this.fileService.walkDirectory(path)
            .parallel()
            .filter { it.isRegularFile() }
            .flatMap { p ->
                faceBoxService.detect(p)
                    .onErrorContinue { _, _ -> DetectFacesResult(0, emptyList()) }
                    .map { p to it.faces.filter(Face::matched) }
            }
            .doOnNext { (p, r) -> logger.info("Detected ${r.size} faces in \"$p\"") }
            .sequential()
            .reduce<Map<Path, List<Face>>>(emptyMap()) { prev, next -> prev + next }
            .flatMap { fileService.serialize(it) }
            .flatMap { fileService.writeBytesTo(it, outputPath) }
            .doOnSuccess { logger.info("Face detection completed, check $it for the results.") }
    }

    companion object : LoggerCompanion(DetectFacesCommand::class)
}