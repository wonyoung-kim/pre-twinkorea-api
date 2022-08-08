package io.amona.twinkorea.request

import io.amona.twinkorea.domain.User
import io.amona.twinkorea.enums.MsgRevenueType

data class PointRequest (
    val pointId: Long? = null,
    val balance: Long? = null,
    val krw: Long? = null,
    val msg: Long,
    val revenueType: MsgRevenueType,
    val user: User,
)