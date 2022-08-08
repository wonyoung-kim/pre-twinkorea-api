package io.amona.twinkorea.transformer
import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.enums.SnsProvider
import io.amona.twinkorea.request.UserRequest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class UserTransformer(
    val appConfig: AppConfig
    ) {
    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()
    fun from(request: UserRequest): User {
        val now = LocalDateTime.now()
        val encryptedPassword = if(request.pw != null) {
            passwordEncoder.encode(request.pw)
        } else {
            null
        }
        return User(
            snsProvider = request.snsProvider?.name ?: SnsProvider.X.name,
            snsId = "",                                 // 나중에 넣어줌
            email = request.email!!,                    // 회원가입시 이메일은 필수값
            pw = encryptedPassword,
            nickname = request.nickname ?: "",
            phoneNumber = request.phoneNumber!!,
            admin = false,
            bookmarkCount = 0,
            invitingCount = 0,
            restaurantMapCount = 0,
            referralCode = genRandomString(12),
            couponCount = appConfig.defaultCouponValue.toLong(),
            refreshToken = "",
            createdAt = now,
            updatedAt = now
//            role = UserRole.ROLE_USER
        )
    }

    private fun genRandomString(length: Int): String {
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length).map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}