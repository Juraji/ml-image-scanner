package nl.juraji.ml.imageScanner.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@ConstructorBinding
@ConfigurationProperties("machine-box.tag-box")
data class TagBoxConfiguration(
    override val endpoint: String,
    override val stateFile: String,
    val blacklist: List<String>,
) : MachineBoxConfiguration {

    @Bean("tagBoxWebClient")
    fun tagBoxWebClient(): WebClient = WebClient.builder()
        .baseUrl(endpoint)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()
}
