package io.amona.twinkorea.controller

import io.amona.twinkorea.request.RestaurantPageRequest
import io.amona.twinkorea.request.RestaurantRequest
import io.amona.twinkorea.service.RestaurantService
import io.amona.twinkorea.transformer.JSONResponse
import io.amona.twinkorea.transformer.ResponseTransformer
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Api(description = "맛집 관련 식신 연계 API")
@RequestMapping("/restaurant")
@RestController
class RestaurantContoller(private val restaurantService: RestaurantService) {
    @ApiOperation(value = "대지역의 하위 지역 ID 리턴", response = JSONResponse::class, notes = RestaurantNotes.getAreaIdNote)
    @GetMapping("/category")
    fun getAreaId(
        @RequestParam(name = "upHpArea", required = true)
        upHpArea: Int,
        @RequestParam(name = "isForeign", required = false, defaultValue = "N")
        isForeign: String? = "N"
    ): ResponseEntity<JSONResponse> {
        // 강남 9 강북 10 경기 2 인천 12 부산 8 대구 6 광주 5 대전 7 울산 11 강원 1 경남 3 경북 4 전남 13 전북 14
        // 충남 16 충북 17 제주 15 북한 1072
        return try {
            val result = restaurantService.getAreaId(upHpAreaId = upHpArea, isForeign = isForeign)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "조건별 맛집 검색", response = JSONResponse::class, notes = RestaurantNotes.getRestaurantListNote)
    @GetMapping("/list")
    fun getRestaurantList(
        request: RestaurantRequest, pageRequest: RestaurantPageRequest
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = restaurantService.getRestaurantList(request = request, pageRequest = pageRequest)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "장소 정보 불러오기", response = JSONResponse::class, notes = RestaurantNotes.getRestaurantInfoNote)
    @GetMapping("/{pid}")
    fun getRestaurantInfo(@PathVariable("pid") pid: Long): ResponseEntity<JSONResponse> {
        return try {
            val result = restaurantService.getRestaurantInfo(pid = pid)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "장소의 이미지 리스트 불러오기", response = JSONResponse::class, notes = RestaurantNotes.getRestaurantImageListNote)
    @GetMapping("{pid}/image")
    fun getRestaurantImageList(@PathVariable("pid") pid: Long): ResponseEntity<JSONResponse> {
        return try {
            val result = restaurantService.getRestaurantImageList(pid = pid)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "장소의 리뷰 리스트 불러오기", response = JSONResponse::class, notes = RestaurantNotes.getRestaurantReviewListNote)
    @GetMapping("{pid}/review")
    fun getRestaurantReviewList(@PathVariable("pid") pid: Long, pageRequest: RestaurantPageRequest): ResponseEntity<JSONResponse> {
        return try {
            val result = restaurantService.getRestaurantReviewList(pid = pid, pageRequest = pageRequest)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "장소의 저장사용자 리스트 불러오기", response = JSONResponse::class, notes = RestaurantNotes.getRestaurantBookmarkUserListNote)
    @GetMapping("{pid}/bookmark")
    fun getRestaurantBookmarkUserList(@PathVariable("pid") pid: Long): ResponseEntity<JSONResponse> {
        return try {
            val result = restaurantService.getRestaurantBookmarkList(pid = pid)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }
}

class RestaurantNotes {
    companion object {
        const val getAreaIdNote =
            "<h1>대지역의 하위 지역 ID를 반환합니다.</h1>" +
            "대지역은 아래의 번호로 구분됩니다. </br>" +
            "강남 9 강북 10 경기 2 인천 12 부산 8 대구 6 광주 5 대전 7 울산 11 강원 1 경남 3 경북 4 전남 13 전북 14 </br>" +
            "충남 16 충북 17 제주 15 북한 1072 </br>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Query Parameter 로 대지역번호(upHpArea)와 해외 지역 여부(isForeign *현재 사용 X)를 받습니다. (ex: upHpArea=4)__"

        const val getRestaurantListNote =
            "<h1>조건 필터를 지정하여 해당하는 식당들의 리스트를 불러옵니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Query Parameter 로  타입의 Request Body를 받습니다.__\n" +
            "|파라미터 명|타입|내용|\n" +
            "|--------|---|---|\n" +
            "|upAreaId(필수)|__integer__|식당을 검색할 대 지역 번호입니다.</br>" +
                    "*(ex. upHpArea=9)* -> 강남에 있는 식당으로 검색 조건 설정\n" +
            "|page(필수)|__integer__|검색을 시작할 페이지 번호입니다. __0번부터 시작합니다.__ </br>" +
                    "*(ex. page=0)*\n" +
            "|limit|__integer__|하나의 페이지에 요청할 검색 결과 갯수입니다. 기본값은 20입니다.</br> " +
                    "*(ex. limit=30)*\n" +
            "|areaId|__integer__|대지역의 하위 지역 번호입니다. 하위 지역 번호는 /v1/restaurant/category API에 요청해 확인할 수 있습니다.</br> " +
                    "*(ex. upHpAreaId=9&areaId=34)* -> 강남 지역(upHpAreaId=9)의 하위지역인 강남역 언덕길(areaId=34)의 맛집으로 검색 조건 설정\n" +
            "|lat|__float__|현재 유저 위치의 위도입니다. 현재 위치 기준 검색이 필요할 때 사용합니다.</br> " +
                    "*(ex. lat=37.525011)*\n" +
            "|lng|__float__|현재 유저 위치의 경도입니다. 현재 위치 기준 검색이 필요할 때 사용합니다.</br>" +
                    "*(ex. lng=127.036188)*\n" +
            "|dist|__integer__|현재 유저 위치에서 검색할 식당의 범위를 meter 로 나타낸값입니다. lat, lng 가 입력된 경우에만 작동합니다.</br>" +
                    "*(ex. lat=37.525011&lng=127.036188&dist=1000)* -> 위경도 37.525011,127.036188 에서 1km 이내의 식당으로 검색 조건 설정\n" +
            "|maxLat|__float__|검색할 지역의 최대 위도입니다. 유저가 지도를 확대/축소/스크롤 할때 검색범위를 표시되는 지도의 우상단 꼭지점으로 제한하기 위해 사용합니다.</br>" +
                    "*(ex. maxLat=37.525011&maxLng=127.036188&minLat=37.495011&minLng=127.006188)* -> 정해진 지도 범위 내의 식당만 검색\n" +
            "|manLng|__float__|검색할 지역의 최대 경도입니다. 유저가 지도를 확대/축소/스크롤 할때 검색범위를 표시되는 지도의 우상단 꼭지점으로 제한하기 위해 사용합니다.</br>" +
                    "*(ex. maxLat=37.525011&maxLng=127.036188&minLat=37.495011&minLng=127.006188)* -> 정해진 지도 범위 내의 식당만 검색\n" +
            "|minLat|__float__|검색할 지역의 최소 위도입니다. 유저가 지도를 확대/축소/스크롤 할때 검색범위를 표시되는 지도의 좌하단 꼭지점으로 제한하기 위해 사용합니다.</br>" +
                    "*(ex. maxLat=37.525011&maxLng=127.036188&minLat=37.495011&minLng=127.006188)* -> 정해진 지도 범위 내의 식당만 검색\n" +
            "|minLng|__float__|검색할 지역의 최소 경도입니다. 유저가 지도를 확대/축소/스크롤 할때 검색범위를 표시되는 지도의 좌하단 꼭지점으로 제한하기 위해 사용합니다.</br>" +
                    "*(ex. maxLat=37.525011&maxLng=127.036188&minLat=37.495011&minLng=127.006188)* -> 정해진 지도 범위 내의 식당만 검색\n" +
            "|restaurantCategory|__string__|검색할 식당의 카테고리입니다. 여러개를 선택할 경우 코드값을 콤마로 구분합니다. </br>" +
                    "1:양식/레스토랑 2:카페/디저트 3:한식 4:고기/구이류 5:일식/중식/세계음식 6:나이트라이프 </br>" +
                    "*(ex. restaurantCategory=1,2,3)* -> 양식, 디저트, 한식 카테고리의 식당만 제한하여 검색 __(OR 조건)__\n" +
            "|serviceCode|__string__|검색할 식당의 제공 서비스 종류 입니다. 여러개를 선택할 경우 코드값을 콤마로 구분합니다. </br>" +
                    "00001:예약 00002:포장 00003:유아시설 00004:애완동물출입 00005:휠체어접근 00006:WI-FI </br>" +
                    "00007:주차 00008:발렛 00009:근처주차장 00010:콜키지 00011:야외좌석 00012:룸 00013:노키즈존 </br>" +
                    "*(ex. serviceCode=00001,00002,00003) -> 예약, 포장, 유아시설 서비스가 있는 식당 검색 __(OR 조건)__\n" +
            "|sort|__string__|정렬 조건을 선택합니다. 기본값은 P 입니다. P: 평점순 V: 조회순 D: 거리순 R: 리뷰순 </br>" +
                    "*(ex. sort=P)* -> 평점이 높은 식당 순서대로 리스트를 받음\n" +
            "|themeCode|__string__|식당의 테마 코드입니다. 여러개를 선택할 경우 코드값을 콤마로 구분합니다. </br>" +
                    "00001:외식 00002:데이트 00003:접대 00004:친구 00005:회식 00006:모임 </br>" +
                    "00007:기념일 00008:혼자 00009:외국손님 00010:가족 00011:아이동반 </br>" +
                    "*(ex. themeCode=00001,00002,00003)* -> 외식, 데이트, 접대 테마의 식당 검색 __(OR 조건)__\n"

        const val getRestaurantInfoNote =
            "<h1>식당 정보를 불러옵니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 식당의 pid 를 받습니다.__"

        const val getRestaurantImageListNote =
            "<h1>장소의 이미지 리스트를 불러옵니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 식당의 pid 를 받습니다.__"

        const val getRestaurantReviewListNote =
            "<h1>장소의 리뷰 리스트를 불러옵니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 식당의 pid 를 받습니다.__"

        const val getRestaurantBookmarkUserListNote =
            "<h1>장소를 즐겨찾기한 사용자 리스트를 불러옵니다.</h1>" +
            "<h3>파라미터 설명</h3>" +
            "__본 API 는 Path Parameter 로 식당의 pid 를 받습니다.__"
    }
}