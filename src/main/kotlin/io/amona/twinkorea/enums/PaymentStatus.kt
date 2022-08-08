package io.amona.twinkorea.enums

enum class PaymentStatus(val displayName: String) {
    REFUNDABLE("환불 가능"),
    REFUNDED("환불 완료"),
    EXPIRED("환불 불가")
}
