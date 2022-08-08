package io.amona.twinkorea.response

import io.amona.twinkorea.dtos.MyPreOrderListDto

class PageResponse(
    val last: Boolean,
    val totalPages: Long,
    val totalElement: Long,
    val size: Long,
    val number: Long,
    val numberOfElements: Long,
    val first: Boolean,
    val empty: Boolean,
    val content: List<SiksinApiResponseBase>
)

open class SiksinApiResponseBase()

class MyPreOrderPageResponse(
    val last: Boolean,
    val totalPages: Long,
    val totalElement: Long,
    val size: Long,
    val number: Long,
    val numberOfElements: Long,
    val first: Boolean,
    val empty: Boolean,
    val content: List<MyPreOrderListDto>
)