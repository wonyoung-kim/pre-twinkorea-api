package io.amona.twinkorea.dtos

import io.amona.twinkorea.enums.PaymentMethod
import io.amona.twinkorea.enums.SnsProvider
import java.time.LocalDateTime

data class AdminInfoDto(
    val id: Long,
    val email: String,
    val nickname: String,
    val adminRole: String,
    val phoneNumber: String,
    val isSuper: Boolean,
)

data class UserInfoDto(
    val id: Long,
    val email: String,
    val nickname: String,
    val phoneNumber: String,
    val userType: String,
    val snsProvider: SnsProvider,
    val createdAt: LocalDateTime
)

data class PaymentHistoryDto(
    val authDate: LocalDateTime,
    val tid: String,
    val email: String,
    val district: String,
    val cellIds: List<Long>,
    val paymentMethod: PaymentMethod,
    val amt: String,
    val status: String,
    val cancelReason: String?,
)