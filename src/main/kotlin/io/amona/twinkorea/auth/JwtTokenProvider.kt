package io.amona.twinkorea.auth

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.exception.AuthenticationException
import io.amona.twinkorea.service.external.AuthService
import io.amona.twinkorea.service.UserService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.context.annotation.Lazy
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.*
import javax.servlet.http.HttpServletRequest

@Component
class JwtTokenProvider(@Lazy
                       private val userService: UserService,
                       @Lazy
                       private val siksinService: AuthService,
                       @Lazy
                       private val appConfig: AppConfig,
) {
    companion object {
        const val SECRET_KEY = "wUFSsBebal"
        const val ADMIN_ACCESS_TOKEN_EXPIRATION_TIME = 30 * 60 * 1000L
        const val ACCESS_TOKEN_EXPIRATION_TIME = 30 * 24 * 60 * 60 * 1000L
        const val REFRESH_TOKEN_EXPIRATION_TIME = 90 * 24 * 60 * 60 * 1000L
    }

    /**
     * SnsKey 와 SnsProvider 로 msg 용 jwt 생성
     */
    fun createToken(snsId: String, admin: Boolean? = false): String {
        val data = snsId
        val claims = Jwts.claims().setSubject(data)
        val now = Date()
        return when (admin) {
            true -> Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(Date(now.time + ADMIN_ACCESS_TOKEN_EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact()
            else -> Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(Date(now.time + ACCESS_TOKEN_EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact()
        }
    }

    fun createRefreshToken(snsId: String): String {
        val data = snsId
        val claims = Jwts.claims().setSubject(data)
        val now = Date()
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + REFRESH_TOKEN_EXPIRATION_TIME))
            .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
            .compact()
    }

    private fun getSnsId(token: String): String {
        try {
            val data = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).body.subject
            return data.split(",")[0]
        } catch (e: Exception) {
            throw AuthenticationException("SNS 인증절차를 정상적으로 수행할 수 없습니다.")
        }
    }

    fun getAuthentication(token: String): UsernamePasswordAuthenticationToken {
        val user = userService.loadUserByUsername(getSnsId(token))
        return UsernamePasswordAuthenticationToken(user, "", user.authorities)
    }

    fun resolveToken(request: HttpServletRequest): String? {
        return request.getHeader("Authorization")
    }

    fun validateToken(jwt: String): Boolean {
        return try {
            val claims = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(jwt)
            claims.body.expiration.after(Date())
        } catch (e: Exception) {
            SecurityContextHolder.clearContext()
            false
        }
    }
}