package io.amona.twinkorea.request

import io.amona.twinkorea.enums.SnsProvider
import io.swagger.annotations.ApiModelProperty

data class UserRequest(
    @ApiModelProperty(value="SNS 인증 제공업체", example = "S")
    val snsProvider: SnsProvider? = null,

    @ApiModelProperty(value="SNS 인증 코드")
    val snsKey: String? = null,

    @ApiModelProperty(value="이메일", example = "sample@gmail.com")
    val email: String? = null,

    @ApiModelProperty(value="비밀번호", example = "thisispassword123!@#")
    var pw: String? = null,

    @ApiModelProperty(value="닉네임", example = "thisisnickname")
    val nickname: String? = null,

    @ApiModelProperty(value="전화번호", example = "010-1111-2222")
    val phoneNumber: String? = null,

    @ApiModelProperty(value="추천인 코드", example = "kTMYz561237")
    val referralCode: String? = null,

    @ApiModelProperty(value = "Google OTP 코드")
    val otpCode: String? = null,
)