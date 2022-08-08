package io.amona.twinkorea.request

import io.amona.twinkorea.response.PageResponse
import io.amona.twinkorea.response.SiksinApiResponseBase
import io.swagger.annotations.ApiModelProperty
import kotlin.math.ceil


data class RestaurantPageRequest(
    @ApiModelProperty(value = "페이지", required = true, example = "0")
    var page: Long,

    @ApiModelProperty(value = "페이지당 갯수", example = "20")
    var limit: Long = 50,
) {
        fun getPageData(counts: Long, content: MutableList<SiksinApiResponseBase>): PageResponse {
            val page = this.page
            val size = this.limit
            val totalPages = ceil((counts.toDouble() / size.toDouble())).toLong()
            val numberOfElements = content.size.toLong()

            return PageResponse(
                content = content,
                totalPages = totalPages,
                totalElement = counts,
                size = size,
                numberOfElements = numberOfElements,
                number = page,
                empty = (numberOfElements == 0L),
                last = (page + 1L == totalPages),
                first = (page == 0L)
            )
        }
}


data class RestaurantRequest (
    @ApiModelProperty(value = "대 지역 구분", required = true)
    val upAreaId: Long = 9,

    val areaId: Long?,
    val themeCode: String?,
    val serviceCode: String?,
    val restaurantCategory: String?,
    val sort: String = "P",
    val lat: Double?,
    val lng: Double?,
    val dist: Long?,
    val minLat: Double?,
    val maxLat: Double?,
    val minLng: Double?,
    val maxLng: Double?,
)

