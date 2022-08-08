package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.Contract
import io.amona.twinkorea.domain.ContractCell
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface ContractCellRepository: JpaRepository<ContractCell, Long>, JpaSpecificationExecutor<ContractCell> {
    fun findAllByContract(contract: Contract): MutableList<ContractCell>
}