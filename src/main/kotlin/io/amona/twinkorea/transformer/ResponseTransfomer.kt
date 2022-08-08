package io.amona.twinkorea.transformer

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.exception.AuthenticationException
import io.amona.twinkorea.exception.MsgException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

// TODO ResponseEntity 상속받도록 변경
data class JSONResponse(val status: HttpStatus, val data: Any?, val message: String? = null)

object ResponseTransformer {
    val appConfig = AppConfig()
    fun successResponse(data: Any): ResponseEntity<JSONResponse> {
        val status = HttpStatus.OK
        val body = JSONResponse(status = status, data = data)

        val result = ResponseEntity.status(status).body(body)
        return result
    }

    fun failResponse(message: String?): JSONResponse {
        return JSONResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, data = null, message = message)
    }

    fun failResponseWithDataAndMessage(message: String?, data: Any): JSONResponse {
        return JSONResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, data = data, message = message)
    }

    fun successResponseOnlyStatus(): JSONResponse {
        return JSONResponse(status = HttpStatus.OK, data = null)
    }

    fun failResponseOnlyStatus(): JSONResponse {
        return JSONResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, data = null)
    }

    fun failResponseWithStatusAndMessage(message: String?, status: HttpStatus): JSONResponse {
        return JSONResponse(status = status, data = null, message = message)
    }

    fun errorWrapper(e: Exception): ResponseEntity<JSONResponse> {
        appConfig.logger.info { e.stackTraceToString() }
        return when(e) {
            is AuthenticationException -> {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    failResponseWithStatusAndMessage(e.message, HttpStatus.UNAUTHORIZED)
                )
            }
            is MsgException -> {
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    failResponseWithStatusAndMessage(e.message, HttpStatus.BAD_REQUEST)
                )
            }
            else -> {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    failResponseWithStatusAndMessage(e.message, HttpStatus.INTERNAL_SERVER_ERROR)
                )
            }
        }
    }
}