package io.amona.twinkorea.service.external

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.service.PreOrderService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors


@SpringBootTest
@AutoConfigureMockMvc
class PreOrderPopUpServiceTest
@Autowired constructor(
    private val appConfig: AppConfig,
    private val preOrderService: PreOrderService
) {
    @Test
    @Throws(InterruptedException::class)
    fun testCounterWithConcurrency() {
        val numberOfThreads = 100
        val service = Executors.newFixedThreadPool(100)
        for (i in 0 until numberOfThreads) {
            val user = User(
                id = 66000L + i,
                couponCount = 1000,
                email = "{${genRandomString(12)}}@asd.asd"
                )
            service.execute {
//                preOrderService.applyPreOrderTest(preOrderId = 633, user = user)
                preOrderService.applyPreOrder(preOrderId = 633, user = user)
            }
        }
        Thread.sleep(1000)
    }

    private fun genRandomString(length: Int): String {
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length).map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    @Test
    fun getDateTimeFromString() {
        val string = appConfig.preOrderAvailableTo
        val now = LocalDateTime.now()
        val dateTime = LocalDateTime.parse(string, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
        println(dateTime > now)
        println(dateTime < now)
    }
}
