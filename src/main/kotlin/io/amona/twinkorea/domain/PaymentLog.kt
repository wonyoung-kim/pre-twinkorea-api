package io.amona.twinkorea.domain

import io.amona.twinkorea.enums.PaymentMethod
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "payment_logs")       // 로그 기록용으로만 사용 / 실제 비즈니스 로직 처리는 pre-contract / contract 에서 처리
data class PaymentLog(
    @Id
    @Column(name = "tr_no")
    val trNo: String,               // 결제번호

    @Column(name = "ord_no")
    val ordNo: String,              // 주문번호 (상점에서 요청시 생성한 주문번호)

    @Enumerated(EnumType.STRING)
    @Column(name = "method")
    val method: PaymentMethod,      // 결제 방법

    @Column(name = "tr_price")
    val trPrice: String,            // 결제 금액

    @Column(name = "created_at")
    val createdAt: LocalDateTime,   // 결제 시간

    @Column(name = "canceled_at")
    val canceledAt: LocalDateTime? = null,

    @Column(name = "cancel_reason")
    val cancelReason: String? = null,

    @Column(name = "cancel_tr_no")
    val cancelTrNum: String? = null, // 취소시 생성된 거래 번호

    @Column(name = "cellIds")
    val cellIds: String? = null,

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cell_id")
    val cell: Cell? = null,

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cancel_admin_id")
    val cancelAdmin: Admin? = null,

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    val user: User,

)
