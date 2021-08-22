package nl.juraji.ml.imageScanner.util.cli

import kotlinx.cli.ArgType
import java.nio.file.Path
import java.nio.file.Paths

object ArgTypes {
    val Boolean = ArgType.Boolean
    val String = ArgType.String
    val Int = ArgType.Int
    val Double = ArgType.Double
    inline fun <reified T : Enum<T>> Choice(
        noinline toVariant: (String) -> T = { s -> enumValues<T>().first { it.toString().equals(s, true) } },
        noinline variantToString: (T) -> String = { it.toString() }
    ) = ArgType.Choice(enumValues<T>().toList(), toVariant, variantToString)

    val Path = object : ArgType<Path>(true) {
        override val description = "{ Path to file }"
        override fun convert(value: kotlin.String, name: kotlin.String): Path = Paths.get(value)
    }
}
