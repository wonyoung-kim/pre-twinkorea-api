package io.amona.twinkorea.request

// 내통장결제 요청 schemas
data class MyAccountPaymentRequest(
    val authNo: String,                 // 결제 인증 완료 후 세틀뱅크가 발행한 인증번호
    val ordNo: String,                  // 결제 인증 요청 시 클라이언트에서 생성한 주문 번호
    val trPrice: String,                // 최종 출금 금액
    val mercntParam1: Long,             // 유저 ID
    val mercntParam2: String,           // 할인된 셀 갯수
)


/**
 * 환불 Request
 */
data class RefundRequest(
    val refundReason: String?
)


/**
 * 클라에서 내통장 결제 인증 요청시 데이터 암호화를 위해 사용할 입력 파라미터 :
 * SHA256(상점아이디 + 주문번호 + 거래일자 + 거래시간 + 거래금액(plain 값) + 인증키)
 */
data class DataEncRequestForMyAccount (
    val ordNo: String,                  // 주문번호
    val trDay: String,                  // 거래일자
    val trTime: String,                 // 거래시간
    val trPrice: String,                // 거래금액
)
