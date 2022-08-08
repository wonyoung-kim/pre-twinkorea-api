package io.amona.twinkorea.configuration

import io.amona.twinkorea.interceptor.IpAddressAccessControlInterceptor
import io.amona.twinkorea.resolver.UserArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    // 로그인 검증용 리졸버
    private val userArgumentResolver: UserArgumentResolver,
    private val ipAddressAccessControlInterceptor: IpAddressAccessControlInterceptor,
    private val appConfig: AppConfig
    ): WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(userArgumentResolver)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        if (appConfig.ipWhiteList == "true") {
            registry.addInterceptor(ipAddressAccessControlInterceptor)
                .order(1)
                .addPathPatterns("/admin/**")
        }
    }
}