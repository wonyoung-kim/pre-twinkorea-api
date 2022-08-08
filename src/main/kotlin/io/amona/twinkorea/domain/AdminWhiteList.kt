package io.amona.twinkorea.domain

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "admin_white_list")
data class AdminWhiteList(
    @Id
    @Column(name = "ip")
    val ip: String,

    @Column(name = "description")
    val description: String,

    @Column(name = "created_at")
    val createdAt: LocalDateTime,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime,
)
