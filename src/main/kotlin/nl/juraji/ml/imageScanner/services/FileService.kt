package nl.juraji.ml.imageScanner.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import nl.juraji.ml.imageScanner.util.iif
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.*

@Service
class FileService(
    private val objectMapper: ObjectMapper,
) {
    private val ioScheduler: Scheduler = Schedulers
        .newBoundedElastic(
            10,
            Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
            "nio-file-ops",
            30,
            true
        )

    fun fileExists(path: Path): Mono<Boolean> =
        deferIoFrom { Files.exists(path) }

    fun walkDirectory(path: Path): Flux<Path> =
        deferIoFrom { Files.walk(path) }
            .flatMapMany { Flux.fromStream(it) }

    fun serialize(obj: Any): Mono<ByteArray> =
        deferIoFrom { objectMapper.writeValueAsBytes(obj) }

    fun <T : Any> deserialize(bytes: ByteArray, typeReference: TypeReference<out T>): Mono<T> =
        deferIoFrom { objectMapper.readValue(bytes, typeReference) }

    fun createDirectories(path: Path): Mono<Path> =
        deferIoFrom { Files.createDirectories(path) }

    fun writeDataBuffersTo(
        dataBuffers: Publisher<DataBuffer>,
        path: Path,
        vararg options: OpenOption = DEFAULT_OPEN_OPTIONS
    ): Mono<Void> =
        createDirectories(path.parent)
            .then(deferIo { DataBufferUtils.write(dataBuffers, path, *options) })

    fun writeBytesTo(
        bytes: ByteArray,
        path: Path,
        vararg options: OpenOption = DEFAULT_OPEN_OPTIONS
    ): Mono<Path> =
        createDirectories(path.parent)
            .then(deferIoFrom { Files.write(path, bytes, *options) })

    fun moveFile(
        source: Path,
        target: Path,
        vararg options: CopyOption = DEFAULT_COPY_OPTIONS
    ): Mono<Path> =
        createDirectories(target.parent)
            .then(deferIoFrom { Files.move(source, target, *options) })

    fun readBytes(path: Path): Mono<ByteArray> =
        deferIoFrom { Files.readAllBytes(path) }

    fun readExifUserComment(path: Path): Mono<String> =
        deferIo { getExifCompatMetaData(path) }
            .mapNotNull { it.getFieldValue(ExifTagConstants.EXIF_TAG_USER_COMMENT) }

    fun setExifUserComment(source: Path, comment: String, force: Boolean): Mono<Path> {
        val onImageSupported = deferIo { getExifCompatMetaData(source) }
            .mapNotNull { it.outputSet }
            .switchIfEmpty { Mono.just(TiffOutputSet()) }
            .map { outputSet ->
                outputSet.apply {
                    with(orCreateExifDirectory) {
                        removeField(ExifTagConstants.EXIF_TAG_USER_COMMENT)
                        add(ExifTagConstants.EXIF_TAG_USER_COMMENT, comment)
                    }
                }
            }
            .map { outputSet ->
                source.parent.resolve("META_UPDATED_${source.fileName}").apply {
                    usingFIS(source) { fis ->
                        usingFOS(this) { fos ->
                            with(ExifRewriter()) {
                                if (force) updateExifMetadataLossy(fis, fos, outputSet)
                                else updateExifMetadataLossless(fis, fos, outputSet)
                            }
                        }
                    }
                }
            }
            .flatMap { moveFile(it, source) }
            .onErrorContinue { e, _ -> logger.error("Could not write meta data to file (${e.message}): $source") }

        val onImageNotSupported: Mono<Path> = Mono.defer {
            logger.warn("Meta data not supported for file: $source")
            Mono.empty()
        }

        return Mono.just(source)
            .flatMap(this::probeContentType)
            .iif(onImageSupported, onImageNotSupported) { it == MediaType.IMAGE_JPEG_VALUE }
    }

    fun probeContentType(path: Path): Mono<String> =
        deferIoFrom { Files.probeContentType(path) }

    private fun getExifCompatMetaData(path: Path): Mono<TiffImageMetadata> = Mono.just(path)
        .map { it to it.fileName.toString() }
        .mapNotNull { (p, fName) -> usingFIS(p) { Imaging.getMetadata(it, fName) } }
        .mapNotNull {
            when (it) {
                is JpegImageMetadata -> it.exif
                is TiffImageMetadata -> it
                else -> null
            }
        }

    private fun <T : Any?> usingFIS(path: Path, block: (InputStream) -> T) =
        Files.newInputStream(path).use(block)

    private fun usingFOS(path: Path, block: (OutputStream) -> Unit) =
        Files.newOutputStream(path).use(block)

    private fun <T> deferIo(block: () -> Mono<T>): Mono<T> =
        Mono.defer(block).subscribeOn(ioScheduler)

    private fun <T> deferIoFrom(block: () -> T?): Mono<T> =
        deferIo { Mono.justOrEmpty(block()) }

    companion object : LoggerCompanion(FileService::class) {
        private val DEFAULT_OPEN_OPTIONS: Array<StandardOpenOption> = arrayOf(
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
        )
        private val DEFAULT_COPY_OPTIONS: Array<StandardCopyOption> = arrayOf(
            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
        )
    }
}