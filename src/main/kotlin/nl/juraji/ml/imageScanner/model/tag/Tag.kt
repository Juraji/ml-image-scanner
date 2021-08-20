package nl.juraji.ml.imageScanner.model.tag

data class Tag(
    val id: String?,
    val tag: String,
    val confidence: Double,
)