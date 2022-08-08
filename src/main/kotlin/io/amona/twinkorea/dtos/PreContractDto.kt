package io.amona.twinkorea.dtos

import java.time.LocalDateTime


data class PurchasablePreContractDto(
    val areaId: Long,
    val name: String,
    val purchasableCellCount: Long,
    val totalCellCount: Long,
//    val status: CellPreContractStatus,
)

data class PurchaseHistoryDto(
    val createdAt: LocalDateTime,
    val trNo: String,
    val ordNo: String,
    val district: String,
    val cellIds: List<Long>,
    val areaId: Long,
    val refunded: Boolean?,
)