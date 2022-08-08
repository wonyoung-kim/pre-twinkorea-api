package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.AdminWhiteList
import io.amona.twinkorea.request.PageRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AdminWhiteListRepository: JpaRepository<AdminWhiteList, String>, JpaSpecificationExecutor<AdminWhiteList> {
    fun findByIp(ip: String): AdminWhiteList?

    @Query("SELECT awl FROM AdminWhiteList awl ORDER BY awl.description ASC")
    fun findAllWithPaging(pageRequest: Pageable): Page<AdminWhiteList>
}