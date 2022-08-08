package io.amona.twinkorea.controller.admin

import io.amona.twinkorea.annotation.AdminUser
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.request.*
import io.amona.twinkorea.service.AdminService
import io.amona.twinkorea.service.CellService
import io.amona.twinkorea.transformer.JSONResponse
import io.amona.twinkorea.transformer.ResponseTransformer
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import springfox.documentation.annotations.ApiIgnore
import java.time.LocalDateTime

@Api(description = "어드민 툴 관련 API")
@RequestMapping("/admin")
@RestController
class AdminController(
    private val service: AdminService,
    private val cellService: CellService,
) {
    @ApiOperation(value = "회원 정보 조회", response = JSONResponse::class, notes = AdminNotes.getUserInfoNote)
    @GetMapping("/user/{userId}")
    fun getUserInfo(
        @ApiIgnore @AdminUser user: User,
        @PathVariable(name = "userId") userId: Long,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.getUserInfo(userId = userId)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "회원 리스트 불러오기", response = JSONResponse::class, notes = AdminNotes.getUserListNote)
    @GetMapping("/user/list")
    fun getUserList(
        @ApiIgnore @AdminUser user: User,
        @ApiParam(value = "이름(닉네임)", example = "김동현") @RequestParam(name = "nickname", required = false) nickname: String?,
        @ApiParam(value = "아이디(이메일)", example = "gisa@gmail.com") @RequestParam(name = "email", required = false) email: String?,
        @RequestParam(name = "page", required = true) page: Int,
        @RequestParam(name = "size", required = true) size: Int,
    ): ResponseEntity<JSONResponse> {
        return try {
            val userSearchRequest = UserSearchRequest(nickname, email)
            val result = service.getUserList(userSearchRequest, PageRequest(page, size).of())
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }


    @ApiOperation(value = "회원 탈퇴 시키기", response = JSONResponse::class)
    @PostMapping("/user/{userId}/deactivate")
    fun deactivateUser(
        @ApiIgnore @AdminUser user: User,
        @PathVariable(name = "userId") userId: Long,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.deactivateUser(userId = userId, adminUser = user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "결제 기록 불러오기", response = JSONResponse::class, notes = AdminNotes.getPaymentHistoryNote)
    @GetMapping("/payment/list")
    fun getPaymentList(
        @ApiIgnore @AdminUser user: User,
        @ApiParam(value = "거래번호(tid)") @RequestParam(name = "tid", required = false) tid: String?,
        @ApiParam(value = "아이디(이메일)") @RequestParam(name = "email", required = false) email: String?,
        @ApiParam(value = "날짜필터 시작", example = "2022-01-10T01:30:00") @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @ApiParam(value = "날짜필터 종료", example = "2022-01-14T01:30:00") @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?,
        @RequestParam(name = "page", required = true) page: Int,
        @RequestParam(name = "size", required = true) size: Int,
        ): ResponseEntity<JSONResponse> {
        return try {
            val paymentLogSearchRequest = PaymentLogSearchRequest(tid, email, startDate, endDate)
            val result = service.getPaymentHistory(paymentLogSearchRequest, PageRequest(page, size).of())
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "특정 셀의 정보 확인", response = JSONResponse::class, notes = AdminNotes.getCellInfoNote)
    @GetMapping("/cell/{cellId}")
    fun getCellInfo(
        @PathVariable(name="cellId") cellId: Long): ResponseEntity<JSONResponse> {
        return try {
            val cellData = cellService.getCellDetail(cellId = cellId)
            ResponseTransformer.successResponse(data = cellData)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "전체 폴리곤의 셀 데이터 확인", response = JSONResponse::class, notes = AdminNotes.getAllPolygonCellData)
    @GetMapping("/polygon")
    fun getAllPolygonCellData(
        @ApiIgnore @AdminUser user: User,
        @ApiParam(value = "사전청약 이름") @RequestParam(name = "name", required = false) name: String?,
        @ApiParam(value = "행정구역") @RequestParam(name = "district", required = false) district: String?,
        @ApiParam(value = "정렬조건") @RequestParam(name = "sort", required = false) sort: String?,
        @RequestParam(name = "page", required = true) page: Int,
        @RequestParam(name = "size", required = true) size: Int,
    ): ResponseEntity<JSONResponse> {
        return try {
            val searchResponseEntity = PolygonCellDataSearchRequest(district, name, sort)
            val result = service.getCellDataInAllPolygon(searchResponseEntity, PageRequest(page, size).of())
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "특정 폴리곤에 해당하는 셀 리스트 확인", response = JSONResponse::class, notes = AdminNotes.getCellListByAreaIdNote)
    @GetMapping("/polygon/{polygonId}")
    fun getCellListByAreaId(
        @ApiIgnore @AdminUser user: User,
        @PathVariable(name="polygonId") polygonId: Long
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.getCellListByAreaId(areaId = polygonId)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "셀들의 판매 상태 변경", response = JSONResponse::class, notes = AdminNotes.changeCellReservingStatusNote)
    @PostMapping("/cell/status")
    fun changeCellReservingStatus(
        @ApiIgnore @AdminUser user: User,
        @RequestBody cellIdsRequest: CellIdsRequest,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.changeCellReservingStatus(request = cellIdsRequest)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "환불 신청", response = JSONResponse::class, notes = AdminNotes.refundPaymentNote)
    @PostMapping("/payment/{trNo}/refund")
    fun refundPayment(
        @ApiIgnore @AdminUser user: User,
        @PathVariable(name = "trNo") trNo: String,
        @RequestBody refundRequest: RefundRequest,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.refundPayment(trNo, refundRequest, user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }
}


class AdminNotes{
    companion object {
        const val getAdminListNote =
            "<h1>등록된 전체 어드민 리스트를 확인합니다.</h1>" +
            "<h3>__본 API는 admin 권한의 유저만 접근 가능합니다.__</h3>"
        const val getAdminInfoNote =
            "<h1>어드민 ID를 통해 특정 어드민의 정보를 확인합니다.</h1>" +
            "<h3>__본 API는 admin 권한의 유저만 접근 가능합니다.__</h3>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 확인하고자 하는 어드민의 ID 를 받습니다.__"
        const val createNewAdminNote =
            "<h1>새로운 어드민을 추가합니다.</h1>" +
            "<h3>__본 API는 admin 권한의 유저만 접근 가능합니다.__</h3>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON 타입의 Request Body를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|email|string|어드민 회원의 이메일입니다 (로그인시 username 으로 사용됨)|\n" +
            "|nickname|string|어드민 회원의 이름입니다.|\n" +
            "|adminRole|string|어드민 회원의 직책입니다.|\n" +
            "|phoneNumber|string|어드민 회원의 전화번호 입니다. (ex. 010-1234-5678)|\n" +
            "|pw|string|어드민 회원의 비밀번호입니다. (로그인시 사용)|\n" +
            "|superAdmin|bool|수퍼어드민 여부 입니다.|"
        const val deactivateAdminNote =
            "<h1>특정 어드민 계정을 비활성화 합니다. </h1>" +
            "<h3>__본 API는 superAdmin 권한의 유저만 접근 가능합니다.__</h3>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 확인하고자 하는 어드민의 ID 를 받습니다.__"
        const val editAdminNote =
            "<h1>특정 어드민 계정 정보를 수정 합니다. </h1>" +
            "<h3>__본 API는 superAdmin 권한의 유저만 접근 가능합니다.__</h3>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON 타입의 Request Body를 받습니다.__ *null 로 오는 값은 기존의 값을 유지하며 수정되지 않습니다. 따라서 수정할 값만 보내주시면 됩니다.\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|email(__optional__)|string|어드민 회원의 이메일입니다 (로그인시 username 으로 사용됨)|\n" +
            "|nickname(__optional__)|string|어드민 회원의 이름입니다.|\n" +
            "|adminRole(__optional__)|string|어드민 회원의 직책입니다.|\n" +
            "|phoneNumber(__optional__)|string|어드민 회원의 전화번호 입니다. (ex. 010-1234-5678)|\n" +
            "|pw(__optional__)|string|어드민 회원의 비밀번호입니다. (로그인시 사용)|\n" +
            "|superAdmin(__optional__)|bool|수퍼어드민 여부 입니다.|\n" +
            "__본 API 는 Path Parameter 로 확인하고자 하는 어드민의 ID 를 받습니다.__"
        const val getUserInfoNote =
            "<h1>특정 회원 계정 정보를 확인 합니다. </h1>" +
            "<h3>__본 API는 admin 권한의 유저만 접근 가능합니다.__</h3>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 확인하고자 하는 유저의 ID 를 받습니다.__"
        const val getUserListNote =
            "<h1>회원 전체 리스트를 확인합니다. </h1>"
        const val getCellInfoNote =
            "<h1>특정 셀의 정보를 확인 합니다. </br> " +
            "본 API 의 결과값은 /v1/cell/{cellId} 와 동일합니다. </h1>" +
            "<h3>__본 API는 admin 권한의 유저만 접근 가능합니다.__</h3>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 확인하고자 하는 셀의 ID 를 받습니다.__"
        const val getPaymentHistoryNote =
            "<h1>결제 내역을 조회합니다. </h1>" +
            "<h3>모든 데이터는 UTC 기준으로 저장됩니다. 따라서 startDate / endDate 필터링 시 시간대를 고려하여 요청해야합니다. </br>"
        const val refundPaymentNote =
            "<h1>환불을 신청합니다. </h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 환불하고자 하는 거래 건의 trNo 를 받습니다.__\n" +
            "__본 API 는 JSON 타입의 Request Body를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|refundReason(__optional__)|string|환불사유 (미입력시 기본값으로 \"미입력\"이 입력됨|"
        const val getAllPolygonCellData =
            "<h1>모든 폴리곤의 정보를 조회합니다. </h1>" +
            "<h3>검색 가능한 값은 폴리곤 영역이 속한 행정 구역 (district), 상권명 (name), 정렬기준 (sort) 입니다. </br> " +
            "정렬기준은 \"정렬하고자하는column명,정렬조건\"입니다. (ex. name,asc) comma 사이에 공백이 있으면 안됩니다.</h3>"
        const val getCellListByAreaIdNote =
            "<h1>특정 폴리곤에 속한 셀 리스트를 조회합니다. </h1>" +
            "<h3>status 값에, 구매가능한경우 PURCHASABLE 구매된 경우 PURCHASED 구매제한인경우 RESERVED 값을 리턴합니다./ </h3>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 조회하고자 하는 폴리곤 영역의 id 를 받습니다.__"
        const val changeCellReservingStatusNote =
            "<h1>입력된 콤마로 구분된 cellId 문자열을 통해 해당하는 셀들의 reserved 상태를 변경합니다. </h1>" +
            "<h3>기존의 reserved 상태가 true 이면, false 로, // false 이면, true 로 변경합니다. </h3>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON 타입의 Request Body를 받습니다.__ *null 로 오는 값은 기존의 값을 유지하며 수정되지 않습니다. 따라서 수정할 값만 보내주시면 됩니다.\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|cellIds|string|콤마로 구분된 셀 ID 문자열 (ex. \"31100,31102,31459,31460,31463,31466,31819\")\n|" +
            "|status|string(ENUM)|\"RESERVED\"(구매제한) / \"PURCHASABLE\"(구매가능)"
        const val adminLoginNote =
            "<h1>이메일로 로그인 합니다. </h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON 타입의 Request Body를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|email(필수)|string|전화번호|\n" +
            "|pw(필수)|string|비밀번호 (대소문자 / 숫자 / 특수문자 혼용)\n" +
            "|otpCode(필수)|string|30초마다 변하는 OTP 코드|"
        const val renewOtpSecretKeyNote =
            "<h1>특정 어드민의 OTP 시크릿키를 재발급 합니다. 이 API는 슈퍼 관리자만 호출 할 수 있습니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 재발급하고자 하는 어드민의 id 를 받습니다. (userId 아님)__"

    }
}