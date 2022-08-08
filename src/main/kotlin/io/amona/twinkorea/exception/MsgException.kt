package io.amona.twinkorea.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

open class MsgException(message: String) : RuntimeException(message)

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class NotFoundException(message: String = "입력된 파라미터로 기대한 응답을 받아올 수 없습니다. 입력한 요청 값을 확인해주세요.") : MsgException(message)

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class NotNullException(message: String = "필수 입력값이 Null 입니다. 입력값을 확인해주세요.") : MsgException(message)

@ResponseStatus(code = HttpStatus.UNAUTHORIZED)
class AuthenticationException(message: String = "권한이 필요합니다. Access Token 을 확인해주세요.") : MsgException(message)

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class DuplicatedException(message: String = "이미 등록되었습니다.") : MsgException(message)

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class BalanceException(message: String = "보유 MSG가 부족합니다.") : MsgException(message)

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class WrongStatusException(message: String = "요청을 완수할 수 있는 상태가 아닙니다.") : MsgException(message)

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class ValidationException(message: String) : MsgException(message)