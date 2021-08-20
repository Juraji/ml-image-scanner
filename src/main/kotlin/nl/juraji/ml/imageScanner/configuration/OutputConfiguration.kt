package nl.juraji.ml.imageScanner.configuration

import nl.juraji.ml.imageScanner.services.FileService
import nl.juraji.ml.imageScanner.util.blockAndCatch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.nio.file.Paths
import javax.annotation.PostConstruct

@ConstructorBinding
@ConfigurationProperties("output")
data class OutputConfiguration(
    val dataOutputDirectory: String,
) {
    @Autowired
    private lateinit var fileService: FileService

    @PostConstruct
    fun init() {
        fileService.createDirectories(Paths.get(dataOutputDirectory)).blockAndCatch()
    }
}
