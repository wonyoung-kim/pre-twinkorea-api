package io.amona.twinkorea.enums

enum class PreOrderPopUp (val displayName: String) {
    NORMAL("NORMAL"),
    BENEFIT("BENEFIT"),
    LOSS("LOSS")
}

enum class PreOrderStatus(val displayName: String) {
    NONE("상태 없음"),
    OPEN("모두 분양"),
    PRECONTRACT("사전 청약 대상 분양"),
    PREORDER("사전 청약"),
    FREECOUPON("무료쿠폰 구매")
}