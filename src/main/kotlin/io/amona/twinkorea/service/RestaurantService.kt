package io.amona.twinkorea.service

import com.fasterxml.jackson.databind.JsonNode
import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.dtos.BookmarkUserDto
import io.amona.twinkorea.dtos.RestaurantBookmarkDto
import io.amona.twinkorea.repository.BookmarkRestaurantRepository
import io.amona.twinkorea.request.RestaurantPageRequest
import io.amona.twinkorea.request.RestaurantRequest
import io.amona.twinkorea.response.*
import io.amona.twinkorea.service.external.PlaceService
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.*

@Suppress("IMPLICIT_CAST_TO_ANY")
@Service
class RestaurantService (private val placeService: PlaceService,
                         private val appConfig: AppConfig,
                         private val bookmarkRestaurantRepo: BookmarkRestaurantRepository,
) {
    fun getAreaId(upHpAreaId: Int, isForeign: String?): MutableList<AreaResponse> {
        val getFromSiksinApi = placeService.getAreaId(upHpAreaId, isForeign)
        val getBestFromSiksinApi = placeService.getAreaBestOrd(upHpAreaId, isForeign)
        val areaCategory = getFromSiksinApi.get("list")[0].get("list")
        val areaLat = getFromSiksinApi.get("list")[0].get("lat").doubleValue()
        val areaLng = getFromSiksinApi.get("list")[0].get("lng").doubleValue()
        val areaCategoryList: MutableList<AreaResponse> = mutableListOf()
        // 베스트 모음
        areaCategoryList.add(
            AreaResponse(
                areaId = 0,             // 베스트맛집은 지역 하위 카테고리에서 분류하지 않기 때문에, 일괄적으로 0번 값을 반환한다.
                areaTitle = "Best 맛집",
                restaurantCount = getBestFromSiksinApi.get("cnt").longValue(),
                centerLatitude = areaLat,
                centerLongitude = areaLng,
            ))

        // 일반 카테고리
        areaCategory.forEach{
            areaCategoryList.add(AreaResponse(
                areaId = it.get("hpAreaId").longValue(),
                areaTitle = it.get("hpAreaTitle").toString().replace("\"",""),
                restaurantCount = it.get("hpCnt").longValue(),
                centerLatitude = it.get("lat").doubleValue(),
                centerLongitude = it.get("lng").doubleValue(),
            ))
        }
        appConfig.logger.info{"[TWINKOREA API] 상위지역 ${upHpAreaId}의 하위 지역 카테고리 ${areaCategoryList.size}개 불러오기 완료"}
        return areaCategoryList
    }

    fun getRestaurantList(request: RestaurantRequest, pageRequest: RestaurantPageRequest): PageResponse {
        val getFromSiksinApi = placeService.getRestaurantList(request, pageRequest)
        val getContents = getFromSiksinApi.get("list")
        val getCounts = getFromSiksinApi.get("cnt")
        val restaurantList: MutableList<SiksinApiResponseBase> = mutableListOf()

        getContents.forEach {
            restaurantList.add(
                RestaurantResponse(
                    pid = it.get("pid").longValue(),
                    name = it.get("pname").toString().replace("\"",""),
                    comment = it.get("cmt").toString().replace("\"",""),
                    score = it.get("score").doubleValue(),
                    bookMarkCount = it.get("bookmarkCnt").longValue(),
                    viewCount = it.get("viewCnt").longValue(),
                    lat = it.get("lat").doubleValue(),
                    lng = it.get("lng").doubleValue(),
                    // TODO 회원 디비 관리 부분 구현 후 완성
                    isStar = false,
                    isHot = false,
                    isNormal = false,
                    isMy = false,
            )
            )
        }

        val result = pageRequest.getPageData(getCounts.longValue(), restaurantList)

        appConfig.logger.info{"[TWINKOREA API] 식당 리스트 ${restaurantList.size}개 불러오기 완료"}
        return result
    }

    fun getRestaurantInfo(pid: Long): RestaurantInfoResponse {
        val siksinInfo = placeService.getRestaurantInfo(pid = pid)
        val hourInfo = placeService.getRestaurantHourInfo(pid = pid).get("list")
        val holidayInfo = placeService.getRestaurantHolidayInfo(pid = pid).get("list")
        val menuInfo = placeService.getRestaurantMenuInfo(pid = pid).get("menu")
        val imageInfo = placeService.getRestaurantImageInfo(pid = pid).get("list")

        val openHour = openHoursWrapper(hourInfo = hourInfo, holidayInfo = holidayInfo)
        val menuList = menuListWrapper(menuInfo = menuInfo)
        val imageList = imageListWrapper(imageInfo = imageInfo)

        return RestaurantInfoResponse(
            pid = pid,
            name = siksinInfo.get("pname").toString().replace("\"", ""),
            category = siksinInfo.get("hpSchCateNm").toString().replace("\"", ""),
            score = siksinInfo.get("score").doubleValue(),
            comment = siksinInfo.get("cmt").toString().replace("\"", ""),
            phoneNumber = siksinInfo.get("phone").toString().replace("\"", ""),
            address = siksinInfo.get("addr").toString().replace("\"", ""),
            address2 = siksinInfo.get("addr2").toString().replace("\"", ""),
            openHour = openHour,
            menuList = menuList,
            imageUrlList = imageList
        )
    }

    fun getRestaurantImageList(pid: Long): MutableList<MutableMap<Any, Any>> {
        val imageInfo = placeService.getRestaurantImageInfo(pid = pid).get("list")
        return imageListWrapper(imageInfo = imageInfo)
    }

    fun getRestaurantBookmarkList(pid: Long): RestaurantBookmarkDto {
        val bookmarkUserList = getBookmarkUserList(pid = pid)
        val totalCount = bookmarkRestaurantRepo.countAllByPidAndBookmark_Default(pid = pid, bookmarkDefault = true)
        return RestaurantBookmarkDto(totalUserCount = totalCount, bookmarkUserList = bookmarkUserList)
    }

    // 369401
    fun getRestaurantReviewList(pid: Long, pageRequest: RestaurantPageRequest): PageResponse {
        val reviewList: MutableList<SiksinApiResponseBase> = mutableListOf()
        val imageHost = "https://img.siksinhot.com/story/"
        val format = SimpleDateFormat("yyyy.MM.dd HH:mm")

        val siksinReview = placeService.getRestaurantReviewList(pid = pid, pageRequest = pageRequest)
        val getList = siksinReview.get("list")
        val getCounts = siksinReview.get("cnt")

        getList.forEach { it ->
            val imageUrlList: MutableList<String> = mutableListOf()
            val writerId = it.get("writeUser").get("uid")
            val contents = it.get("storyContents").toString().replace("\"","")
            val score = it.get("score")
            val createdAt = it.get("writeDt").longValue()
            it.get("photo").forEach {
                imageUrlList.add(
                    "${imageHost}${it.get("imgNm").toString().replace("\"","")}"
                )
            }
            reviewList.add(
                RestaurantReviewResponse(
                    writerId = writerId.longValue(),
                    contents = contents,
                    score = score.doubleValue(),
                    createdAt = format.format(Date(createdAt)),
                    imageUrlList = imageUrlList
                )
            )
        }

        val result = pageRequest.getPageData(getCounts.longValue(), reviewList)

        appConfig.logger.info{"[TWINKOREA API] 리뷰 리스트 ${reviewList.size}개 불러오기 완료"}
        return result
    }

    private fun openHoursWrapper(hourInfo: JsonNode, holidayInfo: JsonNode): MutableList<String> {
        val openHour = mutableListOf<String>()
        hourInfo.forEach {
            val weekBit = it.get("weekBit").toString().replace("\"", "")
            var workDay = ""
            var value = it.get("oprtCodeVal").toString().replace("\"", "")
            val hours = "${it.get("startTm").toString().replace("\"", "")} " +
                    "~ ${it.get("endTm").toString().replace("\"", "")}"
            if (weekBit.toList()[0] == '1') {
                workDay += "일"
            }
            if (weekBit.toList()[1] == '1') {
                workDay += "월"
            }
            if (weekBit.toList()[2] == '1') {
                workDay += "화"
            }
            if (weekBit.toList()[3] == '1') {
                workDay += "수"
            }
            if (weekBit.toList()[4] == '1') {
                workDay += "목"
            }
            if (weekBit.toList()[5] == '1') {
                workDay += "금"
            }
            if (weekBit.toList()[6] == '1') {
                workDay += "토"
            }
            when (workDay) {
                "일월화수목금토" -> workDay = "매일"
                "월화수목금" -> workDay = "월~금"
                "일토" -> workDay = "주말"
            }
            when (value) {
                "일반" -> value = "영업시간"
                "휴식" -> value = "브레이크타임"
                "런치" -> value = "런치 영업시간"
                "디너" -> value = "디너 영업시간"
                "LO 점심" -> value = "점심 라스트오더"
                "LO 저녁" -> value = "저녁 라스트오더"
            }
            openHour.add("[$value] $workDay $hours")
        }
        holidayInfo.forEach {
            val value = it.get("hdayCodeVal").toString().replace("\"","")
            val holiday = if (it.get("day").size() > 0) {
                "[$value] ${it.get("day")[0].get("dayCodeVal").toString().replace("\"", "")}"
            } else {
                "[$value]"
            }
            openHour.add(holiday)
        }
        return openHour
    }

    private fun menuListWrapper(menuInfo: JsonNode): MutableList<String> {
        val menuList = mutableListOf<String>()
        menuInfo.forEach {
            val menuName = it.get("menuNm").toString().replace("\"", "")
            val price = it.get("price")
            val currency = it.get("currencyUnit").toString().replace("\"", "")
            val menu = "$menuName $price$currency"
            menuList.add(menu)
        }
        return menuList
    }

    private fun imageListWrapper(imageInfo: JsonNode): MutableList<MutableMap<Any, Any>> {
        val imageHost = "https://img.siksinhot.com/place/"
        val imageParams = "?w=500&h=500&c=Y"
        val imageList = mutableListOf<MutableMap<Any, Any>>()
        imageInfo.forEach {
            val imageMap: MutableMap<Any, Any> = mutableMapOf()
            val from = it.get("from").toString().replace("\"","")
            val image = it.get("photo").get("imgNm").toString().replace("\"","")
            val imageUrl = "${imageHost}${image}${imageParams}"
            imageMap["from"] = from
            imageMap["imageUrl"] = imageUrl
            imageList.add(imageMap)
        }
        return imageList
    }

    private fun getBookmarkUserList(pid: Long): MutableList<BookmarkUserDto> {
        val returnList: MutableList<BookmarkUserDto> = mutableListOf()
        val userList = bookmarkRestaurantRepo.findAllByPidAndBookmark_Default(pid = pid, bookmarkDefault = true)
        userList.forEach {
            val user = it.bookmark.user
            returnList.add(
                BookmarkUserDto(
                    id = user.id,
                    nickname = user.nickname,
                    bookmarkCount = user.bookmarkCount,
                    restaurantMapCount = user.restaurantMapCount,
                    // TODO
                    isFollowing = false
                )
            )
        }
        return returnList
    }
}