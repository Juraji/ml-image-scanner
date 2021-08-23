package nl.juraji.ml.imageScanner.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.nio.file.Path

@ConstructorBinding
@ConfigurationProperties("output")
data class OutputConfiguration(
    val dataOutputDirectory: Path,
) {
    fun resolve(filename: String): Path = dataOutputDirectory.resolve(filename)
}
