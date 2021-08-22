package nl.juraji.ml.imageScanner.cli

import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.delimiter
import nl.juraji.ml.imageScanner.configuration.FaceBoxConfiguration
import nl.juraji.ml.imageScanner.configuration.OutputConfiguration
import nl.juraji.ml.imageScanner.configuration.TagBoxConfiguration
import nl.juraji.ml.imageScanner.services.FileService
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import reactor.core.publisher.Mono
import java.nio.file.Path
import java.nio.file.Paths

enum class ConfigAction {
    SET, GET
}

@Component
class ConfigCommand(
    private val tagBoxConfiguration: TagBoxConfiguration,
    private val outputConfiguration: OutputConfiguration,
    private val faceBoxConfiguration: FaceBoxConfiguration,
    private val fileService: FileService,
) : AsyncCommand("config", "Set configuration options") {
    private val action by argument(
        ArgType.Choice<ConfigAction>(),
        "action",
        "Use \"get\" to print current configuration, use \"set\" to update configuration"
    )

    private val outputDirectory by option(
        ArgType.String,
        "output-directory",
        description = "Set the target output directory for detection data"
    ).default(outputConfiguration.dataOutputDirectory)

    private val tagBoxEndpoint by option(
        ArgType.String,
        "tag-box.endpoint",
        description = "Http endpoint for Tag Box"
    ).default(tagBoxConfiguration.endpoint)
    private val tagBoxStateFile by option(
        ArgType.String,
        "tag-box.state-file",
        description = "Target file to persist Tag Box state to"
    ).default(tagBoxConfiguration.stateFile)
    private val tagBoxBlacklist by option(
        ArgType.String,
        "tag-box.blacklist",
        description = "Set blacklist for tags (comma separated), these tags will not be proposed"
    ).delimiter(",")

    private val faceBoxEndpoint by option(
        ArgType.String,
        "face-box.endpoint",
        description = "Http endpoint for Face Box"
    ).default(faceBoxConfiguration.endpoint)
    private val faceBoxStateFile by option(
        ArgType.String,
        "face-box.state-file",
        description = "Target file to persist Face Box state to"
    ).default(faceBoxConfiguration.stateFile)

    override fun executeAsync(): Publisher<*> {
        return when (action) {
            ConfigAction.GET -> printConfiguration()
            ConfigAction.SET -> updateConfiguration()
        }
    }

    private fun printConfiguration(): Mono<Unit> {

        logger.info(
            """
            --
            Current configuration:
                output-directory    = ${outputConfiguration.dataOutputDirectory}
                tag-box.endpoint    = ${tagBoxConfiguration.endpoint}
                tag-box.state-file  = ${tagBoxConfiguration.stateFile}
                tag-box.blacklist   = ${tagBoxConfiguration.blacklist}
                face-box.endpoint   = ${faceBoxConfiguration.endpoint}
                face-box.state-file = ${faceBoxConfiguration.stateFile}
            """.trimIndent()
        )

        return Mono.empty()
    }

    private fun updateConfiguration(): Mono<Path> {
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
                    "blacklist" to tagBoxBlacklist.ifEmpty { tagBoxConfiguration.blacklist }
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