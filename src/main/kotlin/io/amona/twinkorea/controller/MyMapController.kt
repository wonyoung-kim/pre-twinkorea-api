package io.amona.twinkorea.controller

import io.amona.twinkorea.annotation.MsgUser
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.request.MyMapRequest
import io.amona.twinkorea.service.MyMapService
import io.amona.twinkorea.transformer.JSONResponse
import io.amona.twinkorea.transformer.ResponseTransformer
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import springfox.documentation.annotations.ApiIgnore

@Api(description = "나의 맛집 지도 관리")
@RequestMapping("/my-map")
@RestController
class MyMapController(private val service: MyMapService) {
    @ApiOperation(value = "마이맛집 지도 만들기", response = JSONResponse::class, notes = MyMapNotes.addMyMapNote)
    @PostMapping("")
    fun addMyMap(
        @RequestBody
        request: MyMapRequest,

        @ApiIgnore
        @MsgUser
        user: User
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.addMyMap(request = request, user = user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "마이맛집 지도 리스트 불러오기", response = JSONResponse::class, notes = MyMapNotes.getMyMapListNote)
    @GetMapping("")
    fun getMyMapList(
        @ApiIgnore
        @MsgUser
        user: User
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.getMyMapList(user = user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "마이맛집 지도 수정하기", response = JSONResponse::class, notes = MyMapNotes.editMyMapNote)
    @PatchMapping("/{myMapId}")
    fun editMyMap(
        @ApiIgnore
        @MsgUser
        user: User,

        @RequestBody
        request: MyMapRequest,

        @PathVariable(value = "myMapId")
        myMapId: Long,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.editMyMap(request = request, id = myMapId, user = user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "맛집지도에 식당 추가하기", response = JSONResponse::class)
    @PostMapping("/{myMapId}/pid/{pid}")
    fun addRestaurantToMyMap(
        @ApiIgnore
        @MsgUser
        user: User,
        @PathVariable(value = "myMapId")
        myMapId: Long,
        @PathVariable(value = "pid")
        pid: Long
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.addRestaurantToMyMap(user = user, mapId = myMapId, pid = pid)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }
}

class MyMapNotes{
    companion object {
        const val addMyMapNote =
            "<h1>회원의 새로운 맛집 지도를 만듭니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON 타입의 Request Body를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|mapName|string|맛집 지도의 대표 이름을 설정합니다.|\n" +
            "|iconUrl|string|커스텀 아이콘 이미지파일의 url 경로입니다.|\n"

        const val getMyMapListNote =
            "<h1>회원의 맛집 지도 리스트를 불러옵니다. </h1>"

        const val editMyMapNote =
            "<h1>회원의 기존 맛집 지도를 수정합니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로, 수정하려는 맛집지도 ID 를 받습니다.__</br>" +
            "__본 API 는 JSON 타입의 Request Body를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|mapName|string|변경하고자 하는 맛집 지도의 이름을 설정합니다.|\n" +
            "|iconUrl|string|변경하고자 하는 커스텀 아이콘 이미지파일의 url 경로입니다.|\n"
    }
}