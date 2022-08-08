package io.amona.twinkorea.controller

import io.amona.twinkorea.annotation.MsgUser
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.request.CellIdsRequest
import io.amona.twinkorea.request.MultiCellRequest
import io.amona.twinkorea.request.PageRequest
import io.amona.twinkorea.service.LandService
import io.amona.twinkorea.transformer.JSONResponse
import io.amona.twinkorea.transformer.ResponseTransformer
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import springfox.documentation.annotations.ApiIgnore

@Api(description = "랜드 관리")
@RequestMapping("/land")
@RestController
class LandController(val landService: LandService) {
    @ApiOperation(value = "셀들을 조합하여 랜드 만들기", response = JSONResponse::class, notes = LandNotes.createLandByCellIdsNote)
    @PostMapping("")
    fun createLandByCellIds(
        @RequestBody
        cellIds: MultiCellRequest,
        @ApiIgnore
        @MsgUser user: User
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = landService.createLandByCellIds(user = user, cellIds = cellIds.cellIds)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "내가 보유하고 있는 땅 보기", response = JSONResponse::class, notes = LandNotes.getMyLandsNote)
    @GetMapping("")
    fun getMyLands(
        @ApiParam(value = "페이지", example = "0", defaultValue = "0")
        @RequestParam(name = "page", required = true)
        page: Int,
        @ApiIgnore
        @MsgUser user: User,
        ): ResponseEntity<JSONResponse> {
        return try {
            val result = landService.getLandsByUserId(user = user, pageRequest = PageRequest(page = page).of())
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    // TODO 리스폰스 타입 이렇게 바꾸기
    @ApiOperation(value = "랜드 삭제 (어드민용)", response = JSONResponse::class, notes = LandNotes.deleteLandNote)
    @DeleteMapping("/{landId}")
    fun deleteLand(
        @ApiIgnore
        @MsgUser user: User,

        @PathVariable(value = "landId")
        landId: Long,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = landService.deleteLand(user = user, landId = landId)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "랜드 수정 (어드민용)", response = JSONResponse::class, notes = LandNotes.editLandNote)
    @PatchMapping("/{landId}")
    fun editLand(
        @ApiIgnore
        @MsgUser
        user: User,
        @PathVariable(value = "landId")
        landId: Long,
        @RequestBody
        cellIds: MultiCellRequest,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = landService.editLand(user = user, landId = landId, cellIds = cellIds.cellIds)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }
}

class LandNotes{
    companion object {
        const val createLandByCellIdsNote =
            "<h1>셀 ID로 땅을 만들어 유저에게 소속시킵니다.</h1>" +
            "셀은 지도에 표시되는 최소단위이며, 땅은 셀들의 집합으로 거래가 가능한 최소단위입니다. 기본가격 * 선택한 셀 갯수로 땅의 최초 구매 비용이 정해집니다." +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON Body 로 셀 ID의 리스트를 받습니다. (ex. {\"cellIds\":\"47547,47195,47555\"})__"
        const val getMyLandsNote =
            "<h1>내가 보유하고있는 땅의 리스트를 보여줍니다.</h1>"
        const val deleteLandNote =
            "<h1>땅을 삭제합니다.</h1>" +
            "__본 API 는 Path Parameter 로 삭제하고자 하는 땅의 landId 를 받습니다.__"
        const val editLandNote =
            "<h1>땅을 수정합니다.</h1>" +
            "__본 API 는 Path Parameter 로 수정하고자 하는 땅의 landId 를 받습니다.__\n" +
            "__본 API 는 JSON Body 로 셀 ID의 리스트를 받습니다. (ex. {\"cellIds\":\"47547,47195,47555\"})__"
    }
}