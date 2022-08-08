package io.amona.twinkorea.dtos

import com.querydsl.core.annotations.QueryProjection
import io.amona.twinkorea.enums.OfferStatus

data class LandDto
@QueryProjection
constructor(
    val id: Long,
    val district: String,
    val cellCount: Long,
    val priceNearby: Double?,
    val pricePerCell: Long,
    val leftTop: String,
    val rightBottom: String,
    val offerStatus: OfferStatus? = null,
)

