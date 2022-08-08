package io.amona.twinkorea.request

import io.swagger.annotations.ApiModelProperty

data class MyMapRequest(
    @ApiModelProperty(value="등록할 지도 이름", example = "강남역 쪽 맛집 지도")
    val mapName: String? = null,
    @ApiModelProperty(value="좌상단 좌표", example = "https://sample.com/image/1.jpg")
    val iconUrl: String? = null,
    @ApiModelProperty(value="지도의 좌상단 경계 좌표", example = "127.024494,37.496891")
    val leftTop: String? = null,
    @ApiModelProperty(value="지도의 우하단 경계 좌표", example = "127.036629,37.490919")
    val rightBottom: String? = null
)