package nl.juraji.ml.imageScanner.model.tag

data class SimilarTagsResult(
    val tagsCount: Int,
    val similar: List<Tag>
)
