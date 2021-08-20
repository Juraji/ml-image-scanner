package nl.juraji.ml.imageScanner.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import nl.juraji.ml.imageScanner.util.LoggerCompanion
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

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
        deferredIoFrom { path }
            .map(Files::exists)

    fun walkDirectory(path: Path): Flux<Path> =
        deferredIoFrom { path }
            .map { Files.walk(it) }
            .flatMapMany { Flux.fromStream(it) }

    fun serialize(obj: Any): Mono<ByteArray> =
        deferredIoFrom { obj }
            .map { objectMapper.writeValueAsBytes(it) }

    fun <T : Any> deserialize(bytes: ByteArray, typeReference: TypeReference<out T>): Mono<T> =
        deferredIoFrom { bytes }
            .map { objectMapper.readValue(it, typeReference) }

    fun createDirectories(path: Path): Mono<Path> =
        deferredIoFrom { path }
            .map { Files.createDirectories(path) }

    fun writeDataBuffersTo(
        dataBuffers: Publisher<DataBuffer>,
        path: Path,
        vararg options: StandardOpenOption = DEFAULT_OPEN_OPTIONS
    ): Mono<Void> =
        deferredIoFrom { dataBuffers }
            .map { Files.createDirectories(path.parent) }
            .flatMap { DataBufferUtils.write(dataBuffers, path, *options) }


    fun writeBytesTo(
        bytes: ByteArray,
        path: Path,
        vararg options: StandardOpenOption = DEFAULT_OPEN_OPTIONS
    ): Mono<Path> =
        deferredIoFrom { bytes }
            .map { Files.write(path, it, *options) }

    fun readBytes(path: Path): Mono<ByteArray> =
        deferredIoFrom { path }
            .map { Files.readAllBytes(path) }

    fun applyFileComments(path: Path, tags: String): Mono<Path> =
        deferredIoFrom {
            val outputSet: TiffOutputSet = Imaging.getMetadata(path.toFile())
                ?.let { (it as JpegImageMetadata).exif?.outputSet }
                ?: TiffOutputSet()

            val exifDirectory = outputSet.orCreateExifDirectory
            exifDirectory.removeField(ExifTagConstants.EXIF_TAG_USER_COMMENT)
            exifDirectory.add(ExifTagConstants.EXIF_TAG_USER_COMMENT, tags)

            outputSet
        }
            .map {
                val dstPath = path.parent.resolve("META_UPDATED_${path.fileName}")
                Files.newOutputStream(dstPath).use { fos ->
                    ExifRewriter().updateExifMetadataLossless(path.toFile(), fos, it)
                }
                dstPath
            }
            .map { Files.move(it, path, *DEFAULT_COPY_OPTIONS) }

    private fun <T> deferredIoFrom(block: () -> T?): Mono<T> =
        Mono.defer { Mono.justOrEmpty(block()) }.subscribeOn(ioScheduler)

    companion object : LoggerCompanion(FileService::class) {
        private val DEFAULT_OPEN_OPTIONS: Array<StandardOpenOption> = arrayOf(
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
        )
        private val DEFAULT_COPY_OPTIONS: Array<StandardCopyOption> = arrayOf(
            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
        )
    }
}