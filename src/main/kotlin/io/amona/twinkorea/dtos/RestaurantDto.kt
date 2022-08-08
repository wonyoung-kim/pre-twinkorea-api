package io.amona.twinkorea.dtos

data class BookmarkUserDto(
    val id: Long,
    val nickname: String,
    val bookmarkCount: Long,
    val restaurantMapCount: Long,
    val isFollowing: Boolean,
)

data class RestaurantBookmarkDto(
    val totalUserCount: Long,
    val bookmarkUserList: MutableList<BookmarkUserDto>
)

