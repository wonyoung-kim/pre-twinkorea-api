package io.amona.twinkorea.service

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.Contract
import io.amona.twinkorea.domain.ContractCell
import io.amona.twinkorea.domain.PaymentLog
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.dtos.PurchasablePreContractDto
import io.amona.twinkorea.enums.PaymentMethod
import io.amona.twinkorea.enums.PreOrderStatus
import io.amona.twinkorea.exception.DuplicatedException
import io.amona.twinkorea.exception.MsgException
import io.amona.twinkorea.exception.NotFoundException
import io.amona.twinkorea.exception.WrongStatusException
import io.amona.twinkorea.repository.*
import io.amona.twinkorea.request.MyAccountPaymentRequest
import io.amona.twinkorea.response.ContractResponse
import io.amona.twinkorea.response.MultiCellContractResponse
import io.amona.twinkorea.response.SettleBankResponse
import io.amona.twinkorea.service.external.SettlebankService
import org.redisson.api.RedissonClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Service
class ContractService(
    private val appConfig: AppConfig,
    private val repo: ContractRepository,
    private val cellRepo: CellRepository,
    private val preOrderRepo: PreOrderRepository,
    private val paymentLogRepo: PaymentLogRepository,
    private val preOrderDslRepo: PreOrderRepositorySupport,
    private val preOrderUserRepo: PreOrderUserRepository,
    private val contractCellRepo: ContractCellRepository,
    private val userRepo: UserRepository,
    private val preOrderService: PreOrderService,
    private val userService: UserService,
    private val cellService: CellService,
    private val settlebankService: SettlebankService,
    private val redissonClient: RedissonClient,
    private val transactionManager: PlatformTransactionManager,
    ) {
    // 모두분양 가능 시기
    val allPurchaseAvailableFrom = LocalDateTime.parse(appConfig.allPurchaseAvailableFrom, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))

    /**
     * 구매 절차 시작 (내통장결제)
     * 1. 입력된 셀 ID 리스트를 순회하며 개별 셀에 대한 구매 가능여부를 검증한다.
     * 2. 구매 가능한 상태면, (_cellId: userId)를 각각 K: V로 하는 레디스 데이터의 키 리스트 (redisKeys)를 만든다.
     * 3. 레디스 키들의 리스트를 순회하면서, 해당 키 값이 이미 점유된 상태인지 확인한다
     * 4. 레디스 키 획득에 실패한 경우 failedList / 성공한 경우 successList 에 각각 결과 값을 넣는다.
     * 5. 성공한경우, 600 초 간 해당 유저가 해당 Key의 Value로 등록되며, 이미 점유한 셀에 대해서 요청 한경우도 마찬가지이다.
     * 6. failedList 의 크기가 0보다 클 때, 즉 key 획득에 실패한 케이스가 하나라도 있을 때, 성공한 redis key 획득을 모두 취소한다.
     * 7. 그리고 실패에 대한 응답값을 리턴한다.
     * 8. failedList 의 크기가 0일 때, 즉 입력된 cellIds에 대해 검증이 통과되고, key 획득도 성공했을 때,
     * 9. 셀들의 점유 상태를 변경하고 성공 응답값을 리턴한다.
     */
    @Transactional
    fun startPayment(user: User, cellIds: String): MultiCellContractResponse {
        val now = LocalDateTime.now()
        val nowAvailable = now.isAfter(allPurchaseAvailableFrom)
        if (!nowAvailable) throw WrongStatusException("현재는 분양 받을 수 있는 상태가 아닙니다.")
        val redisKeys = arrayListOf<Long>()

        val cells = cellService.getCellListFromCellIds(cellIds)
        cellService.checkCellIsPurchasableByCellList(cells, user)

        val targetPreOrder = cellService.findPreOrderByCell(cells[0])
        if (targetPreOrder.status != PreOrderStatus.OPEN) throw WrongStatusException("현재 분양 가능 지역이 아닙니다.")
        preOrderService.checkPreOrderIsRefundedByUser(targetPreOrder, user)

        cells.forEach { redisKeys.add(it.id) }
        val userId = user.id
        val successList = arrayListOf<ContractResponse>()
        val failedList = arrayListOf<ContractResponse>()
        // redis 키 획득
        redisKeys.forEach {
            val redisKey = "_$it"
            val redisValue = redissonClient.getBucket<Long>(redisKey).get()
            val redisRemainingTime = redissonClient.getBucket<Long>(redisKey).remainTimeToLive()
            when (redisValue) {
                // 점유중이 아닌 셀
                null -> {
                    redissonClient.getBucket<Long>(redisKey).set(userId, 600, TimeUnit.SECONDS)
                    successList.add(ContractResponse(it, "PAYMENT_START", 600_000))
                }
                // 내가 점유한 셀
                userId -> {
                    successList.add(ContractResponse(it, "ON_PAYMENT", redisRemainingTime))
                }
                // 다른 사람이 점유한 셀
                else -> {
                    failedList.add(ContractResponse(it, "FAILED", 0))
                }
            }
        }

        // 키 획득에 실패한게 하나라도 있으면 성공한 애들도 키를 해제함
        if (failedList.size > 0) {
            successList.forEach {
                val redisBucket = redissonClient.getBucket<Long>("_${it.cellId}")
                redisBucket.delete()
            }
            appConfig.logger.info{"[TWINKOREA API] 회원#${userId} 셀[${cellIds}}]에 대한 결제 요청 실패 -> 이미 점유된 셀[${failedList.map{it.cellId}}]"}
            return MultiCellContractResponse(
                status = "FAIL",
                message = "셀[${failedList.map{it.cellId}}]는 이미 점유된 상태입니다.",
                result = (successList + failedList) as MutableList<ContractResponse>
            )
        } else {
            // 모든 키 획득에 성공한 경우
            successList.forEach {
                if (it.message == "PAYMENT_START") {
                    // 결제 상태를 변경해주고
                    val targetCell = cellRepo.findById(id = it.cellId)!!
                    cellRepo.save(targetCell.copy(
                        onPaymentBy = userId,
                        onPayment = true,
                        updatedAt = now,
                    ))
                } else if (it.message == "ON_PAYMENT") {
                    val targetCell = cellRepo.findById(id = it.cellId)!!
                    cellRepo.save(targetCell.copy(
                        onPaymentBy = userId,
                        onPayment = true,
                    ))
                }
            }
            appConfig.logger.info{"[TWINKOREA API] 회원#${userId} 셀[${cellIds}}]에 대한 결제 요청 성공"}
            // 값을 리턴
            return MultiCellContractResponse(
                status = "SUCCESS",
                message = "결제 요청 성공",
                result = successList
            )
        }
    }

    /**
     * 구매 절차 완료 (내통장결제)
     * 1. 기본 정보를 불러온다 (회원정보, 셀정보 등)
     * 2. 셀 ID 들로 루프를 돌면서 셀들의 구매 가능여부를 한번더 확인한다.
     * 3. 셀 ID 들로 이루어진 redis key 들의 루프를 돌면서 모든 셀에 대해 결제 권한을 갖고 있는지 확인한다.
     * 4. 키를 모두 획득했으면 세틀뱅크에 결제 요청을 보낸다.
     * 5. 결제 요청이 실패하면 실패 결과를 리턴하고,
     * 6. 결제 요청이 성공하면 관련 데이터를 저장한 후 성공 결과를 리턴한다.
     * 7. 그 과정중 오류가 발생하면 DB를 수동으로 롤백 하고 셀의 소유권을 반환한다.
     */
    fun endPayment(cellIds: String, request: MyAccountPaymentRequest): SettleBankResponse {
        val user = userService.readUser(request.mercntParam1)
        val discount = settlebankService.convertMercntParam2ToLong(request.mercntParam2) > 0    // mercntParam2 는 할인되는 셀 갯수
        val now = LocalDateTime.now()
        val redisKeys = arrayListOf<Long>()

        val cells = cellService.getCellListFromCellIds(cellIds)
        cellService.checkCellIsPurchasableByCellList(cells, user)

        val targetPreOrder = cellService.findPreOrderByCell(cells[0])
        if (targetPreOrder.status != PreOrderStatus.OPEN) throw WrongStatusException("현재 분양 가능 지역이 아닙니다.")
        preOrderService.checkPreOrderIsRefundedByUser(targetPreOrder, user)

        // 셀들 검증
        cells.forEach { redisKeys.add(it.id) }

        // 키 획득 여부 검증
        redisKeys.forEach {
            val redisKey = "_$it"
            val redisBucket = redissonClient.getBucket<Long>(redisKey)
            val redisValue = redisBucket.get()
            if (redisValue != user.id) {
                appConfig.logger.info { "[TWINKOREA API]회원#${user.id}의 셀#${it}에 대한 결제 시도가 실패하였습니다. redis key 획득 실패" }
                throw WrongStatusException("결제 진행 가능한 셀이 아닙니다. (600초 초과, redis 키 획득 실패)")
            }
        }

        // 복수개의 키가 모두 획득된 상황
        val status = transactionManager.getTransaction(DefaultTransactionDefinition())
        try {
            val paymentProcess = settlebankService.paymentRequest(request, user.id)
            appConfig.logger.info { paymentProcess }
            // 결제 실패
            if (!paymentProcess.resultIsSuccess) {
                cells.forEach {
                    // 셀 점유 해제 처리
                    cellRepo.save(
                        it.copy(onPaymentBy = null, onPayment = null)
                    )
                }
                transactionManager.commit(status)
                appConfig.logger.info { "[TWINKOREA API] 유저#${user.id} 셀#${cellIds} 결제 실패 (세틀뱅크상 결제 실패): ${paymentProcess.resultMsg}" }
                return paymentProcess
            // 결제 성공
            } else {
                // 셀들 획득 처리
                cells.forEach {
                    cellRepo.save(
                        it.copy(
                            owner = user, onPayment = null, updatedAt = now, onPaymentBy = null
                        )
                    )
                }

                // contract 데이터 저장
                val contract = repo.save(
                    Contract(
                        preOrder = targetPreOrder,
                        createdAt = now,
                        updatedAt = now,
                        ordNo = paymentProcess.ordNo!!,     // 성공일경우 ordNo 는 무조건 있음
                        trNo = paymentProcess.trNo!!,       // 성공일경우 trNo 는 무조건 있음
                        user = user
                    )
                )

                // 관계 테이블에 저장
                cells.forEach {
                    contractCellRepo.save(
                        ContractCell(
                            contract = contract,
                            cell = it,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }

                // 구매 갯수 저장
                val purchasedCellCount = cellRepo.countAllByAreaIdAndOwnerIsNotNull(targetPreOrder.areaId)
                preOrderRepo.save(
                    targetPreOrder.copy(
                        purchaseCount = purchasedCellCount
                    )
                )

                // 로그 저장
                paymentLogRepo.save(
                    PaymentLog(
                        trNo = paymentProcess.trNo,
                        ordNo = paymentProcess.ordNo,
                        method = PaymentMethod.MYACCOUNT,
                        trPrice = paymentProcess.trPrice!!,
                        createdAt = now,
                        user = user,
                        cellIds = cellIds
                    )
                )

                // 할인인경우 쿠폰 보유 여부를 확인하고 할인되는 셀 갯수만큼 유저가 보유한 할인 쿠폰을 제거한다
                if (discount) {
                    if (user.preContractCouponCount < settlebankService.convertMercntParam2ToLong(request.mercntParam2)) throw WrongStatusException("사용할 수 있는 할인 쿠폰이 없습니다.")
                    appConfig.logger.info { "[TWINKOREA API] 유저#${user.id} 셀#${cellIds} 구매시 할인쿠폰 사용 (남은 할인쿠폰 갯수: ${user.preContractCouponCount - settlebankService.convertMercntParam2ToLong(request.mercntParam2)}" }
                    userRepo.save(user.copy(preContractCouponCount = user.preContractCouponCount - settlebankService.convertMercntParam2ToLong(request.mercntParam2)))
                }

                // 서울지역인 경우 쿠폰 카운트 + 1
                if (targetPreOrder.id < 630) {
                    appConfig.logger.info { "[TWINKOREA API] 유저#${user.id} 서울지역 셀#${cellIds} 구매로 할인쿠폰 사용 ${cells.size}개 추가 (남은 할인쿠폰 갯수: ${user.preContractCouponCount + cells.size}" }
                    userRepo.save(user.copy(preContractCouponCount = user.preContractCouponCount + cells.size))
                }

                // 성공했으니 이제 레디스 키 삭제
                redisKeys.forEach {
                    val redisKey = "_$it"
                    val redisBucket = redissonClient.getBucket<Long>(redisKey)
                    redisBucket.delete()
                }
                transactionManager.commit(status)
                appConfig.logger.info { "[TWINKOREA API] 유저#${user.id} 셀#${cellIds} 결제 성공" }
                return paymentProcess
            }
        } catch (e: RuntimeException) {
            transactionManager.rollback(status)
            val rollbackTransaction = transactionManager.getTransaction(DefaultTransactionDefinition())
            cells.forEach {
                cellRepo.save(it.copy(onPayment = null, onPaymentBy = null))
            }
            transactionManager.commit(rollbackTransaction)
            // 실패했으니 다시 레디스 키 삭제하여 반환
            redisKeys.forEach {
                val redisKey = "_$it"
                val redisBucket = redissonClient.getBucket<Long>(redisKey)
                redisBucket.delete()
            }
            appConfig.logger.info { "[TWINKOREA API] 유저#${user.id} 셀#${cellIds} 결제 실패 (DB 롤백 후 셀 점유 해제)" }
            throw MsgException(e.message.toString())        }
    }

    /**
     * 구매 도중 이탈
     */
    fun exitPayment(cellIds: String): Boolean? {
        val cellIdsList = cellIds.replace(" " ,"").split(",").toMutableList().map{it.toLong()}
        cellIdsList.forEach {
            val cell = cellService.readCell(it)
            cellRepo.save(
                cell.copy(
                    onPayment = null,
                    onPaymentBy = null,
                )
            )
        }
        return true
    }

    /**
     * 구매 가능한 지역 확인
     */
    fun getPurchasableAreaList(areaId: Long?, pageRequest: Pageable): Page<PurchasablePreContractDto> {
        val now = LocalDateTime.now()
        return when (now.isAfter(allPurchaseAvailableFrom)) {
            // 정상
            true -> {
                val purchasableList = preOrderDslRepo.findPurchasableInSeoul(areaId, pageRequest)
                purchasableList.map {
                    PurchasablePreContractDto(
                        areaId = it.areaId,
                        name = it.name,
                        totalCellCount = it.cellCount,
                        purchasableCellCount = it.cellCount - it.purchaseCount
                    )
                }
            }
            // 구매 기간 X
            false -> {
                val noneList = preOrderUserRepo.findNone(pageRequest)
                noneList.map {
                    PurchasablePreContractDto(
                        areaId = 0, name = "", totalCellCount = 0, purchasableCellCount = 0,
                    )
                }
            }
        }
    }

    fun paymentTest(cellIds: String, request: MyAccountPaymentRequest): SettleBankResponse {
        val user = userService.readUser(request.mercntParam1)
        val cellIdsList = cellIds.replace(" " ,"").split(",").toMutableList().map{it.toLong()}
        val cells = cellRepo.findAllById(cellIdsList)
        val now = LocalDateTime.now()
        val targetPreOrder = preOrderRepo.findByAreaId(cells[0].areaId!!) ?: throw NotFoundException("구매건과 일치하는 지역을 찾을 수 없습니다.")

        // 셀들 검증
        cells.forEach {
            if (it.owner != null || it.reserved) throw DuplicatedException("이미 구매된 셀 입니다.")
            else if (it.onPayment == true && it.onPaymentBy != user.id) throw DuplicatedException("이미 다른 유저에 의해 결제 진행중입니다.")
        }

        // 복수개의 키가 모두 획득된 상황
        val status = transactionManager.getTransaction(DefaultTransactionDefinition())
        try {
            val paymentProcess = settlebankService.paymentRequest(request, user.id)
            appConfig.logger.info { paymentProcess }
            // 결제 실패
            if (!paymentProcess.resultIsSuccess) {
                cells.forEach {
                    // 셀 점유 해제 처리
                    cellRepo.save(
                        it.copy(onPaymentBy = null, onPayment = null)
                    )
                }
                appConfig.logger.info { "[TWINKOREA API] 유저#${user.id} 셀#${cellIds} 결제 실패 (세틀뱅크상 결제 실패): ${paymentProcess.resultMsg}" }
                return paymentProcess
                // 결제 성공
            } else {
                // 셀들 획득 처리
                cells.forEach {
                    cellRepo.save(
                        it.copy(
                            owner = user, onPayment = null, updatedAt = now, onPaymentBy = null
                        )
                    )
                }

                // contract 데이터 저장
                val contract = repo.save(
                    Contract(
                        preOrder = targetPreOrder,
                        createdAt = now,
                        updatedAt = now,
                        ordNo = paymentProcess.ordNo!!,     // 성공일경우 ordNo 는 무조건 있음
                        trNo = paymentProcess.trNo!!,       // 성공일경우 trNo 는 무조건 있음
                        user = user
                    )
                )

                // 관계 테이블에 저장
                cells.forEach {
                    contractCellRepo.save(
                        ContractCell(
                            contract = contract,
                            cell = it,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }

                // 로그 저장
                paymentLogRepo.save(
                    PaymentLog(
                        trNo = paymentProcess.trNo,
                        ordNo = paymentProcess.ordNo,
                        method = PaymentMethod.MYACCOUNT,
                        trPrice = paymentProcess.trPrice!!,
                        createdAt = now,
                        user = user,
                        cellIds = cellIds
                    )
                )

                transactionManager.commit(status)
                appConfig.logger.info { "[TWINKOREA API] 유저#${user.id} 셀#${cellIds} 결제 성공" }
                return paymentProcess
            }
        } catch (e: RuntimeException) {
            transactionManager.rollback(status)
            val rollbackTransaction = transactionManager.getTransaction(DefaultTransactionDefinition())
            cells.forEach {
                cellRepo.save(it.copy(onPayment = null, onPaymentBy = null))
            }
            transactionManager.commit(rollbackTransaction)

            appConfig.logger.info { "[TWINKOREA API] 유저#${user.id} 셀#${cellIds} 결제 실패 (DB 롤백 후 셀 점유 해제)" }
            throw MsgException(e.message.toString())        }
    }
}