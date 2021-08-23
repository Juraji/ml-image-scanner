package nl.juraji.ml.imageScanner.util

fun <T : Any> List<T>.unique(): List<T> = toSet().toList()