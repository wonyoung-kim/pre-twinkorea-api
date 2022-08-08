package io.amona.twinkorea.request

import io.amona.twinkorea.dtos.MyPreOrderListDto
import io.amona.twinkorea.response.MyPreOrderPageResponse
import io.swagger.annotations.ApiModelProperty
import kotlin.math.ceil

data class MyPreOrderListPageRequest(
    @ApiModelProperty(value = "페이지", required = true, example = "0")
    var page: Long,

    @ApiModelProperty(value = "페이지당 갯수", example = "20")
    var limit: Long = 50,
) {
    fun getPageData(counts: Long, content: List<MyPreOrderListDto>): MyPreOrderPageResponse {
        val page = this.page
        val size = this.limit
        val totalPages = ceil((counts.toDouble() / size.toDouble())).toLong()
        val numberOfElements = content.size.toLong()

        return MyPreOrderPageResponse(
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

data class PreOrderRequest(
    val upHpAreaId: Long,
    val name: String? = null,
)