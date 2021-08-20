package nl.juraji.ml.imageScanner.model.face

data class Face(
    val id: String = "",
    val name: String = "",
    val matched: Boolean,
    val confidence: Double,
    val rect: FacePosition = FacePosition(0, 0, 0, 0),
)
