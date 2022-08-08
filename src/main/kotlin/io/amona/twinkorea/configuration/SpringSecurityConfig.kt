package io.amona.twinkorea.configuration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.amona.twinkorea.auth.JwtAuthenticationFilter
import io.amona.twinkorea.repository.AdminWhiteListRepository
import io.amona.twinkorea.service.UserService
import io.amona.twinkorea.service.external.AuthService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.sql.Timestamp
import java.util.*

@Configuration
@EnableWebSecurity
@EnableGlobalAuthentication
class SpringSecurityConfig(@Lazy private val userService: UserService,
                           @Lazy private val siksinService: AuthService,
                           @Lazy private val appConfig: AppConfig,
                           @Lazy private val adminWhiteListRepo: AdminWhiteListRepository
                           ): WebSecurityConfigurerAdapter() {
    @Bean
    fun restAuthenticationEntryPoint(): AuthenticationEntryPoint? {
        return AuthenticationEntryPoint { _, httpServletResponse, _ ->
            val errorObject: MutableMap<String, Any> = HashMap()
            val errorCode = 401
            errorObject["message"] = "Access Denied"
            errorObject["error"] = HttpStatus.UNAUTHORIZED
            errorObject["code"] = errorCode
            errorObject["timestamp"] = Timestamp(Date().time)
            httpServletResponse.contentType = "application/json;charset=UTF-8"
            httpServletResponse.status = errorCode
            httpServletResponse.writer.write(jacksonObjectMapper().writeValueAsString(errorObject))
        }
    }

    override fun configure(http: HttpSecurity) {
        http
            .cors().and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .csrf().disable()
            .formLogin().disable()
            .httpBasic().disable()
            .exceptionHandling()
                .authenticationEntryPoint(restAuthenticationEntryPoint()).and()
            .authorizeRequests()
                .antMatchers("/v2/api-docs", "/swagger-ui/**", "/swagger-resources/**", "/webjars/**", "/h2/**", "/swagger/**").permitAll()
                .antMatchers(
                    "/default/**", "/pre-order/**", "/user/**", "/cell/**", "/offer/**",
                    "/restaurant/**", "/bookmark/**", "/land/**", "/pre-contract/**",
                    "/contract/**", "/admin/**"
                ).permitAll()
                .anyRequest().authenticated()

        http
            .addFilterBefore(JwtAuthenticationFilter(userService, siksinService, appConfig), UsernamePasswordAuthenticationFilter::class.java)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf(
            "http://localhost:8080",
            "http://localhost:3000",
            "http://localhost:9999",
            "http://pre.twinkorea.io",
            "http://dev-pre.twinkorea.io",
            "https://pre.twinkorea.io",
            "https://dev-pre.twinkorea.io",
            "https://dev-admin.twinkorea.io",
            "https://admin.twinkorea.io",
            "https://tbezauth.settlebank.co.kr",
            "https://nspay.settlebank.co.kr",
            "https://ezauth.settlebank.co.kr",
            "https://ezauth.settlebank.co.kr:8081",
            "https://ezauthapi.settlebank.co.kr",
            "https://ezauthapi.settlebank.co.kr:8081",
            "ezauthapi.settlebank.co.kr",
            "ezauthapi.settlebank.co.kr:8081",
            "tbnpg.settlebank.co.kr",
            "npg.settlebank.co.kr",
        )
        configuration.allowedMethods = listOf("GET", "POST", "OPTIONS", "DELETE", "PUT", "PATCH")
        configuration.exposedHeaders = listOf("Access-Control-Allow-Headers")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
