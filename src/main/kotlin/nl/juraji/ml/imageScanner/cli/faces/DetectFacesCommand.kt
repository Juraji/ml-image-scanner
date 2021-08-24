package nl.juraji.ml.imageScanner.cli.faces

import nl.juraji.ml.imageScanner.cli.AbstractDetectionCommand
import nl.juraji.ml.imageScanner.configuration.OutputConfiguration
import nl.juraji.ml.imageScanner.model.face.DetectFacesResult
import nl.juraji.ml.imageScanner.model.face.Face
import nl.juraji.ml.imageScanner.services.FaceBoxService
import nl.juraji.ml.imageScanner.services.FileService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.nio.file.Path

@Component
class DetectFacesCommand(
    private val faceBoxService: FaceBoxService,
    outputConfiguration: OutputConfiguration,
    fileService: FileService,
) : AbstractDetectionCommand<Face>(
    "detect-faces",
    "Detect faces in an image",
    outputConfiguration,
    fileService
) {
    override fun logOnBoot(inputFile: Path): String = "Start detecting faces (recursively) in $inputFile..."
    override fun logOnDetected(path: Path, items: List<Face>): String = "Detected ${items.size} in $path"
    override fun logOnComplete(outputFile: Path): String =
        "Face detection completed, check $outputFile for the results."

    override fun getItemsForPath(path: Path): Mono<List<Face>> =
        faceBoxService.detect(path)
            .onErrorContinue { _, _ -> DetectFacesResult(0, emptyList()) }
            .map { it.faces.filter(Face::matched) }
}