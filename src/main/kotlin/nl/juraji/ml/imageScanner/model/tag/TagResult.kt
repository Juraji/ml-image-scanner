package nl.juraji.ml.imageScanner.model.tag

import com.fasterxml.jackson.annotation.JsonProperty

data class TagResult(
    val tags: List<Tag>,
    @JsonProperty("custom_tags")
    val customTags: List<Tag>,
)
