package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.PreOrder
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PreOrderRepository: JpaRepository<PreOrder, Long>, JpaSpecificationExecutor<PreOrder> {
    fun findById(id: Long?): PreOrder?

    fun findByAreaId(areaId: Long): PreOrder?

    @Query("SELECT p FROM PreOrder p WHERE p.areaId = :areaId")
    fun findByAreaIdNoneRO(areaId: Long): PreOrder?

    override fun findAll(pageRequest: Pageable): Page<PreOrder>

    // 전체
    fun findAllByOrderByDoneAscRatioDescApplyCountDescLimitAsc(pageRequest: Pageable): Page<PreOrder>

    // 마감된 것만
    fun findAllByDoneIsTrueOrderByApplyCountDescLimitAsc(pageRequest: Pageable): Page<PreOrder>

    // 판매중인 것만
    fun findAllByDoneIsFalseOrderByRatioDescApplyCountDescLimitAsc(pageRequest: Pageable): Page<PreOrder>
}