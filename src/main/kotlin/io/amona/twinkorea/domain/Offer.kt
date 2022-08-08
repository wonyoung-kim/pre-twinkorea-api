package io.amona.twinkorea.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import io.amona.twinkorea.enums.OfferStatus
import org.hibernate.Hibernate
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name="offers")
data class Offer (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = 0,

    @Column(name = "name")
    val name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    val status: OfferStatus = OfferStatus.PENDING,

    @Column(name = "district")
    val district: String? = null,

    @JsonIgnore
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @JsonIgnore
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    val buyer: User? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    val seller: User? = null,

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "land_id")
    val land: Land? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Offer

        return id != null && id == other.id
    }

    override fun hashCode(): Int = 0

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id )"
    }

}