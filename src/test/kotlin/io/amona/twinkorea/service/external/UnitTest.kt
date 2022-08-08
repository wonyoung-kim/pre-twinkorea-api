package io.amona.twinkorea.service.external

import io.amona.twinkorea.utils.AES256EncDec
import io.amona.twinkorea.utils.TOPT.TOTPTokenGenerator
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class UnitTest {
    @Test
    fun datetime() {
        val canceledAt = LocalDateTime.parse(
            "20220119150146", DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        ).minusHours(9L)
        println(canceledAt)
    }

    @Test
    fun getTimeString() {
        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
        println(time)
    }

    @Test
    fun dataEnc(){
        val key = "pgSettle30y739r82jtd709yOfZ2yK5K"
        val source = "391"
        println(AES256EncDec.aesECBEncodeBase64(key, source))
    }

    @Test
    fun base64(){
        val oriString = "id,desc"
        val sortTarget = oriString.split(",")[0]
//        val encodedString: String = Base64.getEncoder().encodeToString(oriString.toByteArray())
        println(sortTarget)
    }

    @Test
    fun genOTPCode() {
        val result = TOTPTokenGenerator.getGoogleAuthenticatorBarcode(secretKey = "SDYLMUUPIGQII6WMMFFFL7X5N6GSDRZ4", "super_admin","TwinKorea Admin Auth")
        println(result)
    }
}