package io.amona.twinkorea.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import io.amona.twinkorea.enums.MsgRevenueType
import org.hibernate.Hibernate
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name="points")
data class Point (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "revenue_type")
    val revenueType: MsgRevenueType = MsgRevenueType.DEPOSIT,

    @Column(name = "msg")
    val msg: Long? = null,

    @Column(name = "krw")
    val krw: Long? = null,

    @Column(name = "balance")
    val balance: Long = 0,

    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User? = null,
)