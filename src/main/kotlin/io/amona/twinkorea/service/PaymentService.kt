package io.amona.twinkorea.service

import io.amona.twinkorea.domain.PaymentLog
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.dtos.PurchaseHistoryDto
import io.amona.twinkorea.repository.CellRepository
import io.amona.twinkorea.repository.PaymentLogRepository
import io.amona.twinkorea.service.external.SettlebankService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class PaymentService(
    private val cellService: CellService,
    private val settlebankService: SettlebankService,
    private val paymentLogRepo: PaymentLogRepository,
    private val cellRepo: CellRepository,
) {
    /**
     * 구매 이력 확인
     * 사전분양은 -> pre-contract
     * 일반분양은 -> contract 로 각기 다른 테이블에서 관리됨에 따라, payment-log 라는 테이블을 따로 만들어서 거기서 구매이력을 조회함
     */
    fun getPurchaseHistory(user: User, pageRequest: Pageable): Page<PurchaseHistoryDto> {
        val paymentLogList = paymentLogRepo.findAllByUserOrderByCreatedAtDesc(user = user, pageRequest = pageRequest)
        return paymentLogList.map { paymentLog: PaymentLog ->
            when (paymentLog.cell != null) {
                // 사전청약 구매건 (paymentLog 와 cell 이 OneToOne)
                true -> PurchaseHistoryDto(
                    createdAt = paymentLog.createdAt,
                    trNo = paymentLog.trNo,
                    ordNo = paymentLog.ordNo,
                    district = paymentLog.cell.centerCity!!,
                    cellIds = listOf(paymentLog.cell.id),
                    areaId = paymentLog.cell.areaId!!,
                    refunded = paymentLog.cancelTrNum != null
                )
                // 일반 구매건 (paymentLog에 cell이 아니라 cellIds가 있는경우)
                false -> {
                    val firstCell = cellService.getFirstCellFromCellIds(paymentLog.cellIds)
                    println(firstCell)
                    val cellIds = paymentLog.cellIds!!.split(",").map{it.toLong()}
                    PurchaseHistoryDto(
                        createdAt = paymentLog.createdAt,
                        trNo = paymentLog.trNo,
                        ordNo = paymentLog.ordNo,
                        district = firstCell.centerCity!!,
                        cellIds = cellIds,
                        areaId = firstCell.areaId!!,
                        refunded = paymentLog.cancelTrNum != null
                    )
                }
            }
        }
    }
}