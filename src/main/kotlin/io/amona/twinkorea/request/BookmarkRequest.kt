package io.amona.twinkorea.request

import io.amona.twinkorea.enums.GroupColor

data class BookmarkRequest (
    val groupName: String,
    val groupColor: GroupColor,
    val iconUrl: String? = null,
    )