package io.amona.twinkorea.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.AbstractRequestLoggingFilter
import javax.servlet.http.HttpServletRequest

@Configuration
class CustomLoggingFilter(appConfig: AppConfig): AbstractRequestLoggingFilter() {
    val logger = appConfig.logger
    private val excludeUrls = setOf(
        "/default/health-check",
        "/user/signin/email", "/user/signin/sns",
        "/user/signup/email", "/user/signup/sns")

    override fun shouldLog(request: HttpServletRequest): Boolean {
        if(excludeUrls.contains(request.requestURI)) {
            return false
        }
        return logger.isInfoEnabled
    }

    override fun beforeRequest(request: HttpServletRequest, message: String) {
    }

    override fun afterRequest(request: HttpServletRequest, message: String) {
        logger.info(message)
    }
}

@Configuration
class RequestLoggingConfig {
    @Bean
    fun requestLoggingFilter(): CustomLoggingFilter {
        val filter = CustomLoggingFilter(appConfig = AppConfig())
        filter.setIncludeClientInfo(true)
        filter.setIncludeHeaders(true)
        filter.setIncludePayload(true)
        filter.setIncludeQueryString(true)
        filter.setMaxPayloadLength(1000)
        return filter
    }
}