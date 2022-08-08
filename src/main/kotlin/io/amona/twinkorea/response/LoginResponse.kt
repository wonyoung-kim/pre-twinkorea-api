package io.amona.twinkorea.response

import io.amona.twinkorea.domain.User
import io.swagger.annotations.ApiModelProperty

data class LoginResponse (
    @ApiModelProperty(value="JWT Token")
    val jwt: String,

    @ApiModelProperty(value="user")
    val user: User
    )

data class DeactivateResponse (
    val success: Boolean,
    val user: User,
        )