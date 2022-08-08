package io.amona.twinkorea

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
class TwinKoreaApiApplication

fun main(args: Array<String>) {
    runApplication<TwinKoreaApiApplication>(*args)
}