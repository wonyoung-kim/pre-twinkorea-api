package io.amona.twinkorea.request

import org.springframework.data.domain.Sort
import java.time.LocalDateTime

/**
 * 거래 검색 모델
 */
data class OfferSearchRequest(
    val addressOne: String? = null,
    val addressTwo: String? = null,
    val addressThree: String? = null,
    val text: String? = null
)

/**
 * 회원 검색 모델
 */
data class UserSearchRequest(
    val nickname: String? = null,
    val email: String? = null
)

/**
 * 결제 내역 검색 모델
 */
data class PaymentLogSearchRequest(
    val trNo: String? = null,
    val userEmail:  String? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null
)

/**
 * 폴리곤 정보 및 포함 셀 데이터 검색 모델
 */
data class PolygonCellDataSearchRequest(
    val district: String? = null,
    val name: String? = null,
    val sort: String? = null,
)

data class PageRequest(
    private var page: Int,
    private var size: Int = 50,
    private var direction: Sort.Direction = Sort.Direction.DESC
) {
    fun of(): org.springframework.data.domain.PageRequest {
        return org.springframework.data.domain.PageRequest.of(page, size, direction, "createdAt")
    }
}
