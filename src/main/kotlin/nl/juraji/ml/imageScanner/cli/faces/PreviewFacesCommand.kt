package nl.juraji.ml.imageScanner.cli.faces

import com.fasterxml.jackson.core.type.TypeReference
import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.required
import nl.juraji.ml.imageScanner.cli.ApplyTagsCommand
import nl.juraji.ml.imageScanner.configuration.OutputConfiguration
import nl.juraji.ml.imageScanner.model.face.Face
import nl.juraji.ml.imageScanner.services.FileService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.blockLastAndCatch
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.io.path.extension

@Component
class PreviewFacesCommand(
    private val fileService: FileService,
    private val outputConfiguration: OutputConfiguration,
) : Subcommand("preview-faces", "Preview face detections using the actual images") {
    private val faceDetectionFile by option(
        ArgType.String,
        fullName = "faces-file",
        shortName = "f",
        description = "Detection result file path for faces"
    ).required()

    override fun execute() {
        val outputDir = Paths.get(outputConfiguration.dataOutputDirectory).resolve("face-previews")
        val faces = readDetectionFile(faceDetectionFile)

        faces
            .parallel()
            .map { (path, faces) -> outputDir.resolve(path.fileName) to drawFaceBoxes(path, faces) }
            .flatMap { (path, imgBytes) -> fileService.writeBytesTo(imgBytes, path) }
            .doOnNext { logger.info("Created preview file $it") }
            .blockLastAndCatch()
            .onSuccess { logger.info("Created preview files in $outputDir") }
            .onFailure { logger.error("Error creating preview files", it) }
    }

    private fun drawFaceBoxes(path: Path, faces: List<Face>): ByteArray {
        val sourceImage = ImageIO.read(path.toFile())

        val graphics = sourceImage.createGraphics().apply {
            color = GRAPHICS_COLOR
            stroke = BasicStroke(BOX_BORDER_WIDTH.toFloat())
            font = font.deriveFont(Font.BOLD, FONT_SIZE)
        }

        graphics.drawImage(sourceImage, 0, 0, null)

        faces.forEachIndexed { index, face ->
            val (y, x, width, height) = face.rect

            logger.info("Marking face of \"${face.name}\" (${index + 1}/${faces.size}) in ${path.fileName}")

            graphics.drawRect(x, y, width, height)
            graphics.drawString(
                face.name,
                x + BOX_BORDER_WIDTH,
                y + height + BOX_BORDER_WIDTH + graphics.font.size
            )
        }

        graphics.dispose()

        return ByteArrayOutputStream().use { os ->
            ImageIO.write(sourceImage, path.extension, os)
            os.toByteArray()
        }
    }

    private fun readDetectionFile(
        filePath: String
    ): Flux<Pair<Path, List<Face>>> {
        val facesFileTypeReference = object : TypeReference<Map<String, List<Face>>>() {}
        return Mono
            .just(filePath).map(Paths::get)
            .filterWhen(fileService::fileExists)
            .flatMap(fileService::readBytes)
            .flatMap { fileService.deserialize(it, facesFileTypeReference) }
            .flatMapMany { Flux.fromIterable(it.entries) }
            .map { (path, list) -> Paths.get(path) to list.filter { it.matched } }
            .filter { (_, list) -> list.isNotEmpty() }
    }

    companion object : LoggerCompanion(ApplyTagsCommand::class) {
        private const val BOX_BORDER_WIDTH = 4
        private const val FONT_SIZE = 20f
        private val GRAPHICS_COLOR = Color.RED
    }
}