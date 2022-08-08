package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.Cell
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface CellRepository: JpaRepository<Cell, Long>, JpaSpecificationExecutor<Cell> {
    fun findById(id: Long?): Cell?

    fun findAllByCenterXBetweenAndCenterYBetween(
        centerXStart: Double, centerXEnd: Double, centerYStart: Double, centerYEnd: Double) : MutableList<Cell>

    fun findAllByAreaId(areaId: Long): MutableList<Cell>

    // 구매 가능한 셀 갯수
    fun countAllByAreaIdAndReservedIsTrue(areaId: Long): Long

    fun countAllByAreaIdAndOwnerIsNotNull(areaId: Long): Long

    // 결제 상태로 10분이상 지속된 셀
    fun findAllByOnPaymentIsTrueAndUpdatedAtIsBefore(updatedAtBefore: LocalDateTime): MutableList<Cell>

    // 구매 가능한 셀 리스트 ( 결제진행 플래그가 없고, 소유자가 없고, 구매제한상태가 아님)
    fun findAllByOnPaymentIsNullAndOwnerIsNullAndReservedIsFalseAndAreaId(areaId: Long): MutableList<Cell>

    // 구매 불가능한 셀 리스트 (결제진행 중이거나, 구매제한상태이거나 소유자가 있는데 그게 내가 아님)
    @Query("SELECT c FROM Cell c WHERE (c.onPayment = true or c.owner is not null or c.reserved = true) and c.areaId = :areaId")
    fun findAllByOnPaymentIsTrueOrOwnerIsNotNullOrReservedIsTrueAndAreaId(@Param(value = "areaId") areaId: Long): MutableList<Cell>

    @Query("SELECT c FROM Cell c WHERE c.id IN :ids")
    fun findAllByIds(@Param(value = "ids") ids: List<Long>): MutableList<Cell>
}