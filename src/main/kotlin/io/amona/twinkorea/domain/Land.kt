package io.amona.twinkorea.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.Hibernate
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name="lands")
data class Land (
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = 0,

    @Column(name = "price_per_cell")
    val pricePerCell: Long = 0,

    @Column(name = "price_nearby")
    val priceNearBy: Double? = null,

    @Column(name = "is_selling")
    val isSelling: Boolean = false,

    @Column(name = "cell_count")
    val cellCount: Long = 0,

    @Column(name = "district")
    val district: String,

    @Column(name = "left_top")
    val leftTop: String,

    @Column(name = "right_bottom")
    val rightBottom: String,

    @Column(name = "est_earn")
    val estEarn: Double? = null,

    @Column(name = "siksin_count")
    val siksinCount: Long? = null,

    @JsonIgnore
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @JsonIgnore
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,

    @JsonIgnore
    @OneToMany(mappedBy = "land", fetch = FetchType.LAZY)
    val cells: MutableList<Cell> = ArrayList(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    val owner: User? = null,

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "offer_id")
    val offer: Offer? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Land

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id )"
    }
}