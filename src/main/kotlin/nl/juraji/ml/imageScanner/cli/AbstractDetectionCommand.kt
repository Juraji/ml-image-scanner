package nl.juraji.ml.imageScanner.cli

import kotlinx.cli.default
import kotlinx.cli.required
import nl.juraji.ml.imageScanner.cli.tags.DetectTagsCommand
import nl.juraji.ml.imageScanner.configuration.OutputConfiguration
import nl.juraji.ml.imageScanner.services.FileService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.cli.intOption
import nl.juraji.ml.imageScanner.util.cli.pathOption
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.isRegularFile

abstract class AbstractDetectionCommand<T : Any>(
    name: String,
    actionDescription: String,
    outputConfiguration: OutputConfiguration,
    private val fileService: FileService
) : AsyncCommand(name, actionDescription) {
    private val inputFile by pathOption(
        fullName = "input",
        shortName = "i",
        description = "Path to image file or folder with images to detect"
    ).required()

    private val outputFile by pathOption(
        fullName = "output",
        shortName = "o",
        description = "Output file path (*.json)"
    ).default(outputConfiguration.resolve("detected-faces.json"))

    private val checkpointInterval by intOption(
        fullName = "checkpoint-interval",
        description = "Create a checkpoint file every n seconds, set to 0 or lower to disable"
    ).default(30)

    protected abstract fun logOnBoot(inputFile: Path): String
    protected abstract fun logOnDetected(path: Path, items: List<T>): String
    protected abstract fun logOnComplete(outputFile: Path): String
    protected abstract fun getItemsForPath(path: Path): Mono<List<T>>

    override fun executeAsync(): Publisher<*> {
        logger.info(logOnBoot(inputFile))

        val detections = Mono.just(inputFile)
            .flatMapMany { root ->
                if (root.isRegularFile()) Flux.just(root)
                else this.fileService.walkDirectory(root)
                    .filter { it.isRegularFile() }
            }
            .parallel()
            .flatMap { p ->
                getItemsForPath(p)
                    .map { p to it }
                    .onErrorResume { t ->
                        logger.error("Could not run detection on $p: ${t.message}")
                        Mono.empty<Pair<Path, List<T>>>()
                    }
            }
            .doOnNext { (p, items) -> logger.info(logOnDetected(p, items)) }
            .sequential()
            .share()

        if (checkpointInterval > 0)
            sideEffectCreateCheckpointFiles(detections)

        return detections
            .reduce<Map<Path, List<T>>>(emptyMap()) { prev, next -> prev + next }
            .flatMap(fileService::serialize)
            .flatMap { fileService.writeBytesTo(it, outputFile) }
            .doOnSuccess { logger.info(logOnComplete(outputFile)) }
    }

    private fun sideEffectCreateCheckpointFiles(detections: Flux<Pair<Path, List<T>>>) {
        logger.info("Creating a checkpoint file every $checkpointInterval seconds!")

        val checkpointCounter = AtomicInteger()
        detections
            .buffer(Duration.ofSeconds(checkpointInterval.toLong()))
            .doOnNext { logger.info("Checkpoint reached, saving partial") }
            .map(List<Pair<*, *>>::toMap)
            .flatMap(fileService::serialize)
            .flatMap { bytes ->
                val index = checkpointCounter.incrementAndGet()
                fileService.writeBytesTo(bytes, buildCheckpointFilePath(index))
            }
            .runCatching(Flux<*>::blockLast)
    }

    private fun buildCheckpointFilePath(index: Int): Path =
        outputFile.parent.resolve("$name-checkpoint.$index.json")

    companion object : LoggerCompanion(DetectTagsCommand::class) {

    }
}