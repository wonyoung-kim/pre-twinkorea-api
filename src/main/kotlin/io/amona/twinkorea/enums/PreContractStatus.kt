package io.amona.twinkorea.enums

enum class PreContractStatus(val displayName: String) {
    PURCHASABLE("구매가능"),
    PURCHASED("구매중"),
    OWNED("보유중"),
    RESERVED("구매제한"),
}

enum class PreContractAvailable(val displayName: String) {
    PREORDER("사전청약"),
    WAITING("대기청약"),
    NOBODY("분양중단")
}