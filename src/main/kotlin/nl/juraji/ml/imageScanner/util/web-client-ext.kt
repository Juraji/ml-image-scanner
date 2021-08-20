package nl.juraji.ml.imageScanner.util

import org.springframework.core.io.FileSystemResource
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.reactive.function.client.WebClient
import java.nio.file.Path

fun WebClient.RequestBodySpec.multiPartBody(
    block: MultipartBodyBuilder.() -> Unit
): WebClient.RequestHeadersSpec<*> =
    this.bodyValue(MultipartBodyBuilder().apply(block).build())

fun MultipartBodyBuilder.filePart(
    name: String,
    file: Path,
    fileName: String = file.fileName.toString()
): MultipartBodyBuilder.PartBuilder = this
    .part(name, FileSystemResource(file))
    .filename(fileName)