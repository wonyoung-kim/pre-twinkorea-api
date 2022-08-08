package io.amona.twinkorea.controller

import io.amona.twinkorea.annotation.MsgUser
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.request.BookmarkRequest
import io.amona.twinkorea.service.BookmarkService
import io.amona.twinkorea.transformer.JSONResponse
import io.amona.twinkorea.transformer.ResponseTransformer
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import springfox.documentation.annotations.ApiIgnore

@Api(description = "식당 그룹(즐겨찾기) 관리")
@RequestMapping("/bookmark")
@RestController
class BookmarkController(private val service: BookmarkService) {
    @ApiOperation(value = "북마크 그룹 리스트 가져오기", response = JSONResponse::class, notes = BookmarkNotes.getBookmarkGroupListNote)
    @GetMapping("")
    fun getBookmarkGroupList(
        @ApiIgnore
        @MsgUser
        user: User
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.getBookmarkGroupList(user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "새로운 북마크 그룹 만들기", response = JSONResponse::class, notes = BookmarkNotes.addNewBookmarkGroupNote)
    @PostMapping("/group")
    fun addNewBookmarkGroup(
        @RequestBody
        request: BookmarkRequest,

        @ApiIgnore
        @MsgUser
        user: User
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.addBookmarkGroup(request = request, user = user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "북마크 그룹 정보 수정", response = JSONResponse::class, notes = BookmarkNotes.editBookmarkGroupDetailNote)
    @PatchMapping("/{groupId}")
    fun editBookmarkGroupDetail(
        @PathVariable("groupId")
        groupId: Long,

        @RequestBody
        request: BookmarkRequest,

        @ApiIgnore
        @MsgUser
        user: User,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.editBookmarkGroup(groupId = groupId, user = user, request = request)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "장소를 마이맛집(기본 그룹)에 넣기", response = JSONResponse::class, notes = BookmarkNotes.addRestaurantToDefaultBookmarkNote)
    @PostMapping("/group/default/restaurant/{pid}")
    fun addRestaurantToDefaultBookmark(@PathVariable("pid")
                                       pid: Long,

                                       @ApiIgnore
                                       @MsgUser
                                       user: User
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.addRestaurantToDefaultBookmark(pid = pid, user = user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "장소를 특정 그룹에 넣기", response = JSONResponse::class, notes = BookmarkNotes.addRestaurantToBookmarkNote)
    @PostMapping("/group/{groupId}/restaurant/{pid}")
    fun addRestaurantToBookmarkGroup(@PathVariable("groupId")
                                     groupId: Long,

                                     @PathVariable("pid")
                                     pid: Long,

                                     @ApiIgnore
                                     @MsgUser
                                     user: User,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.addRestaurantToBookmarkGroup(groupId = groupId, pid = pid, user = user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "장소를 그룹에서 삭제", response = JSONResponse::class, notes = BookmarkNotes.deleteRestaurantFromBookmark)
    @DeleteMapping("/group/{groupId}/restaurant/{pid}")
    fun deleteRestaurantFromBookmark(@PathVariable("groupId")
                                     groupId: Long,

                                     @PathVariable("pid")
                                     pid: Long,

                                     @ApiIgnore
                                     @MsgUser
                                     user: User,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.deleteRestaurantFromBookmarkGroup(groupId = groupId, pid = pid, user = user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }
}

class BookmarkNotes{
    companion object {
        const val getBookmarkGroupListNote = "<h1>로그인된 회원의 북마크 그룹 (마이맛집) 리스트를 가져옵니다.</h1>"
        const val addNewBookmarkGroupNote =
            "<h1>회원의 새로운 북마크 그룹 (마이맛집)을 만듭니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 JSON 타입의 Request Body를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|groupColor|enum(DEFAULT,BLUE,ORANGE,GREEN,YELLOW,GRAY)|북마크 아이콘이 표시될 색깔을 지정합니다.|\n" +
            "|groupName|string|그룹 이름을 지정합니다.|\n" +
            "|iconUrl|string|커스텀 아이콘 이미지파일의 url 경로입니다.|\n"
        const val editBookmarkGroupDetailNote =
            "<h1>회원이 생성했던 기존 북마크 그룹의 정보를 수정합니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로, 수정하려는 북마크 ID 를 받습니다.__</br>" +
            "__본 API 는 JSON 타입의 Request Body를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|groupColor|enum(DEFAULT,BLUE,ORANGE,GREEN,YELLOW,GRAY)|북마크 아이콘이 표시될 색깔을 지정합니다.|\n" +
            "|groupName|string|그룹 이름을 지정합니다.|\n" +
            "|iconUrl|string|커스텀 아이콘 이미지파일의 url 경로입니다.|\n"

        const val addRestaurantToDefaultBookmarkNote =
            "<h1>식당(pid)을 기본 북마크 그룹 (마이맛집)에 추가합니다.</h1>" +
            "식당을 임의의 커스텀 그룹에 추가하기 위해서는, 최초 1회 기본 북마크 그룹에 등록되어야 합니다. </br>" +
            "기본 그룹에 등록된 식당만 임의 생성된 새로운 북마크 그룹 (마이맛집)에 추가할 수 있습니다." +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 추가하고자하는 대상 식당의 PID 를 받습니다.__"
        const val addRestaurantToBookmarkNote =
            "<h1>식당(pid)을 임의의 북마크 그룹 (마이맛집)에 추가합니다.</h1>" +
            "식당을 임의의 커스텀 그룹에 추가하기 위해서는, 최초 1회 기본 북마크 그룹에 등록되어야 합니다." +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로, 추가하려는 북마크 ID 와 추가하고자하는 대상 식당의 PID 를 받습니다.__"
        const val deleteRestaurantFromBookmark =
            "<h1>식당(pid)을 특정 북마크 그룹에서 삭제합니다.</h1>" +
            "기본 북마크 그룹에서 삭제시 임의의 다른 북마크 그룹에서도 삭제한 식당은 사라집니다." +
            "기본 북마크 그룹이 아닌, 유저가 임의로 생성한 북마크 그룹의 경우, 해당 북마크 그룹에서만 삭제됩니다." +
            "내 북마크 그룹에 등록된 식당이 아닌 다른 식당의 PID 를 파라미터에 입력하거나, 내 보유 북마크 그룹이 아닌 다른 북마크 그룹 ID에 요청할 경우 에러를 반환합니다." +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로, 삭제하려는 북마크 ID 와 삭제하고자하는 대상 식당의 PID 를 받습니다.__"
    }
}