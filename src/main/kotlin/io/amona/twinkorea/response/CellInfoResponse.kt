package io.amona.twinkorea.response

import io.amona.twinkorea.dtos.CellDtoForCellList
import io.amona.twinkorea.dtos.CellOnPaymentStatusDto
import io.amona.twinkorea.enums.PreOrderStatus

class CellInfoResponse (
    val cellCount: Long,
    val siksinStar: Long,
    val siksinHot: Long,
    val siksinNormal: Long,
    val pricePerCell: Long,
    )

data class CellOnPaymentStatusListResponse(
    val hasOnPayment: Boolean,
    val cellList: MutableList<CellOnPaymentStatusDto>
)

data class CellListInfoByAreaIdResponse (
    val areaId: Long,
    val name: String,
    val centerLat: Double,
    val centerLong: Double,
    val totalCellCount: Long,
    val reservedCellCount: Long? = null,
    val purchasableCellCount: Long,
    val purchasedCellCount: Long,
    val cellInfoList: MutableList<CellDtoForCellList>,
    val refunded: Boolean? = null,                          // 로그인한 회원이 해당 지역에 환불을 했는지 파악
    val soldOut: Boolean = false,
    val preOrderStatus: PreOrderStatus,
//    val geoJson: JsonNode,
)