package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.PreContract
import io.amona.twinkorea.domain.PreOrderUser
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.domain.WaitingList
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PreContractRepository: JpaRepository<PreContract, Long>, JpaSpecificationExecutor<PreContract> {
    fun findByUserAndPreOrderUser(user: User, preOrderUser: PreOrderUser): PreContract?

    fun findByUserAndWaitingList(user: User, waitingList: WaitingList): PreContract?

    @Query("SELECT pc.preOrderUser.id FROM PreContract pc JOIN PreOrderUser pu ON pu = pc.preOrderUser WHERE pc.user = :user and pc.refunded is null ")
    fun findAllPurchasedPreOrderUserIdByUser(@Param(value = "user") user: User): MutableList<Long>

    @Query("SELECT pc.preOrderUser.id FROM PreContract pc JOIN PreOrderUser pu ON pu = pc.preOrderUser WHERE pc.user = :user and pc.refunded = true")
    fun findAllRefundedPreOrderUserIdByUser(@Param(value = "user") user: User): MutableList<Long>

    @Query("SELECT pc.waitingList.id FROM PreContract pc JOIN WaitingList wl ON wl = pc.waitingList WHERE pc.user = :user and pc.refunded is null")
    fun findAllWaitingListIdByUser(@Param(value = "user") user: User): MutableList<Long>

    @Query("SELECT pc.waitingList.id FROM PreContract pc JOIN WaitingList wl ON wl = pc.waitingList WHERE pc.user = :user and pc.refunded = true")
    fun findAllRefundedWaitingListIdByUser(@Param(value = "user") user: User): MutableList<Long>

    @Query("SELECT pc.preOrderUser.id FROM PreContract pc JOIN PreOrderUser pu ON pu = pc.preOrderUser WHERE pc.user = :user")
    fun findAllPreOrderUserIdByUser(@Param(value = "user") user: User): MutableList<Long>

    @Query("SELECT pc.waitingList.id FROM PreContract pc JOIN WaitingList wl ON wl = pc.waitingList WHERE pc.user = :user")
    fun findAllWaitingListUserIdByUser(@Param(value = "user") user: User): MutableList<Long>

    @Query("SELECT pc FROM PreContract pc WHERE pc.user = :user and pc.refunded = true")
    fun findAllByUserAndRefundedIsTrue(@Param(value = "user") user: User): MutableList<PreContract>

    @Query("SELECT pc.preOrderUser.preOrder.id FROM PreContract pc WHERE pc.user = :user and pc.refunded = true")
    fun findAllRefundedPreOrderIdByUser(user: User): MutableList<Long>

    @Query("SELECT pc.waitingList.preOrder.id FROM PreContract pc WHERE pc.user = :user and pc.refunded = true")
    fun findAllRefundedWaitingOrderIdByUser(user: User): MutableList<Long>

    fun findByTrNo(trNo: String): PreContract?
}