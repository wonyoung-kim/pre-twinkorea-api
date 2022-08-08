package io.amona.twinkorea.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name="pre_contracts")
data class PreContract(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "tr_no")
    val trNo: String,

    @Column(name = "ord_no")
    val ordNo: String,

    @Column(name = "refunded")
    val refunded: Boolean? = null,

    @OneToOne(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JoinColumn(name = "pre_order_user_id")
    val preOrderUser: PreOrderUser? = null,

    @OneToOne(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JoinColumn(name = "waiting_list_id")
    val waitingList: WaitingList? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "cell_id", unique = true)
    val cell: Cell,

    @ApiModelProperty(value="생성시간")
    @Column(name = "created_at")
    val createdAt: LocalDateTime,

    @ApiModelProperty(value="업데이트 시간")
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime,
)
