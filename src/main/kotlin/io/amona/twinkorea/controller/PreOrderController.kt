package io.amona.twinkorea.controller

import io.amona.twinkorea.annotation.MsgUser
import io.amona.twinkorea.annotation.OptionalMsgUser
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.exception.NotFoundException
import io.amona.twinkorea.repository.PreOrderRepository
import io.amona.twinkorea.request.MyPreOrderListPageRequest
import io.amona.twinkorea.request.PageRequest
import io.amona.twinkorea.request.PreOrderRequest
import io.amona.twinkorea.service.PreOrderService
import io.amona.twinkorea.transformer.JSONResponse
import io.amona.twinkorea.transformer.ResponseTransformer
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import springfox.documentation.annotations.ApiIgnore

@Api(description = "사전청약 관리")
@RequestMapping("/pre-order")
@RestController
class PreOrderController(
    val preOrderService: PreOrderService,
    val preOrderRepository: PreOrderRepository
                         ) {
    @ApiOperation(value = "사전청약 등록 (어드민용)", response = JSONResponse::class, notes = PreOrderNotes.createPreOrderNote)
    @PostMapping("")
    fun createPreOrder(
        @ApiIgnore
        @MsgUser
        user: User,
        @RequestBody
        request: PreOrderRequest,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.createPreOrder(user = user, request = request)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "사전청약 리스트 불러오기", response = JSONResponse::class, notes = PreOrderNotes.getListNote)
    @GetMapping("/all")
    fun getList(@RequestParam(name = "page", required = true)
                page: Int,
                @RequestParam(name = "size", required = true)
                size: Int,
                @RequestParam(name = "option", required = false)
                option: String?
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.getAllPreOrderList(option = option, pageRequest = PageRequest(page = page, size = size).of())
            return ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "지도 렌더링을 위한 사전 청약 리스트 불러오기", response = JSONResponse::class,)
    @GetMapping("/all/map")
    fun getListForMap(
        @ApiIgnore @OptionalMsgUser user: User
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.getPreOrderListForMap(user)
            return ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "사전청약의 ID로 응모", response = JSONResponse::class, notes = PreOrderNotes.applyPreOrderNote)
    @PostMapping("/{preOrderId}")
    fun applyPreOrder(@PathVariable(value = "preOrderId")
                      preOrderId: Long,
                      @ApiIgnore
                      @MsgUser
                      user: User): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.applyPreOrder(preOrderId = preOrderId, user = user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "사전청약의 폴리곤 구역 ID로 응모", response = JSONResponse::class, notes = PreOrderNotes.applyPreOrderByAreaId)
    @PostMapping("/polygon/{polygonId}")
    fun applyPreOrderByAreaId(
        @PathVariable(value = "polygonId")
        polygonId: Long,
        @ApiIgnore
        @MsgUser
        user: User
    ): ResponseEntity<JSONResponse> {
        return try {
            val preOrder = preOrderRepository.findByAreaIdNoneRO(polygonId) ?: throw NotFoundException("찾을수없는 사전청약")
            val result = preOrderService.applyPreOrder(preOrderId = preOrder.id, user = user)
//            val result = preOrderService.applyPreOrderByAreaId(areaId = polygonId, user = user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "사전청약의 폴리곤 구역 ID로 대기청약 응모", response = JSONResponse::class, notes = PreOrderNotes.applyWaitingOrderByAreaId)
    @PostMapping("/waiting/polygon/{polygonId}")
    fun applyWaitingOrderByAreaId(
        @PathVariable(value = "polygonId")
        polygonId: Long,
        @ApiIgnore
        @MsgUser
        user: User
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.applyWaitingOrder(areaId = polygonId, user = user, preOrderId = null)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "사전청약 ID로 대기청약 응모", response = JSONResponse::class, notes = PreOrderNotes.applyWaitingOrderNote)
    @PostMapping("/waiting/{preOrderId}")
    fun applyWaitingOrderByPreOrderId(
        @PathVariable(value = "preOrderId")
        preOrderId: Long,
        @ApiIgnore
        @MsgUser
        user: User
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.applyWaitingOrder(preOrderId = preOrderId, user = user, areaId = null)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "회원의 사전청약 신청 리스트 확인", response = JSONResponse::class, notes = PreOrderNotes.getMyPreOrdersNote)
    @GetMapping("/me/list")
    fun getMyPreOrders(@ApiIgnore
                       @MsgUser
                       user: User,
                       @RequestParam(name = "page", required = true)
                       page: Int,
                       @RequestParam(name = "size", required = true)
                       size: Int,
                       ): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.getUserPreOrderList(user, pageRequest = MyPreOrderListPageRequest(page = page.toLong(), limit = size.toLong()))
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "내 청약/대기청약/티켓/초대친구 갯수 보기", response = JSONResponse::class, notes = PreOrderNotes.getMyPreOrderInfoNote)
    @GetMapping("/me/info")
    fun getMyPreOrderInfo(@ApiIgnore
                          @MsgUser
                          user: User): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.getMyPreOrderInfo(user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "사전청약 상세 정보 확인", response = JSONResponse::class, notes = PreOrderNotes.getPreOrderDetail)
    @GetMapping("/{preOrderId}")
    fun getPreOrderDetail(@PathVariable(value = "preOrderId")
                          preOrderId: Long): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.getPreOrderDetail(preOrderId = preOrderId)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "사전청약 상세 정보 확인 (폴리곤 ID 이용)", response = JSONResponse::class, notes = PreOrderNotes.getPreOrderDetailByAreaId)
    @GetMapping("/polygon/{polygonId}")
    fun getPreOrderDetailByAreaId(@PathVariable(value = "polygonId")
                          polygonId: Long): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.getPreOrderDetailByAreaId(polygonId)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }


    @ApiOperation(value = "친구초대 랭킹보기", response = JSONResponse::class, notes = PreOrderNotes.getInvitingRankNote)
    @GetMapping("/rank/top10")
    fun getInvitingRank(): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.getTop10Inviter()
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "내 친구초대 랭킹보기", response = JSONResponse::class, notes = PreOrderNotes.getMyInvitingRankNote)
    @GetMapping("/rank/me")
    fun getMyInvitingRank(
        @ApiIgnore
        @MsgUser
        user: User
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.getMyInvitingRank(user)
            return ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "사전청약에 해당하는 cell 리스트 확인", response = JSONResponse::class, notes = PreOrderNotes.getPreOrderCellsByAreaIdNote)
    @GetMapping("/polygon/{polygonId}/cells")
    fun getPreOrderCellsByAreaId(
        @PathVariable(value = "polygonId") polygonId: Long,
        @ApiIgnore @OptionalMsgUser user: User
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.getCellsByAreaId(areaId = polygonId, user = user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "사전청약 제목 수정 (어드민용)", response = JSONResponse::class, notes = PreOrderNotes.editPreOrder)
    @PatchMapping("/{preOrderId}")
    fun editPreOrder(@PathVariable(value = "preOrderId")
                     preOrderId: Long,
                     @ApiIgnore
                     @MsgUser
                     user: User,
                     @RequestBody
                     request: PreOrderRequest
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.editPreOrder(user = user, preOrderId = preOrderId, request = request)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "사전청약 삭제 (어드민용)", response = JSONResponse::class, notes = PreOrderNotes.deletePreOrderNote)
    @DeleteMapping("/{preOrderId}")
    fun deletePreOrder(@PathVariable(value = "preOrderId")
                       preOrderId: Long,
                       @ApiIgnore
                       @MsgUser
                       user: User,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = preOrderService.deletePreOrder(user = user, preOrderId = preOrderId)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }
}

class PreOrderNotes{
    companion object {
        const val createPreOrderNote =
            "<h1>사전청약을 등록합니다. 본 API 는 어드민 회원 전용입니다. </br>" +
            "사전청약의 기준 단위는 식신의 areaId로, 대지역번호(upHpAreaId) 내 속한 모든 area를 사전청약의 대상으로 등록시킵니다.  </h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON 타입의 Request Body 를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|upHpAreaId|integer|사전청약에 등록할 대지역번호 (해당 대지역에 속한 지역이 사전청약 대상으로 등록됨).|\n"

        const val getListNote =
            "<h1>사전청약 리스트를 불러옵니다. * 페이지 인덱스는 0부터 시작합니다. </h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Query Parameter 로 option 을 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|option=soldout|string|마감된 청약건만 불러옵니다.|\n" +
            "|option=onsale|string|진행중인 청약건만 불러옵니다.|\n" +
            "|option=|null|전체 청약건을 불러옵니다.|\n"

        const val applyPreOrderNote =
            "<h1>로그인된 사용자의 명의로 사전청약에 등록합니다. (preOrderId 이용)</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 신청하고자 하는 *사전청약의 Id* 를 받습니다.__"

        const val applyPreOrderByAreaId =
            "<h1>로그인된 사용자의 명의로 사전청약에 등록합니다. (geoJson의 폴리곤 Id 이용)</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 신청하고자 하는 사전청약의 *폴리곤 Id* 를 받습니다.__"

        const val applyWaitingOrderNote =
            "<h1>로그인된 사용자의 명의로 대기청약에 등록합니다. (preOrderId 이용)</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 대기하고자 하는 *사전청약의 Id* 를 받습니다.__"

        const val applyWaitingOrderByAreaId =
            "<h1>로그인된 사용자의 명의로 대기청약 등록합니다. (geoJson의 폴리곤 Id 이용)</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 대기하고자 하는 대기청약의 *폴리곤 Id* 를 받습니다.__"

        const val getMyPreOrdersNote =
            "<h1>로그인 된 회원이 신청한 사전 청약의 목록을 불러옵니다. * 페이지 인덱스는 0부터 시작합니다.</h1>" +
            "<h3>사전 청약중 구매 가능한 사전 청약건은 \"preOrder\"로 표시합니다. / </br>" +
            "사전 청약중 이미 셀을 구매하여 분양을 마친 사전 청약건은 \"purchased\"로 표시합니다. </br> " +
            "대기 청약건은 \"waitingOrder\"로 표시합니다.</h3>"

        const val getMyPreOrderInfoNote =
            "<h1>유저의 사전청약 관련 수치(신청한 청약 갯수, 초대한 회원 갯수, 남은 쿠폰 갯수, 분양가능한 청약 갯수)를 보여줍니다.</h1>"

        const val getPreOrderDetail =
            "<h1>사전청약의 상세 정보를 불러옵니다. </br>" +
            "사전청약에 포함된 cell 이나, 사전청약시 제공되는 땅에 포함된 식신 식당의 숫자등 디테일한 데이터를 제공합니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 확인하고자 하는 사전청약의 Id 를 받습니다.__"

        const val getPreOrderDetailByAreaId =
            "<h1>폴리곤의 Id를 이용하여 사전청약의 상세 정보를 불러옵니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 확인하고자 하는 사전청약의 Id 를 받습니다.__"

        const val getPreOrderCellsByAreaIdNote =
            "<h1>특정 폴리곤에 속한 셀들의 정보를 리턴합니다. 셀들의 랜더링은 별도의 클라이언트에서 하고, </br>" +
            "상황에 따라 동적으로 변하는 셀들의 정보만 본 API 를 통해 리턴합니다. </h1> " +
            "<h3> 구매 가능한 셀인경우 status 값에 PURCHASABLE 를, 구매 불가능한 경우 PURCHASED, 내가 소유한 경우 OWNED 를 리턴합니다.</h3>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 확인하고자 하는 사전청약의 Id 를 받습니다.__"

        const val editPreOrder =
            "<h1>사전청약의 이름을 수정합니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 수정하고자 하는 사전청약의 preOrderId 를 받습니다.__\n" +
            "__본 API 는 JSON 타입의 Request Body 를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|name|string|수정할 사전청약 이름입니다.|\n"

        const val deletePreOrderNote =
            "<h1>사전청약을 삭제합니다.</br>" +
            "이미 사전청약이 시작된 경우 삭제할 수 없습니다..</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 삭제하고자 하는 사전청약의 Id 를 받습니다.__"

        const val getInvitingRankNote =
            "<h1>친구초대 랭킹 테이블을 불러옵니다.</h1>"

        const val getMyInvitingRankNote =
            "<h1>나의 친구초대 상황을 불러옵니다. (랭크 및 갯수) (초대한 유저가 0명일경우 랭크는 0으로 표시)</h1>"
    }
}