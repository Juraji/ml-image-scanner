package nl.juraji.ml.imageScanner.util.cli

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

fun ArgParser.stringOption(
    fullName: String? = null,
    shortName: String? = null,
    description: String? = null,
    deprecatedWarning: String? = null
) = option(ArgType.String, fullName, shortName, description, deprecatedWarning)

fun ArgParser.intOption(
    fullName: String? = null,
    shortName: String? = null,
    description: String? = null,
    deprecatedWarning: String? = null
) = option(ArgType.Int, fullName, shortName, description, deprecatedWarning)

fun ArgParser.booleanOption(
    fullName: String? = null,
    shortName: String? = null,
    description: String? = null,
    deprecatedWarning: String? = null
) = option(ArgType.Boolean, fullName, shortName, description, deprecatedWarning)

fun ArgParser.pathOption(
    fullName: String? = null,
    shortName: String? = null,
    description: String? = null,
    deprecatedWarning: String? = null
) = option(ArgTypes.Path, fullName, shortName, description, deprecatedWarning)

fun ArgParser.uriOption(
    fullName: String? = null,
    shortName: String? = null,
    description: String? = null,
    deprecatedWarning: String? = null
) = option(ArgTypes.URI, fullName, shortName, description, deprecatedWarning)

private object ArgTypes {
    val Path = object : ArgType<Path>(true) {
        override val description = "{ File path }"
        override fun convert(value: kotlin.String, name: kotlin.String): Path = Paths.get(value)
    }

    val URI = object : ArgType<URI>(true) {
        override val description: kotlin.String = "{ Web uri }"
        override fun convert(value: kotlin.String, name: kotlin.String): URI = java.net.URI.create(value)
    }
}
