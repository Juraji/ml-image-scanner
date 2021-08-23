package nl.juraji.ml.imageScanner.cli.faces

import kotlinx.cli.default
import kotlinx.cli.required
import nl.juraji.ml.imageScanner.cli.AsyncCommand
import nl.juraji.ml.imageScanner.configuration.OutputConfiguration
import nl.juraji.ml.imageScanner.model.face.DetectFacesResult
import nl.juraji.ml.imageScanner.model.face.Face
import nl.juraji.ml.imageScanner.services.FaceBoxService
import nl.juraji.ml.imageScanner.services.FileService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.cli.intOption
import nl.juraji.ml.imageScanner.util.cli.pathOption
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.isRegularFile

@Component
class DetectFacesCommand(
    private val outputConfiguration: OutputConfiguration,
    private val fileService: FileService,
    private val faceBoxService: FaceBoxService,
) : AsyncCommand("detect-faces", "Detect faces in an image") {
    private val inputFile by pathOption(
        fullName = "input",
        shortName = "i",
        description = "Path to image file or folder with images to detect"
    ).required()

    private val outputFile by pathOption(
        fullName = "output",
        shortName = "o",
        description = "Output file path (*.json)"
    ).default(outputConfiguration.resolve(OUTPUT_FILE_NAME))

    private val checkpoints by intOption(
        fullName = "checkpoint-interval",
        description = "Create a checkpoint file every n seconds, set to 0 or lower to disable"
    ).default(30)

    override fun executeAsync(): Publisher<*> {
        logger.info("Detecting faces (recursively) in $inputFile...")

        val files =
            if (inputFile.isRegularFile()) Flux.just(inputFile)
            else this.fileService.walkDirectory(inputFile)
                .filter { it.isRegularFile() }

        val detections = files
            .parallel()
            .filter { it.isRegularFile() }
            .flatMap { p ->
                faceBoxService.detect(p)
                    .onErrorContinue { _, _ -> DetectFacesResult(0, emptyList()) }
                    .map { p to it.faces.filter(Face::matched) }
            }
            .doOnNext { (p, r) -> logger.info("Detected ${r.size} faces in \"$p\"") }
            .sequential()
            .share()

        if (checkpoints > 0) {
            sideEffectCreateCheckpointFiles(detections, checkpoints)
        }

        return detections
            .reduce<Map<Path, List<Face>>>(emptyMap()) { prev, next -> prev + next }
            .flatMap { fileService.serialize(it) }
            .flatMap { fileService.writeBytesTo(it, outputFile) }
            .doOnSuccess { logger.info("Face detection completed, check $it for the results.") }
    }

    private fun sideEffectCreateCheckpointFiles(detections: Flux<Pair<Path, List<Face>>>, checkpointInterval: Int) {
        logger.info("Creating checkpoint files every $checkpointInterval seconds!")

        val checkpointCounter = AtomicInteger()
        val nextCheckpointFilename = {
            outputConfiguration.resolve("detected-faces-checkpoint-${checkpointCounter.incrementAndGet()}.json")
        }
        detections
            .buffer(Duration.ofSeconds(checkpointInterval.toLong()))
            .doOnNext { logger.info("Checkpoint reached, saving partial...") }
            .map(List<Pair<*, *>>::toMap)
            .flatMap(fileService::serialize)
            .flatMap { bytes -> fileService.writeBytesTo(bytes, nextCheckpointFilename()) }
            .runCatching(Flux<*>::blockLast)
    }

    companion object : LoggerCompanion(DetectFacesCommand::class) {
        private const val OUTPUT_FILE_NAME = "detected-faces.json"
    }
}