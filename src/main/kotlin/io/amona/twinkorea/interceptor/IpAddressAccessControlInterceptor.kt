package io.amona.twinkorea.interceptor

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.exception.AuthenticationException
import io.amona.twinkorea.repository.AdminWhiteListRepository
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class IpAddressAccessControlInterceptor(
    private val adminWhiteListRepo: AdminWhiteListRepository,
    private val appConfig: AppConfig,
    ) : HandlerInterceptor {

    val logger = appConfig.logger

    @Throws(Exception::class)
    override fun afterCompletion(arg0: HttpServletRequest, arg1: HttpServletResponse, arg2: Any, arg3: Exception?) {
    }

    @Throws(Exception::class)
    override fun postHandle(arg0: HttpServletRequest, arg1: HttpServletResponse, arg2: Any, arg3: ModelAndView?) {
    }

    @Throws(Exception::class)
    override fun preHandle(req: HttpServletRequest, res: HttpServletResponse, arg2: Any): Boolean {
        val ipList = adminWhiteListRepo.findAll().map {it.ip}
        val xForwardedFor = req.getHeader("x-forwarded-for") ?: "1.1.1.1, 1.1.1.1"
        val clientIp = xForwardedFor.split(",")[0]
        logger.info("client ip $clientIp") //접속한 사용자의 IP
        if (clientIp !in ipList) throw AuthenticationException("접근할 수 없는 IP 입니다.")
        return true
    }
}