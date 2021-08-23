package nl.juraji.ml.imageScanner.model.face

import com.fasterxml.jackson.annotation.JsonProperty

data class SimilarFace(
    val rect: FacePosition,
    @JsonProperty("similar_faces")
    val similarFaces: List<Face>
)
