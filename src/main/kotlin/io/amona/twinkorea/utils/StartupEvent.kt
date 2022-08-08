package io.amona.twinkorea.utils

import com.fasterxml.jackson.databind.ObjectMapper
import io.amona.twinkorea.configuration.AppConfig
import org.redisson.api.RedissonClient
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component


@Component
class StartupEvent(
    private val redissonClient: RedissonClient,
    private val appConfig: AppConfig,
    private val objectMapper: ObjectMapper,
): ApplicationListener<ApplicationReadyEvent> {
    /**
     * This event is executed as late as conceivably possible to indicate that
     * the application is ready to service requests.
     */
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        val logger = appConfig.logger

//        val classPathResource = ResourceUtils.getURL("classpath:cell_json_by_area_id/").openStream()
//        println(classPathResource.read())
//        InputStreamReader(classPathResource).forEachLine {
//            val fileClassPathResource = ClassPathResource("/cell_json_by_area_id/${it}")
//            val redisKey = "geojson_${it.replace(".geojson", "")}"
//            val geoJsonValue = InputStreamReader(fileClassPathResource.inputStream).readText()
//            val node = objectMapper.readTree(geoJsonValue)
//            println(redisKey)
//            println(geoJsonValue)
//            redissonClient.getBucket<JsonNode>(redisKey).set(node)
//        }


//
//        val file = ResourceUtils.getFile("classpath:cell_json_by_area_id/")
//        file.listFiles().forEach {
//            val redisKey = "geojson_${it.name.replace(".geojson", "")}"
//            val geoJsonValue = File("$it").readText(Charsets.UTF_8)
//            val node = objectMapper.readTree(geoJsonValue)
//            redissonClient.getBucket<JsonNode>(redisKey).set(node)
//        }

        logger.info { "[TWINKOREA API] Completely Put GeoJson Data Into Redis" }
    }
}