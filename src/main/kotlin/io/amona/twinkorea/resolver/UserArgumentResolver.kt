package io.amona.twinkorea.resolver

import io.amona.twinkorea.annotation.AdminUser
import io.amona.twinkorea.annotation.MsgUser
import io.amona.twinkorea.annotation.OptionalMsgUser
import io.amona.twinkorea.auth.JwtTokenProvider
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.exception.AuthenticationException
import io.amona.twinkorea.service.UserService
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class UserArgumentResolver(
    private val tokenProvider: JwtTokenProvider,
    private val userService: UserService,
): HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.getParameterAnnotation(MsgUser::class.java) != null
                || parameter.getParameterAnnotation(AdminUser::class.java) != null
                || parameter.getParameterAnnotation(OptionalMsgUser::class.java) != null
                && parameter.parameterType == User::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any? {
        val token = webRequest.getHeader("Authorization")
        // MsgUser 일 때
        if (parameter.getParameterAnnotation(MsgUser::class.java) != null) {
            return if (token == null) {
                throw AuthenticationException()
            } else {
                val auth = tokenProvider.getAuthentication(token)
                val user = userService.getUserBySnsId(auth.name)
                if (!user.deactivate) {
                    auth.principal
                } else {
                    throw AuthenticationException()
                }

            }

        // AdminUser 일 때
        } else if (parameter.getParameterAnnotation(AdminUser::class.java) != null) {
            return if (token == null) {
                throw AuthenticationException()
            } else {
                val auth = tokenProvider.getAuthentication(token)
                val user = userService.getUserBySnsId(auth.name)
                // 비활성화 회원
                if (user.deactivate) {
                    throw AuthenticationException()
                }
                if (user.admin) {
                    auth.principal
                } else {
                    throw AuthenticationException("어드민 회원만 접근할 수 있습니다.")
                }
            }

        // OptionalMsgUser 일 때
        } else {
            val tempUser = User(id = 0)
            return if (token == null) {
                tempUser
            } else {
                try {
                    val auth = tokenProvider.getAuthentication(token)
                    val user = userService.getUserBySnsId(auth.name)
                    if (!user.deactivate) {
                        auth.principal
                    } else {
                        tempUser
                    }
                } catch (e: AuthenticationException) {
                    return tempUser
                }
            }
        }
    }
}
