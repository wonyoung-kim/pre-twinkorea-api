package io.amona.twinkorea.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime
import javax.persistence.*


@Entity
@Table(name = "contracts", indexes = [Index(columnList = "tr_no")])
data class Contract(
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

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "pre_order_id")
    val preOrder: PreOrder,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "user_id")
    val user: User,

    @ApiModelProperty(value="생성시간")
    @Column(name = "created_at")
    val createdAt: LocalDateTime,

    @ApiModelProperty(value="업데이트 시간")
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime,
)

@Entity
@Table(name="contract_cells")
data class ContractCell(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    val contract: Contract,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cell_id")
    val cell: Cell,

    @Column(name = "created_at")
    val createdAt: LocalDateTime,

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime
)