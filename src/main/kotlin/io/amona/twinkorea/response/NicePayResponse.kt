package io.amona.twinkorea.response

data class NicePayResponse (
    val resultIsSuccess: Boolean,
    val resultCode: String,
    val resultMsg: String,
    val goodsName: String,
    val cancelNum: String?,
    val cancelDate: String?,
    val cancelTime: String?,
)

data class SettleBankResponse(
    val resultIsSuccess: Boolean,
    val trNo: String? = null,
    val trPrice: String? = null,
    val ordNo: String? = null,
    val resultCd: String,
    val errCd: String,
    val resultMsg: String,
    val cancelTrNo: String? = null,    // 취소 요청시에만 사용
    val cancelOrdNo: String? = null,   // 취소 요청시에만 사용
    val cancelPrice: String? = null,   // 취소 요청시에만 사용
)