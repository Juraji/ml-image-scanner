package nl.juraji.ml.imageScanner.util.cli

import kotlinx.cli.DefaultRequiredType
import kotlinx.cli.MultipleOption
import kotlinx.cli.MultipleOptionType
import kotlinx.cli.default

@Suppress("UNCHECKED_CAST")
fun <T : Any, OptionType : MultipleOptionType>
        MultipleOption<T, OptionType, DefaultRequiredType.None>.defaultOrEmpty(value: Collection<T>):
        MultipleOption<T, OptionType, DefaultRequiredType.Default> {
    // The cast should be fine, since [MultipleOptionType] always should have at least an empty set as default value
    return if (value.isEmpty()) this as MultipleOption<T, OptionType, DefaultRequiredType.Default>
    else this.default(value)
}