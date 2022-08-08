package io.amona.twinkorea.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import io.amona.twinkorea.enums.PreOrderStatus
import org.hibernate.Hibernate
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name="pre_orders")
data class PreOrder (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "name")
    val name: String,

    @Column(name = "apply_count")
    val applyCount: Long,

    @Column(name = "waiting_count")
    val waitingCount: Long,

    @Column(name = "done")
    val done: Boolean = false,

    @Column(name = "area_id")
    val areaId: Long,

    @Column(name = "latitude")
    val latitude: Double,

    @Column(name = "longitude")
    val longitude: Double,

    @Column(name = "district")
    val district: String,

    @Column(name = "limit")
    val limit: Long,

    @Column(name = "ratio")
    val ratio: Double,

    @Column(name = "cell_count")
    val cellCount: Long = 0,

    @Column(name = "purchase_count")
    val purchaseCount: Long = 0,

    @Column(name = "reserved_cell_count")
    val reservedCellCount: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    val status: PreOrderStatus = PreOrderStatus.NONE,

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "preOrder")
    @OrderBy(value = "updated_at")
    val preOrderUsers: MutableList<PreOrderUser> = ArrayList(),

    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
        ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as PreOrder

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id )"
    }
}

@Entity
@Table(name="pre_oder_users")
data class PreOrderUser (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_order_id")
    val preOrder: PreOrder,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @JsonIgnore
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @JsonIgnore
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,

    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as PreOrderUser

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id )"
    }
}

@Entity
@Table(name="canceld_list")
data class CanceledList (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_order_id")
    val preOrder: PreOrder,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @JsonIgnore
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @JsonIgnore
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)

@Entity
@Table(name="waiting_list")
data class WaitingList (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_order_id")
    val preOrder: PreOrder,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @JsonIgnore
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @JsonIgnore
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)