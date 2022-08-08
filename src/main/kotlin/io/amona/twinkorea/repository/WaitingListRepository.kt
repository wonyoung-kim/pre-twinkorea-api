package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.PreOrder
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.domain.WaitingList
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface WaitingListRepository: JpaRepository<WaitingList, Long>, JpaSpecificationExecutor<WaitingList> {
    fun findByUserAndPreOrder(user: User, preOrder: PreOrder): WaitingList?

    fun countAllByCreatedAtBefore(createdAt: LocalDateTime): Long

    fun countAllByUser(user: User): Long

    fun findAllByUserOrderByCreatedAtDesc(user: User): MutableList<WaitingList>

    fun findByUserAndPreOrderId(user: User, id: Long): WaitingList?

    fun findAllByUserAndPreOrder_AreaIdOrderByCreatedAtDesc(user: User, areaId: Long): MutableList<WaitingList>
}
