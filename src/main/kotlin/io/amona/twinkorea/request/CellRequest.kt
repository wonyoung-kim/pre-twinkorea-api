package io.amona.twinkorea.request

import io.amona.twinkorea.annotation.StringFormatCoordinates
import io.amona.twinkorea.domain.Land
import io.amona.twinkorea.domain.User
import io.swagger.annotations.ApiModelProperty
import springfox.documentation.annotations.ApiIgnore

data class CellRequest(
    @ApiIgnore
    @ApiModelProperty(value="소유자")
    val owner: User? = null,

    @ApiIgnore
    @ApiModelProperty(value="땅")
    val land: Land? = null,
)

data class CellIdsRequest(
    @ApiModelProperty(value="콤마로 구분된 셀 아이디", example = "256044,256045,256046,256047,256048,256049,256050")
    val cellIds: String,
    val status: String? = null
)

data class MultiCellRequest(
    val cellIds: List<Long>
)