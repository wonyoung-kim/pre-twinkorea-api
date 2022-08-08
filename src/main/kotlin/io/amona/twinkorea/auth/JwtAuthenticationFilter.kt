package io.amona.twinkorea.auth

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.service.external.AuthService
import io.amona.twinkorea.service.UserService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest


class JwtAuthenticationFilter(private val userService: UserService,
                              private val siksinService: AuthService,
                              private val appConfig: AppConfig
                              ): UsernamePasswordAuthenticationFilter() {

    private val jwtTokenProvider: JwtTokenProvider = JwtTokenProvider(userService, siksinService, appConfig)


    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain) {
        val token = jwtTokenProvider.resolveToken((request as HttpServletRequest?)!!)
        if (token != null && jwtTokenProvider.validateToken(token)) {
            val authentication = jwtTokenProvider.getAuthentication(token)
            SecurityContextHolder.getContext().authentication = authentication
//            try {
//                val authentication = jwtTokenProvider.getAuthentication(token)
//                SecurityContextHolder.getContext().authentication = authentication as UsernamePasswordAuthenticationToken }
//            catch (e: AuthenticationException) {
//                response!!.writer.println("asdasd")
//            }

        }
        chain.doFilter(request, response)
    }
}