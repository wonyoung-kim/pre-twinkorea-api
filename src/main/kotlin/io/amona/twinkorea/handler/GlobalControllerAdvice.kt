package io.amona.twinkorea.handler

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.exception.MsgException
import io.amona.twinkorea.transformer.JSONResponse
import io.amona.twinkorea.transformer.ResponseTransformer
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * value 에 내가 처리하고자 하는 익셉션을 넣는다.
 * try catch 를 쓰지 않아도 에러를 알아서 잡게 된다.
 */
@RestControllerAdvice
class GlobalControllerHandler(private val appConfig: AppConfig) {
    @ExceptionHandler(value = [Exception::class])
    fun exception(e: Exception) : JSONResponse {
        appConfig.logger.error { e.stackTraceToString() }
        return ResponseTransformer.failResponse(message = e.message)
    }

    @ExceptionHandler(value = [RuntimeException::class])
    fun runtimeException(e: RuntimeException) : JSONResponse {
        appConfig.logger.error { e.stackTraceToString() }
        return ResponseTransformer.failResponse(message = e.message)
    }

    @ExceptionHandler(value = [IndexOutOfBoundsException::class])
    fun indexOutOfBoundsException(e: IndexOutOfBoundsException) : JSONResponse {
        return ResponseTransformer.failResponse(message = e.toString())
    }

    @ExceptionHandler(value = [MethodArgumentTypeMismatchException::class])
    fun methodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException) : JSONResponse {
        return ResponseTransformer.failResponse(message = e.toString())
    }

    @ExceptionHandler(value = [NumberFormatException::class])
    fun numberFormatException(e: NumberFormatException) : JSONResponse {
        return ResponseTransformer.failResponse(message = e.toString())
    }

    // 컨트롤러 로직 밖에서 발생하는 예외 처리 (JWT 없거나 등등)
    @ExceptionHandler(value = [MsgException::class])
    fun msgException(e: MsgException) : ResponseEntity<JSONResponse> {
        appConfig.logger.error { e.stackTraceToString() }
        return ResponseTransformer.errorWrapper(e)
    }
}