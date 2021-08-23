package nl.juraji.ml.imageScanner.model.tag

data class SimilarImagesResult(
    val tagsCount: Int,
    val similar: List<Tag>
)
