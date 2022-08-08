package io.amona.twinkorea.dtos

import io.amona.twinkorea.enums.PreContractStatus
import io.amona.twinkorea.enums.CellType


data class MultiCellDetailDto(
    val cellDetailList: MutableList<CellDetailDto>,
    val cellCount: Long,
    val totalPrice: Long,
    val discount: Boolean? = null,
    val discountedPrice: Long? = null,
)

data class CellDetailDto(
    val cellId: Long,
    val cellType: CellType,
    val areaId: Long,
    val leftTop: String,
    val rightTop: String,
    val leftBottom: String,
    val rightBottom: String,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val centerCity: String,
    val price: Long,
    val onPayment: Boolean?,
    val discount: Boolean? = null,
    val discountedPrice: Long? = null,
    )

data class CellDtoForCellList(
    val cellId: Long,
    val areaId: Long,
    val status: PreContractStatus,
    val centerX: Double,
    val centerY: Double,
)

data class CellOnPaymentStatusDto(
    val cellId: Long,
    val onPayment: Boolean,
)
