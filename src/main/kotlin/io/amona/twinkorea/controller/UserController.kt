package io.amona.twinkorea.controller

import io.amona.twinkorea.annotation.AdminUser
import io.amona.twinkorea.annotation.MsgUser
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.request.PageRequest
import io.amona.twinkorea.request.UserRequest
import io.amona.twinkorea.service.PaymentService
import io.amona.twinkorea.service.UserService
import io.amona.twinkorea.transformer.JSONResponse
import io.amona.twinkorea.transformer.ResponseTransformer
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import springfox.documentation.annotations.ApiIgnore

@Validated
@Api(description = "user 관리")
@RequestMapping("/user")
@RestController
class UserController(
    val userService: UserService,
    val paymentService: PaymentService,
    ) {
    /**
     * SNS 회원가입
     */
    @ApiOperation(value = "SNS 회원가입", response = JSONResponse::class, notes = UserNotes.signupWithSnsAccountNote)
    @PostMapping("/signup/sns")
    fun signupWithSnsAccount(@RequestBody request: UserRequest): ResponseEntity<JSONResponse> {
        return try {
            val result = userService.createUserWithSns(request)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    /**
     * 이메일 회원가입
     */
    @ApiOperation(value = "이메일 회원가입", response = JSONResponse::class, notes = UserNotes.signupWithEmailNote)
    @PostMapping("/signup/email")
    fun signupWithEmail(@RequestBody request: UserRequest): ResponseEntity<JSONResponse> {
        return try {
            val result = userService.createUserWithEmail(request)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    /**
     * SNS 로그인
     */
    @ApiOperation(value = "SNS 로그인", response = JSONResponse::class, notes = UserNotes.loginWithSnsAccountNote)
    @PostMapping("/signin/sns")
    fun loginWithSnsAccount(@RequestBody request: UserRequest): ResponseEntity<JSONResponse> {
        return try {
            val result = userService.loginWithSns(request)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    /**
     * 이메일 로그인
     */
    @ApiOperation(value = "이메일 로그인", response = JSONResponse::class, notes = UserNotes.loginWithEmailNote)
    @PostMapping("/signin/email")
    fun loginWithEmail(@RequestBody request: UserRequest): ResponseEntity<JSONResponse> {
        return try {
            val result = userService.loginWithPhone(request)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "레퍼럴 코드 가져오기", response = JSONResponse::class, notes = UserNotes.getMyReferralNote)
    @GetMapping("/me/referral")
    fun getMyReferral(@ApiIgnore @MsgUser user: User): ResponseEntity<JSONResponse> {
        return try {
            val result = userService.getReferralCode(user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "내 회원정보 가져오기", response = JSONResponse::class, notes = UserNotes.getMyReferralNote)
    @GetMapping("/me/info")
    fun getMyInfo(@ApiIgnore @MsgUser user: User): ResponseEntity<JSONResponse> {
        return try {
            val result = userService.getMyInfo(user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "내 구매목록 가져오기", response = JSONResponse::class, notes = UserNotes.getMyPaymentLogsNote)
    @GetMapping("/me/paymentHistory")
    fun getMyPaymentLogs(
        @ApiIgnore @MsgUser user: User,
        @RequestParam(name = "page", required = true) page: Int,
        @RequestParam(name = "size", required = true) size: Int,
    ) : ResponseEntity<JSONResponse> {
        return try {
            val result = paymentService.getPurchaseHistory(user = user, pageRequest = PageRequest(page = page, size = size).of())
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "회원 탈퇴(비활성화)", response = JSONResponse::class, notes = UserNotes.deactivateUserNote)
    @PostMapping("/me/deactivate")
    fun deactivateUser(@ApiIgnore @MsgUser user: User): ResponseEntity<JSONResponse> {
        return try {
            val result = userService.deactivateUser(user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }
}

class UserNotes {
    companion object {
        const val signupWithSnsAccountNote =
            "<h1>SNS 계정으로 회원가입합니다. </h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON 타입의 Request Body를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|snsKey(필수)|string|SNS 인증 후 받은 SNS 인증서버에서 발급받은 인증키|\n" +
            "|snsProvider(필수)|string|SNS 종류 \"S\": 카카오 \"N\": 네이버 \"F\": 페이스북 \"T\": 트위터 \"A\": 애플" +
            "|nickname(필수)|string|사용할 닉네임 (카카오 API 통해 받은값)|\n" +
            "|phoneNumber(필수)|string|전화번호 (카카오 API 통해 받은값)|\n" +
            "|email(필수)|string|이메일 (카카오 API 통해 받은값)|\n"

        const val signupWithEmailNote =
            "<h1>이메일을 이용하여 회원가입 합니다.</h1>" +
            "</h3>파라미터 설명</h3>" +
            "__본 API 는 JSON 타입의 Request Body를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|email(필수)|string|이메일|\n" +
            "|pw(필수)|string|비밀번호 (대소문자 / 숫자 / 특수문자 혼용)|\n" +
            "|nickname(필수)|string|사용할 닉네임|\n" +
            "|phoneNumber(필수)|string|전화번호 (카카오 API 통해 받은값)|\n" +
            "|email(필수)|string|이메일 (카카오 API 통해 받은값)|\n"

        const val loginWithSnsAccountNote =
            "<h1>SNS 계정으로 로그인합니다. </h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON 타입의 Request Body를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|snsKey(필수)|string|SNS 인증 후 받은 SNS 인증서버에서 발급받은 인증키|\n" +
            "|snsProvider(필수)|string|SNS 종류 \"S\": 카카오 \"N\": 네이버 \"F\": 페이스북 \"T\": 트위터 \"A\": 애플\n"

        const val loginWithEmailNote =
            "<h1>이메일로 로그인 합니다. </h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON 타입의 Request Body를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|email(필수)|string|전화번호|\n" +
            "|pw(필수)|string|비밀번호 (대소문자 / 숫자 / 특수문자 혼용)\n"

        const val getMyReferralNote =
            "<h1>로그인된 사용자의 레퍼럴 코드 및 현재 보유한 청약 쿠폰 갯수를 불러옵니다.</h1>"

        const val getMyInfoNote =
            "<h1>로그인된 이메일/전화번호/닉네임을 불러옵니다.</h1>"

        const val deactivateUserNote =
            "<h1>회원을 비활성화 상태 (탈퇴)로 전환합니다. </h1>"

        const val getMyPaymentLogsNote =
            "<h1>회원의 사전 분양 구매 이력을 불러옵니다. </h1>"
    }
}

        /*
email, pw, gender, nickname, phoneNumber, smsYn
         */