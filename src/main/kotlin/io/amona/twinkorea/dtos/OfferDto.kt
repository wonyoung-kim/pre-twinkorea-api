package io.amona.twinkorea.dtos

import com.querydsl.core.annotations.QueryProjection

data class OfferDto
@QueryProjection
constructor(
    val id: Long,
    val name: String,
    val district: String,
    val cellCount: Long,
    var priceNearby: Double?,
    val pricePerCell: Long,
    val leftTop: String,
    val rightBottom: String,
)

data class PopularOfferDto
@QueryProjection
constructor(
    val id: Long,
    val district: String,
    val siksinCount: Long,
    var estEarn: Double,
    val pricePerCell: Long,
    val cellCount: Long,
)