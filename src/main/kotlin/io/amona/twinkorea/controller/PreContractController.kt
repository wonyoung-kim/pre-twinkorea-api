package io.amona.twinkorea.controller

import io.amona.twinkorea.annotation.MsgUser
import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.request.DataEncRequestForMyAccount
import io.amona.twinkorea.request.MyAccountPaymentRequest
import io.amona.twinkorea.request.PageRequest
import io.amona.twinkorea.service.CellService
import io.amona.twinkorea.service.PreContractService
import io.amona.twinkorea.service.external.SettlebankService
import io.amona.twinkorea.transformer.JSONResponse
import io.amona.twinkorea.transformer.ResponseTransformer
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView
import springfox.documentation.annotations.ApiIgnore

@Api(description = "사전계약 관리")
@RequestMapping("/pre-contract")
@RestController
class PreContractController(
    val preContractService: PreContractService,
    val settlebankService: SettlebankService,
    val cellService: CellService,
    private val appConfig: AppConfig,
    ) {
    val redirectUrl = appConfig.redirectUrl

    @ApiOperation(value = "셀 계약 시작", response = JSONResponse::class, notes = PreContractNotes.startContractNote)
    @PostMapping("/{cellId}/start")
    fun startContract(
        @PathVariable(value = "cellId") cellId: Long,
        @ApiIgnore @MsgUser user: User,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = preContractService.startPayment(user = user, cellId = cellId)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "셀 계약 완료", response = JSONResponse::class, notes = PreContractNotes.endContractNote)
    @PostMapping("/{cellId}/end")
    fun endContract(
        @PathVariable(value = "cellId") cellId: Long,
        request: MyAccountPaymentRequest,
    ): RedirectView {
        val areaId = cellService.readCell(cellId).areaId
        return try {
            val result = preContractService.endPaymentMyAccount(cellId = cellId, request = request)
            val redirectUrl = if (result.resultCd == "0") {
                "${redirectUrl}/${areaId}?success=true"
            } else {
                "${redirectUrl}/${areaId}?fail=true"
            }
            appConfig.logger.info { "[TWINKOREA API] 결제프로세스 종료. 결과 메시지: ${result.resultMsg}" }
            RedirectView(redirectUrl)
        } catch (e: Exception) {
            val redirectUrl = "${redirectUrl}/${areaId}?fail=true"
            appConfig.logger.info { "[TWINKOREA API] 결제프로세스 종료. 결과 메시지: ${e.message}" }
            RedirectView(redirectUrl)
        }
    }

    // TODO 보안 강화 필요
    @ApiOperation(value = "셀 계약 과정 중 이탈", response = JSONResponse::class, notes = PreContractNotes.exitWhilePaymentMyAccountNote)
    @PostMapping("/{cellId}/exit")
    fun exitWhilePaymentMyAccount(
        @PathVariable(value = "cellId") cellId: Long
    ): RedirectView {
        val areaId = cellService.readCell(cellId).areaId
        val redirectUrl = "${redirectUrl}/${areaId}?fail=true"
        val result = preContractService.exitWhilePaymentMyAccount(cellId)
        appConfig.logger.info { "[TWINKOREA API] 결제중 중도 취소 발생 셀#${cellId}의 onpayment 상태 변경 -> $result" }
        return RedirectView(redirectUrl)
    }

    @ApiOperation(value = "분양 가능 지역 리스트 불러오기", response = JSONResponse::class, notes = PreContractNotes.getPurchasablePreContractListNote)
    @GetMapping("/list")
    fun getPurchasablePreContractList(
        @ApiIgnore @MsgUser user: User,
        @RequestParam(name = "areaId", required = false) areaId: Long?,
        @RequestParam(name = "page", required = true) page: Int,
        @RequestParam(name = "size", required = true) size: Int,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = preContractService.getPurchasablePreContractList(user = user, areaId = areaId, pageRequest = PageRequest(page = page, size = size).of())
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "구매 이력 불러오기", response = JSONResponse::class, notes = PreContractNotes.getPurchaseHistoryListNote)
    @GetMapping("/history")
    fun getPurchaseHistoryList(
        @ApiIgnore @MsgUser user: User,
        @RequestParam(name = "page", required = true) page: Int,
        @RequestParam(name = "size", required = true) size: Int,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = preContractService.getPurchaseHistory(user = user, pageRequest = PageRequest(page = page, size = size).of())
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "결제 데이터 암호화 (내통장결제용)", response = JSONResponse::class, notes = PreContractNotes.getSignDataForMyAccountNote)
    @PostMapping("/signature/my-account")
    fun getSignDataForMyAccount(
        @ApiIgnore @MsgUser user: User,
        @RequestBody request: DataEncRequestForMyAccount
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = settlebankService.getSignDataMyAccount(request)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }
}

class PreContractNotes {
    companion object {
        const val getPurchasablePreContractListNote =
            "<h1>분양 가능한 지역 리스트를 불러옵니다. </br>" +
            "사전청약 당시 회원이 신청한 지역을 기준으로, 구매 가능한 지역과 이미 구매 이력이 있는 지역을 구분하여 보여줍니다.  </br> " +
            "* 분양 가능한 지역이 없는 경우 (청약 이력이 없는 경우) -> content 에 빈 list 가 응답됩니다.</h1>" +
            "<h3> purchasableCellCount 에는 구매 가능한 셀 갯수를, totalCellCount 에는 전체 셀 갯수를 리턴합니다. (리저브, 구매된 셀 모두 포함한 전체 갯수)</h3>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Query Parameter 로 조회시 제외하고자 할 지역 ID(areaId)를 받습니다.__"

        const val startContractNote =
            "<h1>셀 사전 분양을 위해 결제 프로세스를 시작합니다. </br>" +
            "본 API를 호출 할 경우 해당 셀은 10분간 결제 진행중 상태로 변경되며, 다른 유저의 구매는 제한됩니다.</h1>" +
            "<h3> 구매 예약에 성공한 경우 응답값의 message 파라미터에 \"PAYMENT_START\"를, timeRemaining 파라미터에 남은 결제 시간을 응답합니다. </br>" +
            "이미 구매 중인 셀에 같은 유저가 요청한 경우 message 파라미터에 \"ON_PAYMENT\"를, timeRemaining 파라미터에 남은 결제 시간을 응답합니다. </br>" +
            "이미 다른 유저가 구매중이거나, 구매 완료한 셀에 요청한 경우 에러를 응답합니다.</h3>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 확인하고자 하는 셀의 Id 를 받습니다.__"

        const val endContractNote =
            "<h1>셀 사전 분양을 위해 결제 프로세스를 종료합니다. </br>" +
            "<h3> 결제 여부가 확인되면, 셀을 요청한 유저 명의로 귀속시키고, 구매 이력을 생성합니다." +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 확인하고자 하는 셀의 Id 를 받습니다.__" +
            "__본 API 는 JSON 타입의 Request Body 를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|authNo|string|세틀뱅크에서 생성한 인증 단계 응답값|\n" +
            "|ordNo|string|결제 요청시생성한 고유 주문번호|\n" +
            "|trPrice|string|가맹점에서 최초 결제 요청한 금액|\n"

        const val exitWhilePaymentMyAccountNote =
            "<h1>내통장 결제 과정 도중 이탈하는경우 호출됩니다.</br>" +
            "<h3>세틀뱅크 UI 호출시 클라이언트에서 cancelUrl 을 본 API로 지정해주셔야합니다. </h3>"

        const val getPurchaseHistoryListNote =
            "<h1>회원의 사전 분양 구매 이력을 불러옵니다. </h1>"

        const val getSignDataForMyAccountNote =
            "<h1>내통장 결제 요청을 위한 데이터 암호화 API 입니다. </br>" +
            "ordNo, trDay, trTime, trPrice 를 입력받아 </br>" +
            "SHA256(상점아이디 + 주문번호 + 거래일자 + 거래시간 + 거래금액(plain 값) + 인증키)의 값과 AES256(거래금액)의 값을 응답합니다.</h1>"

        const val getSingDataForPgNote =
            "<h1>PG 결제 요청을 위한 데이터 암호화 API 입니다. </br>" +
            "method, mchntTrdNo, trdDt, trdTm, trdAmt 를 입력받아 SHA256(상점아이디 + 결제수단 + 상점주문번호 + 요청일자 + 요청시간 + 거래금액(평문) + 해쉬키)의 값을 응답합니다."
    }
}

