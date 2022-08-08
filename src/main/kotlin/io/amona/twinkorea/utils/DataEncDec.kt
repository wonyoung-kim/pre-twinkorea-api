package io.amona.twinkorea.utils

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


/**
 * SHA246 암호화 및 인코딩 object
 */
object SHA256Encrypt {
    fun encryptSHA256HexEncoding(strData: String): String {
        var passACL = ""
        val md: MessageDigest?
        try {
            md = MessageDigest.getInstance("SHA-256")
            md.reset()
            md.update(strData.toByteArray())
            val raw: ByteArray = md.digest()
            passACL = encodeHex(raw)
        } catch (e: Exception) {
            print("암호화 에러$e")
        }
        return passACL
    }

    private fun encodeHex(b: ByteArray?): String {
        val c: CharArray = Hex.encodeHex(b)
        return String(c)
    }
}

/**
 * AES256 암복호화 및 인코딩 object
 */
object AES256EncDec {
    private fun getSecretKey(sKey: String): SecretKeySpec {
        return SecretKeySpec(sKey.toByteArray(charset("UTF-8")), "AES")
    }

    fun aesECBEncodeBase64(sKey: String, plainText: String): String {
        val secretKey = getSecretKey(sKey)                                                      // secretKey 생성
        val c = Cipher.getInstance("AES/ECB/PKCS5Padding")                       // Cipher 객체 인스턴스화
        c.init(Cipher.ENCRYPT_MODE, secretKey)                                                   // Cipher 객체 초기화
        val encryptedByte = c.doFinal(plainText.toByteArray(charset("UTF-8")))      // 암호화
        return Base64.encodeBase64String(encryptedByte)                                         // Base64 Encode
    }

    fun aesECBDecodeBase64(sKey: String, encodeText: String): String {
        val secretKey = getSecretKey(sKey)
        val c = Cipher.getInstance("AES/ECB/PKCS5Padding")
        c.init(Cipher.DECRYPT_MODE, secretKey)
        val decodeByte = Base64.decodeBase64(encodeText)
        return String(c.doFinal(decodeByte))
    }

    fun aesECBEncodeHex(sKey: String, plainText: String): String {
        val secretKey = getSecretKey(sKey)
        val c = Cipher.getInstance("AES/ECB/PKCS5Padding")
        c.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedByte = c.doFinal(plainText.toByteArray(charset("UTF-8")))
        return Hex.encodeHexString(encryptedByte)
    }

    fun aesECBDecodeHex(sKey: String, encodeText: String): String {
        val secretKey = getSecretKey(sKey)
        val c = Cipher.getInstance("AES/ECB/PKCS5Padding")
        c.init(Cipher.DECRYPT_MODE, secretKey)
        val decodeByte: ByteArray = Hex.decodeHex(encodeText.toCharArray())
        return String(c.doFinal(decodeByte))
    }
}
