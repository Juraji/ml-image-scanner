package nl.juraji.ml.imageScanner.model.face

data class DetectFacesResult(
    val facesCount: Int,
    val faces: List<Face>,
)
