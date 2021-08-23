package nl.juraji.ml.imageScanner.model.face

data class Face(
    val id: String = "",
    val name: String = "",
    val confidence: Double,
    val matched: Boolean = false,
    val rect: FacePosition = FacePosition(0, 0, 0, 0),
)
