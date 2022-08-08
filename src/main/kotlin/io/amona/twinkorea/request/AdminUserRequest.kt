package io.amona.twinkorea.request

import io.swagger.annotations.ApiModelProperty

data class AdminUserRequest(
    @ApiModelProperty(value = "이메일")
    val email: String?,

    @ApiModelProperty(value = "닉네임")
    val nickname: String?,

    @ApiModelProperty(value = "직급")
    val adminRole: String?,

    @ApiModelProperty(value = "휴대폰번호")
    val phoneNumber: String?,

    @ApiModelProperty(value = "비밀번호")
    val pw: String?,

    @ApiModelProperty(value = "슈퍼관리자 여부")
    val superAdmin: Boolean?
)