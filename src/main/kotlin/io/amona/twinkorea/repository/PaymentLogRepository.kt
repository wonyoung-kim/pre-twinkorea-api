package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.PaymentLog
import io.amona.twinkorea.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface PaymentLogRepository: JpaRepository<PaymentLog, String>, JpaSpecificationExecutor<PaymentLog> {
    fun findByTrNo(trNo: String?): PaymentLog?

    fun findAllByUserOrderByCreatedAtDesc(user: User, pageRequest: Pageable): Page<PaymentLog>
}