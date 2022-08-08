package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.Contract
import io.amona.twinkorea.domain.PreOrder
import io.amona.twinkorea.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface ContractRepository: JpaRepository<Contract, Long>, JpaSpecificationExecutor<Contract> {
    fun findByTrNo(trNo: String): Contract?

    fun findAllByUserAndRefundedIsTrue(user: User): MutableList<Contract>

    fun findByPreOrderAndRefundedIsTrueAndUser(preOrder: PreOrder, user: User): Contract?
}