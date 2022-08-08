package io.amona.twinkorea.dtos

import io.amona.twinkorea.enums.PreOrderPopUp
import io.amona.twinkorea.enums.PreOrderStatus
import java.time.LocalDateTime


data class PreOrderDto(
    val id: Long,
    val areaId: Long,
    val district: String,
    val applyCount: Long,
    val waitingCount: Long,
    val limit: Long,
    val done: Boolean,
    val soldOut: Boolean = false,
    val ratio: Double,
    val status: String?,
    val purchaseRatio: Double = 0.00,
    val preOrderStatus: PreOrderStatus,
)

data class InvitingRankingDto(
    val ranking: Long,
    val invitingCount: Long,
    val email: String
)

data class MyInvitingInfoDto(
    val applyCount: Long,
    val waitingCount: Long,
    val couponCount: Long,
    val invitingCount: Long,
    val purchasableCount: Long,
    val popUp: PreOrderPopUp?,
)

data class MyPreOrderListDto(
    var id: Long,
    val preOrderId: Long,
    val areaId: Long,
    val type: String,
    val name: String,
    val createdAt: LocalDateTime,
    val preOrderStatus: PreOrderStatus,
)

data class PolygonCellDataDto(
    val id: Long,
    val areaId: Long,
    val name: String,
    val totalCellCount: Long,
    val reservedCellCount: Long,
    val purchasableCellCount: Long,
    val purchasedCellCount: Long,
    val soldOut: Boolean = false,
)

// 네이티브 쿼리로 랭킹 주입할때 사용
interface MyInvitingRankingDto{
    val ranking: Long
    val invitingCount: Long
    val email: String
}