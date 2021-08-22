package nl.juraji.ml.imageScanner

import kotlinx.cli.ArgParser
import kotlinx.cli.Subcommand
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan("nl.juraji.ml.imageScanner.configuration")
class MlImageScannerApplication(
    private val commands: List<Subcommand>
) : CommandLineRunner {

    override fun run(args: Array<String>) {
        ArgParser("Image Scanner (ML)")
            .apply { subcommands(*commands.toTypedArray()) }
            .parse(args.ifEmpty { arrayOf("-h") })
    }
}

fun main(args: Array<String>) {
    runApplication<MlImageScannerApplication>(*args)
}
