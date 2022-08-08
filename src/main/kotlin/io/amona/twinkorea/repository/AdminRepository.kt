package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.Admin
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface AdminRepository: JpaRepository<Admin, Long>, JpaSpecificationExecutor<Admin> {
    fun findByIdAndUser_DeactivateIsFalse(id: Long?): Admin?

    fun findByUserIdAndUser_DeactivateIsFalse(userId: Long): Admin?

    fun findAllByUser_DeactivateIsFalse(): MutableList<Admin>
}