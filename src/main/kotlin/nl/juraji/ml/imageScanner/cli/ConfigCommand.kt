package nl.juraji.ml.imageScanner.cli

import kotlinx.cli.default
import kotlinx.cli.delimiter
import nl.juraji.ml.imageScanner.configuration.FaceBoxConfiguration
import nl.juraji.ml.imageScanner.configuration.OutputConfiguration
import nl.juraji.ml.imageScanner.configuration.TagBoxConfiguration
import nl.juraji.ml.imageScanner.services.FileService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.cli.ArgTypes
import nl.juraji.ml.imageScanner.util.cli.defaultOrEmpty
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import reactor.core.publisher.Mono
import java.nio.file.Paths

@Component
class ConfigCommand(
    private val fileService: FileService,
    tagBoxConfiguration: TagBoxConfiguration,
    outputConfiguration: OutputConfiguration,
    faceBoxConfiguration: FaceBoxConfiguration,
) : AsyncCommand("config", "Set configuration options") {
    private val outputDirectory by option(
        ArgTypes.Path,
        fullName = "output-directory",
        description = "Set the target output directory for detection data"
    ).default(outputConfiguration.dataOutputDirectory)

    private val tagBoxEndpoint by option(
        ArgTypes.URI,
        fullName = "tag-box.endpoint",
        description = "Http endpoint for Tag Box"
    ).default(tagBoxConfiguration.endpoint)
    private val tagBoxStateFile by option(
        ArgTypes.Path,
        fullName = "tag-box.state-file",
        description = "Target file to persist Tag Box state to"
    ).default(tagBoxConfiguration.stateFile)
    private val tagBoxBlacklist by option(
        ArgTypes.String,
        fullName = "tag-box.blacklist",
        description = "Set blacklist for tags (comma separated), these tags will not be proposed"
    ).delimiter(",").defaultOrEmpty(tagBoxConfiguration.blacklist)

    private val faceBoxEndpoint by option(
        ArgTypes.URI,
        fullName = "face-box.endpoint",
        description = "Http endpoint for Face Box"
    ).default(faceBoxConfiguration.endpoint)
    private val faceBoxStateFile by option(
        ArgTypes.Path,
        fullName = "face-box.state-file",
        description = "Target file to persist Face Box state to"
    ).default(faceBoxConfiguration.stateFile)

    override fun executeAsync(): Publisher<*> {
        val yamlOpts = DumperOptions().apply {
            isPrettyFlow = true
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        }

        val configMap = mapOf(
            "output" to mapOf(
                "data-output-directory" to outputDirectory
            ),
            "machine-box" to mapOf(
                "tag-box" to mapOf(
                    "endpoint" to tagBoxEndpoint,
                    "state-file" to tagBoxStateFile,
                    "blacklist" to tagBoxBlacklist
                ),
                "face-box" to mapOf(
                    "endpoint" to faceBoxEndpoint,
                    "state-file" to faceBoxStateFile
                )
            )
        )

        return Mono.just(configMap)
            .map { Yaml(yamlOpts).dump(it) }
            .flatMap { fileService.writeBytesTo(it.toByteArray(), CONFIG_FILE_PATH) }
            .doOnSuccess { logger.info("Successfully written configuration to $CONFIG_FILE_PATH") }
    }

    companion object : LoggerCompanion(ConfigCommand::class) {
        private val CONFIG_FILE_PATH = Paths.get("./application.yaml")
    }
}