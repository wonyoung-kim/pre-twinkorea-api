package io.amona.twinkorea.controller

import io.amona.twinkorea.annotation.MsgUser
import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.request.DataEncRequestForMyAccount
import io.amona.twinkorea.request.MyAccountPaymentRequest
import io.amona.twinkorea.request.PageRequest
import io.amona.twinkorea.service.CellService
import io.amona.twinkorea.service.ContractService
import io.amona.twinkorea.service.external.SettlebankService
import io.amona.twinkorea.transformer.JSONResponse
import io.amona.twinkorea.transformer.ResponseTransformer
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView
import springfox.documentation.annotations.ApiIgnore

@Api(description = "셀 계약 관리")
@RequestMapping("/contract")
@RestController
class ContractController(
    val contractService: ContractService,
    val cellService: CellService,
    val settlebankService: SettlebankService,
    private val appConfig: AppConfig
) {
    val redirectUrl = appConfig.redirectUrl

    @ApiOperation(value = "셀 계약 시작", response = JSONResponse::class, notes = ContractNote.startContractNote)
    @PostMapping("/start")
    fun startContract(
        @RequestParam(name = "cellIds", required = true) cellIds: String,
        @ApiIgnore @MsgUser user: User,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = contractService.startPayment(user, cellIds)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "셀 계약 완료", response = JSONResponse::class, notes = ContractNote.endContractNote)
    @PostMapping("/end")
    fun endContract(
        @RequestParam(name = "cellIds", required = true) cellIds: String,
        request: MyAccountPaymentRequest,
        ): RedirectView {
        val cellIdsList = cellIds.replace(" " ,"").split(",").toMutableList()
        val firstCellId = cellIdsList[0].toLong()
        val areaId = cellService.readCell(firstCellId).areaId
        return try {
            val result = contractService.endPayment(cellIds, request)
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

    @ApiOperation(value = "셀 계약 테스트", response = JSONResponse::class)
    @PostMapping("/test")
    fun testContract(
        @RequestParam(name = "cellIds", required = true) cellIds: String,
        request: MyAccountPaymentRequest,
    ): RedirectView {
        val cellIdsList = cellIds.replace(" " ,"").split(",").toMutableList()
        val firstCellId = cellIdsList[0].toLong()
        val areaId = cellService.readCell(firstCellId).areaId
        return try {
            val result = contractService.paymentTest(cellIds, request)
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

    @ApiOperation(value = "셀 계약 과정 중 이탈", response = JSONResponse::class, notes = PreContractNotes.exitWhilePaymentMyAccountNote)
    @PostMapping("/exit")
    fun exitWhilePaymentMyAccount(
        @RequestParam(name = "cellIds", required = true) cellIds: String,
    ): RedirectView {
        val areaId = cellService.getFirstCellFromCellIds(cellIds).areaId!!
        val redirectUrl = "${redirectUrl}/${areaId}?fail=true"
        val result = contractService.exitPayment(cellIds)
        appConfig.logger.info { "[TWINKOREA API] 결제중 중도 취소 발생 셀#${cellIds}의 onpayment 상태 변경 -> $result" }
        return RedirectView(redirectUrl)
    }

    @ApiOperation(value = "구매 가능한 지역 리스트 조회", response = JSONResponse::class)
    @GetMapping("/list/purchasable")
    fun getPurchasableList(
        @RequestParam(name = "areaId", required = false) areaId: Long?,
        @RequestParam(name = "page", required = true) page: Int,
        @RequestParam(name = "size", required = true) size: Int,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = contractService.getPurchasableAreaList(areaId = areaId, pageRequest = PageRequest(page = page, size = size).of())
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

class ContractNote{
    companion object{
        const val startContractNote =
            "<h1>셀 분양을 위해 결제 프로세스를 시작합니다. </br>" +
            "본 API를 호출 할 경우 해당 셀은 10분간 결제 진행중 상태로 변경되며, 다른 유저의 구매는 제한됩니다.</h1>" +
            "이미 다른 유저가 구매중이거나, 구매 완료한 셀에 요청한 경우 에러를 응답합니다.</h3>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Query Parameter 로 구매하고자 하는 셀 ID들의 목록을 콤마로 구분된 문자열로 받습니다.__"

        const val endContractNote =
            "<h1>셀 분양을 위해 결제 프로세스를 종료합니다. </br>" +
            "<h3> 결제 여부가 확인되면, 셀을 요청한 유저 명의로 귀속시키고, 구매 이력을 생성합니다." +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Query Parameter 로 구매하고자 하는 셀 ID들의 목록을 콤마로 구분된 문자열로 받습니다.__"
    }
}