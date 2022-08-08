package io.amona.twinkorea.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import io.amona.twinkorea.enums.PreOrderPopUp
import org.hibernate.Hibernate
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name="users")
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "sns_provide")
    val snsProvider: String? = null,

    @Column(name = "sns_id", unique = true)
    val snsId: String = "LOCAL",

    @Column(name = "email", unique = true)
    val email: String = "",

    @Column(name = "phone_number")
    val phoneNumber: String = "",

    @JsonIgnore
    @Column(name = "pw")
    val pw: String? = null,

    @Column(name = "nickname")
    val nickname: String = "",

    @Column(name = "coupon_count")
    val couponCount: Long = 0,

    @Column(name = "inviting_count")
    val invitingCount: Long = 0,

    @Column(name = "referral_code")
    val referralCode: String = "",

    @Column(name = "bookmark_count")
    val bookmarkCount: Long = 0,

    @Column(name = "otp_secret_key")
    val otpSecretKey: String? = null,

    @Column(name = "admin")
    val admin: Boolean = false,

    // 2022.01.10 추가된 컬럼으로 기존 유저 데이터와 호환을 위해 Nullable 컬럼으로 생성
    @Column(name = "deactivate")
    val deactivate: Boolean = false,

    @Column(name = "restaurant_map_count")
    val restaurantMapCount: Long = 0,

    @Column(name = "refresh_token")
    val refreshToken: String = "",

    @Column(name = "pre_order_popup")
    val preOrderPopup: PreOrderPopUp? = null,

    @Column(name = "pre_contract_coupon_count")
    val preContractCouponCount: Long = 0,

    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
    ): UserDetails {
    @JsonIgnore
    override fun getUsername() = snsId

    @JsonIgnore
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        val authorities = ArrayList<GrantedAuthority>()

//        val adminRole = SimpleGrantedAuthority(UserRole.ROLE_ADMIN.name)
//        val userRole = SimpleGrantedAuthority(UserRole.ROLE_USER.name)
//        val anonyRole = SimpleGrantedAuthority(UserRole.ROLE_ANONYMOUS.name)
//
//        when(role) {
//            UserRole.ROLE_ADMIN -> {
//                authorities.addAll(arrayListOf(adminRole, userRole))
//            }
//            UserRole.ROLE_USER -> {
//                authorities.add(userRole)
//            }
//            UserRole.ROLE_ANONYMOUS -> {
//                authorities.add(anonyRole)
//            }
//        }
        return authorities
    }

    override fun getPassword() = pw

    @JsonIgnore
    override fun isEnabled(): Boolean = true
    @JsonIgnore
    override fun isCredentialsNonExpired() = true
    @JsonIgnore
    override fun isAccountNonExpired() = true
    @JsonIgnore
    override fun isAccountNonLocked() = true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as User

        return id == other.id
    }

    override fun hashCode(): Int = 0

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id )"
    }
}