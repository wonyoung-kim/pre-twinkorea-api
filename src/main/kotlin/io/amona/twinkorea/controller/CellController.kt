package io.amona.twinkorea.controller

import io.amona.twinkorea.annotation.MsgUser
import io.amona.twinkorea.annotation.OptionalMsgUser
import io.amona.twinkorea.annotation.StringFormatCoordinates
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.request.CellIdsRequest
import io.amona.twinkorea.request.MultiCellRequest
import io.amona.twinkorea.service.CellService
import io.amona.twinkorea.transformer.JSONResponse
import io.amona.twinkorea.transformer.ResponseTransformer
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import springfox.documentation.annotations.ApiIgnore

@Validated
@Api(description = "땅 관리")
@RequestMapping("/cell")
@RestController
class CellController(val cellService: CellService) {
    @ApiOperation(value = "특정 좌표 영역 내의 땅 목록 확인", response = JSONResponse::class, notes = CellNotes.getCellListByRangeNote)
    @GetMapping("/list")
    fun getCellListByRange(@RequestParam(name = "start", required = true)
                           @StringFormatCoordinates
                           start: String,
                           @RequestParam(name = "end", required = true)
                           @StringFormatCoordinates
                           end: String): ResponseEntity<JSONResponse> {
        return try {
            val cellList = cellService.readCellsByRange(start = start, end = end)
            ResponseTransformer.successResponse(data = cellList)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "셀들의 식신 데이터 확인", response = JSONResponse::class, notes = CellNotes.getCellInfoListByCellIdNote)
    @PostMapping("/list/info")
    fun getCellInfoListByCellId(
        @RequestBody
        cellIds: MultiCellRequest,
    ): ResponseEntity<JSONResponse> {
        return try {
            val cellInfoList = cellService.readCellWithInfo(cellIds = cellIds.cellIds)
            ResponseTransformer.successResponse(data = cellInfoList)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "특정 셀의 정보 확인", response = JSONResponse::class, notes = CellNotes.getCellByIdNote)
    @GetMapping("/{cellId}")
    fun getCellById(
        @PathVariable(name="cellId") cellId: Long,
        @ApiIgnore @OptionalMsgUser user: User,
    ): ResponseEntity<JSONResponse> {
        return try {
            val cellData = cellService.getCellDetail(cellId = cellId, user = user)
            ResponseTransformer.successResponse(data = cellData)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "여러개 셀의 정보 확인", response = JSONResponse::class, notes = CellNotes.getMultiCellsByIdNote)
    @PostMapping("/multi")
    fun getMultiCellsById(
        @RequestBody request: MultiCellRequest,
        @ApiIgnore @MsgUser user: User,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = cellService.getMultiCellDetail(request, user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "여러개 셀의 점유 여부 확인", response = JSONResponse::class, notes = CellNotes.getMultiCellsOnPaymentStatus)
    @PostMapping("/multi/onPayment")
    fun getMultiCellsOnPaymentStatus(
        @RequestBody request: MultiCellRequest,
        @ApiIgnore @MsgUser user: User,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = cellService.getMultiCellOnPaymentStatus(request)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }
}

class CellNotes{
    companion object {
        const val getCellListByRangeNote =
            "<h1>좌하단 좌표와 우상단 좌표로 범위를 지정하여 해당 범위 내의 모든 셀 리스트를 불러옵니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Query Parameter 로 좌표 최소범위, 최대 범위를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|start|float|EPSG:4326 좌표계의 위경도를 콤마로 구분한 값입니다. (ex. start=126.933100,37.555236)\n" +
            "|end|float|EPSG:4326 좌표계의 위경도를 콤마로 구분한 값입니다. (ex. end=126.982727,37.587286)\n"
        const val getCellInfoListByCellIdNote =
            "<h1>셀이 위치한 좌표 범위 내 식신의 등록된 식당 정보를 불러옵니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON Body 로 셀 ID의 리스트를 받습니다. (ex. {\"cellIds\":\"47547,47195,47555\"})__"
        const val getCellByIdNote =
            "<h1>특정 셀의 셀 ID 를 받아 셀의 정보를 반환합니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 확인하고자 하는 셀의 ID 를 받습니다.__"
        const val getMultiCellsByIdNote =
            "<h1>셀 ID 리스트를 받아 해당 셀들의 상세 데이터 리스트를 반환합니다. </h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON Body 로 셀 ID의 리스트를 받습니다. (ex. {\"cellIds\":\"47547,47195,47555\"})__"
        const val getMultiCellsOnPaymentStatus =
            "<h1>셀 ID 리스트를 받아 해당 셀들의 점유 여부를 반환합니다. </h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON Body 로 셀 ID의 리스트를 받습니다. (ex. {\"cellIds\":\"47547,47195,47555\"})__"
    }
}