package io.amona.twinkorea.dtos

data class MyMapDto(
    val id: Long,
    val mapName: String,
    val iconUrl: String? = null,
)