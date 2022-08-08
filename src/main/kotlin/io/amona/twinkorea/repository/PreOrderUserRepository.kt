package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.PreOrderUser
import io.amona.twinkorea.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PreOrderUserRepository: JpaRepository<PreOrderUser, Long>, JpaSpecificationExecutor<PreOrderUser> {
    // 최신순 정렬
    fun findAllByUserOrderByCreatedAtDesc(user: User, pageRequest: Pageable): Page<PreOrderUser>

    // 회원의 서울지역에 신청한 사전청약건 모두 조회
    @Query("SELECT p FROM PreOrderUser p WHERE p.preOrder.id < 630 and p.user = :user ORDER BY p.createdAt desc")
    fun findAllByUserAndPreOrderIsInSeoul(@Param(value = "user") user: User, pageRequest: Pageable): Page<PreOrderUser>

    // 특정 건을 제외한 회원의 서울지역에 신청한 사전청약건 모두 조회
    @Query("SELECT p FROM PreOrderUser p WHERE p.preOrder.areaId not in :areaId and p.preOrder.id < 630 and p.user = :user  ORDER BY p.createdAt desc")
    fun findAllByUserAndPreOrderIsInSeoul(@Param(value = "user") user: User, @Param(value = "areaId") areaId: Long?, pageRequest: Pageable): Page<PreOrderUser>

    // 사전청약 리스트 보기와 같은 조건 (청약률 높은순 -> 신청자 많은순 -> 리밋 적은순)
    @Query("SELECT p FROM PreOrderUser p WHERE p.user.id = :userId ORDER BY p.preOrder.ratio desc, p.preOrder.applyCount desc, p.preOrder.limit asc")
    fun findAllByUserOrderByRatioDescApplyCountDescLimitAsc(@Param(value = "userId") userId: Long, pageRequest: Pageable): Page<PreOrderUser>

    fun findByUserAndPreOrderId(user: User, id: Long): PreOrderUser?

    fun countAllByUser(user: User): Long

    fun findAllByUserOrderByCreatedAtDesc(user: User): MutableList<PreOrderUser>

    fun findAllByUserAndPreOrder_AreaIdOrderByCreatedAtDesc(user: User, areaId: Long): MutableList<PreOrderUser>

    @Query("SELECT pu FROM PreOrderUser pu WHERE pu.user = :user and pu.preOrder.id = :id")
    fun findByUserAndPreOrderIdNoneRo(user: User, id: Long): PreOrderUser?

    @Query("SELECT pu FROM PreOrderUser pu WHERE pu.preOrder.id = 0")
    fun findNone(pageRequest: Pageable): Page<PreOrderUser>
}
