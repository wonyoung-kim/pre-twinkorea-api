package io.amona.twinkorea.service.external

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.amona.twinkorea.request.RestaurantPageRequest
import io.amona.twinkorea.request.RestaurantRequest
import org.locationtech.proj4j.ProjCoordinate
import org.springframework.stereotype.Service

@Service
class PlaceService(
    private val commonService: CommonService,
    private val objectMapper: ObjectMapper,
) {
    fun getSiksinInfoByRange(range: MutableMap<String, ProjCoordinate>): MutableMap<String, Long> {
        val queryParams = "minLat=${range["min"]!!.y}" +
                "&minLng=${range["min"]!!.x}" +
                "&maxLat=${range["max"]!!.y}" +
                "&maxLng=${range["max"]!!.x}"
        val response = commonService.connSiksinApiGet(params = queryParams, url = "/v1/hp", subject = "범위 검색")
        val responseObject = objectMapper.readTree(response).get("data")!!
        return getCountInfo(responseObject)
    }

    fun getAreaId(upHpAreaId: Int, isForeign: String?): JsonNode {
        val queryParams = "upHpAreaId=${upHpAreaId}"
        val response = commonService.connSiksinApiGet(
            params = queryParams, url = "/v1/hp/area", subject = "상위지역 ${upHpAreaId}의 하위 지역 카테고리 요청 검색"
        )
        return objectMapper.readTree(response).get("data")!!
    }

    fun getAreaBestOrd(upHpAreaId: Int, isForeign: String?): JsonNode {
        val queryParams = "upHpAreaId=${upHpAreaId}" +
                "&isBestOrd=Y"
        val response = commonService.connSiksinApiGet(
            params = queryParams, url = "/v1/hp", subject = "상위지역 ${upHpAreaId}의 하위 지역 BestOrd 검색"
        )
        return objectMapper.readTree(response).get("data")!!
    }

    fun getRestaurantList(request: RestaurantRequest, pageRequest: RestaurantPageRequest): JsonNode {
        var queryParams = "limit=${pageRequest.limit}&idx=${pageRequest.page*pageRequest.limit}&upHpAreaId=${request.upAreaId}"
        request.areaId?.let { queryParams += "&hpAreaId=$it" }
        request.restaurantCategory?.let { queryParams += "&hpSchCate=$it" }
        request.lat?.let { queryParams += "&lat=$it" }
        request.lng?.let { queryParams += "&lng=$it" }
        request.dist?.let { queryParams += "&dist=$it" }
        request.themeCode?.let { queryParams += "&hptUpCode=$it" }
        request.serviceCode?.let { queryParams += "&hptFaciCode=$it" }
        request.minLat?.let { queryParams += "&minLat=$it" }
        request.maxLat?.let { queryParams += "&maxLat=$it" }
        request.minLng?.let { queryParams += "&minLng=$it" }
        request.maxLng?.let { queryParams += "&maxLng=$it" }

        val response = commonService.connSiksinApiGet(
            params = queryParams, url = "/v1/hp", subject = "지역 내 맛집 검색 필터링"
        )
        return objectMapper.readTree(response).get("data")!!
    }

    fun getRestaurantInfo(pid: Long): JsonNode {
        val response = commonService.connSiksinApiGet(
            params = "", url = "/v1/hp/${pid}", subject = "장소 ID #${pid}에 대한 정보 검색"
        )
        return objectMapper.readTree(response).get("data")!!
    }

    fun getRestaurantHourInfo(pid: Long): JsonNode {
        val response = commonService.connSiksinApiGet(
            params = "", url = "/v1/hp/${pid}/oprt", subject = "장소 ID #${pid}의 운영시간 정보 검색"
        )
        return objectMapper.readTree(response).get("data")!!
    }

    fun getRestaurantMenuInfo(pid: Long): JsonNode {
        val response = commonService.connSiksinApiGet(
            params = "", url = "/v1/hp/${pid}/menu", subject = "장소 ID #${pid}의 메뉴 정보 검색"
        )
        return objectMapper.readTree(response).get("data")!!
    }

    fun getRestaurantHolidayInfo(pid: Long): JsonNode {
        val response = commonService.connSiksinApiGet(
            params = "", url = "/v1/hp/${pid}/hday", subject = "장소 ID #${pid}의 휴무 정보 검색"
        )
        return objectMapper.readTree(response).get("data")!!
    }

    fun getRestaurantImageInfo(pid: Long): JsonNode {
        val response = commonService.connSiksinApiGet(
            params = "", url = "/v1/hp/${pid}/media", subject = "장소 ID #${pid}의 이미지 정보 검색"
        )
        return objectMapper.readTree(response).get("data")!!
    }

    fun getRestaurantReviewList(pid: Long, pageRequest: RestaurantPageRequest): JsonNode {
        val queryParams = "limit=${pageRequest.limit}&idx=${pageRequest.page*pageRequest.limit}"
        val response = commonService.connSiksinApiGet(
            params = queryParams, url = "/v1/hp/${pid}/review", subject = "장소 ID #${pid}의 리뷰 정보 검색"
        )
        return objectMapper.readTree(response).get("data")!!
    }

    private fun getCountInfo(siksinInfo: JsonNode): MutableMap<String, Long> {
        val countInfo = mutableMapOf<String, Long>(
            "cellCount" to 0, "siksinStar" to 0, "siksinHot" to 0, "siksinNormal" to 0,
        )
        var starCount: Long = 0
        var hotCount: Long = 0
        var normalCount: Long = 0
        return if (siksinInfo.get("list").size() == 0) {
            countInfo
        } else {
            siksinInfo["list"].forEach {
                // TODO 기준 확인 꼭 필요, API 리스폰스에는 이 구분값 없음
                if (!it.get("bestOrd").isNull) starCount += 1
                if (it.get("score").toString() != "0.0") hotCount += 1
                if (it.get("score").toString() == "0.0") normalCount += 1
            }
            countInfo["cellCount"] = siksinInfo["list"].size().toLong()
            countInfo["siksinStar"] = starCount
            countInfo["siksinHot"] = hotCount
            countInfo["siksinNormal"] = normalCount
            countInfo
        }
    }
}