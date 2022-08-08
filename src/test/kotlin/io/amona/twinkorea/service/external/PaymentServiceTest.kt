package io.amona.twinkorea.service.external

import io.amona.twinkorea.request.PgPaymentRequest
import io.amona.twinkorea.utils.AES256EncDec
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class PaymentServiceTest
@Autowired constructor(
    private val settlebankService: SettlebankService
    ) {

    @Test
    fun settleBankCancelRequestTest() {
        val result = settlebankService.cancelRequest("STFP_PNPTM221693700220128153901M1023232")
        println(result)
    }

    @Test
    fun getSignatureForPg() {
        val authKey = "SETTLEBANKISGOODSETTLEBANKISGOOD"
        val sourceStr = "123123"
        val new = AES256EncDec.aesECBEncodeHex(authKey, sourceStr)
        println(new)
    }
}