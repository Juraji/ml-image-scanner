package nl.juraji.ml.imageScanner.configuration

import java.net.URI
import java.nio.file.Path

interface MachineBoxConfiguration {
    val endpoint: URI
    val stateFile: Path
}