package io.amona.twinkorea.controller

import io.amona.twinkorea.annotation.MsgUser
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.request.OfferRequest
import io.amona.twinkorea.request.OfferSearchRequest
import io.amona.twinkorea.request.PageRequest
import io.amona.twinkorea.service.OfferService
import io.amona.twinkorea.transformer.JSONResponse
import io.amona.twinkorea.transformer.ResponseTransformer
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import springfox.documentation.annotations.ApiIgnore

@Api(description = "거래 관리")
@RequestMapping("/offer")
@RestController
class OfferController(val offerService: OfferService) {
    @ApiOperation(value = "제안 가능한 오퍼 리스트 보기", response = JSONResponse::class, notes = OfferNotes.getAllOffersNote)
    @GetMapping("")
    fun getAllOffers(@ApiParam(value = "시/도", example = "서울특별시")
                     @RequestParam(name = "address-one", required = false)
                     addressOne: String?,
                     @ApiParam(value = "시군구", example = "동작구")
                     @RequestParam(name = "address-two", required = false)
                     addressTwo: String?,
                     @ApiParam(value = "행정동/읍/면", example = "신대방동")
                     @RequestParam(name = "address-three", required = false)
                     addressThree: String?,
                     @ApiParam(value = "검색어", example = "서울특별시")
                     @RequestParam(name = "q", required = false)
                     q: String?,
                     @ApiParam(value = "페이지", example = "0", defaultValue = "10")
                     @RequestParam(name = "page", required = true)
                     page: Int,
                     ): ResponseEntity<JSONResponse> {
        return try {
            val offerSearchRequest = OfferSearchRequest(
                addressOne = addressOne, addressTwo = addressTwo, addressThree = addressThree, text = q
            )
            val resultList = offerService.getAllOffersByAddress(request = offerSearchRequest, pageRequest = PageRequest(page = page).of())
            ResponseTransformer.successResponse(resultList)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "인기 매물 보기", response = JSONResponse::class, notes = OfferNotes.getPopularOffersNote)
    @GetMapping("/popular")
    fun getPopularOffers(): ResponseEntity<JSONResponse> {
        return try {
            val result = offerService.getPopularOffers()
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "제안 받은 오퍼 목록 보기", response = JSONResponse::class, notes = OfferNotes.getOffersToMeNote)
    @GetMapping("/to-me")
    fun getOffersToMe(@ApiIgnore
                      @MsgUser user: User,
                      @RequestParam(name = "page", required = true)
                      page: Int,
                      ): ResponseEntity<JSONResponse> {
        return try {
            val result = offerService.getOfferToMe(user = user, pageRequest = PageRequest(page = page).of())
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "제안 한 오퍼 목록 보기", response = JSONResponse::class, notes = OfferNotes.getOffersFromMeNote)
    @GetMapping("/from-me")
    fun getOffersFromMe(@ApiIgnore
                        @MsgUser user: User,
                        @RequestParam(name = "page", required = true)
                        page: Int,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = offerService.getOfferFromMe(user = user, pageRequest = PageRequest(page = page).of())
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "땅 판매 등록", response = JSONResponse::class, notes = OfferNotes.createSellOfferNote)
    @PostMapping("/sell")
    fun createSellOffer(@ApiIgnore
                        @MsgUser user: User,
                        @RequestBody request: OfferRequest): ResponseEntity<JSONResponse> {
        return try {
            val result = offerService.createSellOffer(user,  request)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "셀 구매", response = JSONResponse::class, notes = OfferNotes.createBuyOfferNote)
    @PostMapping("/{offer_id}/buy")
    fun createBuyOffer(@ApiIgnore
                       @MsgUser user: User,
                       @PathVariable("offer_id") offerId: Long): ResponseEntity<JSONResponse> {
        return try {
            val result = offerService.createBuyOffer(user = user, offerId = offerId)
            ResponseTransformer.successResponse(data = result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "구매 제안 승낙", response = JSONResponse::class, notes = OfferNotes.acceptBuyOfferNote)
    @PostMapping("/{offer_id}/accept")
    fun acceptBuyOffer(@ApiIgnore
                       @MsgUser seller: User,
                       @PathVariable("offer_id") offerId: Long): ResponseEntity<JSONResponse> {
        return try {
            val result = offerService.changeOfferStatus(user = seller, offerId = offerId, offerStatus = "ACCEPT")
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "구매 제안 거절", response = JSONResponse::class, notes = OfferNotes.denyBuyOfferNote)
    @PostMapping("/{offer_id}/deny")
    fun denyBuyOffer(@ApiIgnore
                     @MsgUser seller: User,
                     @PathVariable("offer_id") offerId: Long): ResponseEntity<JSONResponse> {
        return try {
            val result = offerService.changeOfferStatus(user = seller, offerId = offerId, offerStatus = "DENY")
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "판매 등록 취소", response = JSONResponse::class, notes = OfferNotes.cancelSellOfferNote)
    @PostMapping("/{offer_id}/sell/cancel")
    fun cancelSellOffer(@ApiIgnore
                        @MsgUser seller: User,
                        @PathVariable("offer_id") offerId: Long): ResponseEntity<JSONResponse> {
        return try {
            val result = offerService.cancelSellOffer(user = seller, offerId = offerId)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "구매 등록 취소", response = JSONResponse::class, notes = OfferNotes.cancelBuyOfferNote)
    @PostMapping("/{offer_id}/buy/cancel")
    fun cancelBuyOffer(@ApiIgnore
                       @MsgUser buyer: User,
                       @PathVariable("offer_id") offerId: Long): ResponseEntity<JSONResponse> {
        return try {
            val result = offerService.cancelBuyOffer(user = buyer, offerId = offerId)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }
}

class OfferNotes{
    companion object {
        const val getAllOffersNote =
            "<h1>제안 가능한 오퍼 리스트를 불러옵니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Query Parameter 로 검색 필터를 지정할 수 있습니다. 페이지를 제외한 모든 파라미터는 Optional 파라미터입니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|addressOne|string|도/시 명 (ex. 경기도)|\n" +
            "|addressTwo|string|시군구 명 (ex. 고양시)|\n" +
            "|addressThree|string|행정동 명 (ex. 중산동)|\n" +
            "|q|string|검색어 (ex. 꿀매물)|\n" +
            "|page|int|페이지 (0번부터 시작)|"

        const val getPopularOffersNote =
            "<h1>메인 페이지에서 보여줄 인기 매물리스트를 불러옵니다.</h1>"

        const val getOffersToMeNote =
            "<h1>제안 받은 오퍼 목록을 확인합니다.</h1>" +
            "내가 등록한 판매 건 중 구매 요청이 있었던 오퍼 리스트를 불러옵니다."

        const val getOffersFromMeNote =
            "<h1>내가 제안한 오퍼 목록을 확인합니다.</h1>"

        const val createSellOfferNote =
            "<h1>내가 보유하고있는 땅을 판매하기위해 거래소에 등록합니다.</h1>" +
            "판매 등록하고자 하는 landId가 유저의 소유가 아닐 경우 에러를 반환합니다." +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON 타입의 Request Body 를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|landId|int|땅 id|\n" +
            "|price|int|셀당 가격 (총 가격은 땅에 포함된 셀 갯수 * 셀당 가격으로 정해짐)|\n" +
            "|name|string|매물 이름|\n"

        const val createBuyOfferNote =
            "<h1>거래소에 등록된 판매 건에 대해 구매를 신청합니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 구매하고자 하는 거래건의 OfferId 를 받습니다.__"

        const val acceptBuyOfferNote =
            "<h1>내가 등록한 판매 건에 대한 구매 의향자의 구매 요청을 승낙합니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 승낙하고자 하는 거래건의 OfferId 를 받습니다.__"

        const val denyBuyOfferNote =
            "<h1>내가 등록한 판매 건에 대한 구매 의향자의 구매 요청을 거절합니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 거절하고자 하는 거래건의 OfferId 를 받습니다.__"

        const val cancelSellOfferNote =
            "<h1>내가 등록한 판매 건을 취소합니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 취소하고자 하는 거래건의 OfferId 를 받습니다.__"

        const val cancelBuyOfferNote =
            "<h1>내가 응찰한 구매 건을 취소합니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 구매하고자 하는 거래건의 OfferId 를 받습니다.__"
    }
}
