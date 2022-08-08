package io.amona.twinkorea.dtos

import io.amona.twinkorea.enums.PreOrderPopUp

data class UserReferralDto(
    val couponCount: Long,
    val referralCode: String,
)

data class MyInfoDto(
    val email: String,
    val nickname: String,
    val phoneNumber: String,
    val popUp: PreOrderPopUp?
)
