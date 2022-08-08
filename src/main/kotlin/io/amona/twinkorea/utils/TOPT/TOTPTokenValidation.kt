import de.taimos.totp.TOTP
import org.apache.commons.codec.binary.Base32
import org.apache.commons.codec.binary.Hex

object TOTPTokenValidation {
    // userservice.getUserTOTPSecretKey
    fun validate(userSecretKey: String, inputCode: String): Boolean {
        // OTP 검증 요청 때마다 개인키로 OTP 생성
        val base32 = Base32()
        val bytes = base32.decode(userSecretKey)
        val hexKey = Hex.encodeHexString(bytes)
        val code = TOTP.getOTP(hexKey)

        return code == inputCode
    }
}