package io.amona.twinkorea.dtos

import io.amona.twinkorea.enums.GroupColor

data class BookmarkListDto (
    val id: Long,
    val groupName: String,
    val groupColor: GroupColor,
    val restaurantCounts: Long?,
)

