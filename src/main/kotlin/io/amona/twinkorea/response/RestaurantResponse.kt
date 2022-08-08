package io.amona.twinkorea.response


class RestaurantResponse (
    val pid: Long,
    val name: String,
    val comment: String,
    val score: Double,
    val bookMarkCount: Long,
    val viewCount: Long,
    val lat: Double,
    val lng: Double,
    val isStar: Boolean = false,
    val isHot: Boolean = false,
    val isNormal: Boolean = false,
    val isMy: Boolean = false,
) : SiksinApiResponseBase()

class RestaurantInfoResponse(
    val pid: Long,
    val name: String,
    val category: String,
    val score: Double,
    val comment: String,
    val phoneNumber: String,
    val address: String,
    val address2: String,
    val openHour: MutableList<String> ,
    val menuList: MutableList<String>,
    val imageUrlList: MutableList<MutableMap<Any, Any>>,
)

class RestaurantReviewResponse(
    val writerId: Long,
    val contents: String,
    val score: Double,
    val createdAt: String,
    val imageUrlList: MutableList<String>
): SiksinApiResponseBase()