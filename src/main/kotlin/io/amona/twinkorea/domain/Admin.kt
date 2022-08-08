package io.amona.twinkorea.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name="admins")
data class Admin (
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    val id: Long = 0,

    @Column(name = "super")
    val superAdmin: Boolean = false,

    @Column(name = "admin_role")
    val adminRole: String?,

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    val user: User,

    @JsonIgnore
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @JsonIgnore
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,

)