package io.amona.twinkorea.utils.TOPT

import org.apache.commons.codec.binary.Base32
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.security.SecureRandom

object TOTPTokenGenerator {
    private const val GOOGLE_URL = "https://www.google.com/chart?chs=200x200&chld=M|0&cht=qr&chl="

    // 최초 개인 Security Key 생성 -> user 테이블에 저장 필요
    fun generateSecretKey(): String {
        val random = SecureRandom()
        val bytes = ByteArray(20)
        random.nextBytes(bytes)
        val base32 = Base32()
        return base32.encodeToString(bytes)
    }

    // 개인키, 계정명(시스템 사용자 ID), 발급자를 받아서 구글OTP 인증용 링크를 생성
    fun getGoogleAuthenticatorBarcode(secretKey: String?, account: String, issuer: String): String {
        return try {
            (GOOGLE_URL + "otpauth://totp/"
                    + URLEncoder.encode("$issuer:$account", "UTF-8").replace("+", "%20")
                    + "?secret=" + URLEncoder.encode(secretKey, "UTF-8").replace("+", "%20")
                    + "&issuer=" + URLEncoder.encode(issuer, "UTF-8").replace("+", "%20"))
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException(e)
        }
    }
}