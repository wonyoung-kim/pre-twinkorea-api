package io.amona.twinkorea.service

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.*
import io.amona.twinkorea.dtos.PurchasablePreContractDto
import io.amona.twinkorea.dtos.PurchaseHistoryDto
import io.amona.twinkorea.enums.PaymentMethod
import io.amona.twinkorea.enums.PreContractAvailable
import io.amona.twinkorea.enums.PreOrderStatus
import io.amona.twinkorea.exception.DuplicatedException
import io.amona.twinkorea.exception.MsgException
import io.amona.twinkorea.exception.NotFoundException
import io.amona.twinkorea.exception.WrongStatusException
import io.amona.twinkorea.repository.*
import io.amona.twinkorea.request.MyAccountPaymentRequest
import io.amona.twinkorea.response.ContractResponse
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

/**
 * 사전분양 관련 서비스 코드 모음
 * * 사전분양은 "사전청약" 혹은 "대기청약" 대상자들에 한해 판매를 진행하는 것임
 */
@Service
class PreContractService(
    private val repo: PreContractRepository,
    private val paymentLogRepo: PaymentLogRepository,
    private val cellService: CellService,
    private val settlebankService: SettlebankService,
    private val userService: UserService,
    private val cellRepo: CellRepository,
    private val userRepo: UserRepository,
    private val preOrderRepo: PreOrderRepository,
    private val preOrderUserRepo: PreOrderUserRepository,
    private val preOrderUserDslRepo: PreOrderUserRepositorySupport,
    private val waitingListRepo: WaitingListRepository,
    private val waitingListDslRepo: WaitingListRepositorySupport,
    private val appConfig: AppConfig,
    private val redissonClient: RedissonClient,
    private val transactionManager: PlatformTransactionManager,
    ) {
    val preOrderPurchaseAvailableFrom = appConfig.preOrderPurchaseAvailableFrom
    val preOrderPurchaseAvailableTo   = appConfig.preOrderPurchaseAvailableTo
    val waitingOrderPurchaseAvailableFrom = appConfig.waitingOrderPurchaseAvailableFrom
    val waitingOrderPurchaseAvailableTo   = appConfig.waitingOrderPurchaseAvailableTo

    // 대기 청약자 구매 가능 시간 범위
    val waitingOrderAvailableFrom  = LocalDateTime.parse(waitingOrderPurchaseAvailableFrom, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    val waitingOrderAvailableTo = LocalDateTime.parse(waitingOrderPurchaseAvailableTo, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    // 사전 청약자 구매 가능 시간 범위
    val preOrderAvailableFrom = LocalDateTime.parse(preOrderPurchaseAvailableFrom, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    val preOrderAvailableTo = LocalDateTime.parse(preOrderPurchaseAvailableTo, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))

    /**
     * 구매 가능한 청약건 확인
     */
    fun getPurchasablePreContractList(user: User, areaId: Long?, pageRequest: Pageable): Page<PurchasablePreContractDto> {
        val nowAvailable = getNowAvailable()
        return when (nowAvailable) {
            PreContractAvailable.PREORDER -> {
                val alreadyContract = repo.findAllPreOrderUserIdByUser(user)
                val preOrderList = preOrderUserDslRepo.findAllAvailablePreContract(user, areaId, pageRequest, alreadyContract)
                preOrderList.map { preOrderUser: PreOrderUser ->
                    val totalCellCount = preOrderUser.preOrder.cellCount                // 전체 셀 갯수
                    val purchaseCount = preOrderUser.preOrder.purchaseCount             // 이미 구매한 사전예약건인지 확인
                    PurchasablePreContractDto(
                        areaId = preOrderUser.preOrder.areaId,
                        name = preOrderUser.preOrder.name,
                        totalCellCount = totalCellCount,
                        purchasableCellCount = totalCellCount - purchaseCount,
                    )
                }
            }
            PreContractAvailable.WAITING -> {
                val alreadyContract = repo.findAllWaitingListUserIdByUser(user)
                val waitingList = waitingListDslRepo.findAllAvailablePreContract(user, areaId, pageRequest, alreadyContract)
                waitingList.map { waitingOrder: WaitingList ->
                    val totalCellCount = waitingOrder.preOrder.cellCount                // 전체 셀 갯수
                    val purchaseCount = waitingOrder.preOrder.purchaseCount             // 이미 구매한 사전예약건인지 확인
                    PurchasablePreContractDto(
                        areaId = waitingOrder.preOrder.areaId,
                        name = waitingOrder.preOrder.name,
                        totalCellCount = totalCellCount,
                        purchasableCellCount = totalCellCount - purchaseCount,
                    )
                }
            }
            // 구매 기간이 아닐땐 아무것도 리턴하지 않음
            PreContractAvailable.NOBODY -> {
                val noneList = preOrderUserRepo.findNone(pageRequest)
                noneList.map {
                    PurchasablePreContractDto(
                        areaId = 0, name = "", totalCellCount = 0, purchasableCellCount = 0,
                    )
                }
            }
        }
    }

    /**
     * 구매 절차 시작 (내통장결제)
     */
    @Transactional    // repo.save 를 위해 readOnly = false 인 트랜잭션 생성 (나머지 select 쿼리는 readOnly = true 트랜잭션을 별개로 생성함)
    fun startPayment(user: User, cellId: Long): ContractResponse {
        val nowAvailable = getNowAvailable()
        val cell = cellService.readCell(cellId)
        val userId = user.id
        val now = LocalDateTime.now()

        // 선택한 셀에 해당하는 사전청약 정보가 없을 경우
        val targetPreOrderAreaId = cell.areaId
            ?: throw NotFoundException("사전청약을 범위 내에서 찾을 수 없는 셀 입니다.")
        // 셀에 할당된 사전청약 정보가 틀렸을 경우
        val targetPreOrder = preOrderRepo.findByAreaId(targetPreOrderAreaId)
            ?: throw NotFoundException("사전 청약 정보를 불러올 수 없습니다.")
        if (targetPreOrder.status != PreOrderStatus.PRECONTRACT) throw WrongStatusException("현재 사전분양 가능 지역이 아닙니다.")

        when (nowAvailable) {
            PreContractAvailable.PREORDER -> {
                val preOrderUser = preOrderUserRepo.findByUserAndPreOrderId(user = user, id = targetPreOrder.id)
                    ?: throw NotFoundException("사전청약한 지역의 셀이 아닙니다.")

                // 이미 분양 받은 지역인지 확인
                val alreadyExist = repo.findByUserAndPreOrderUser(user = user, preOrderUser = preOrderUser)
                if (alreadyExist != null) throw DuplicatedException("사전분양은 1지역당 1개의 셀만 구매가능합니다. ${targetPreOrder.name}은 이미 분양받은 지역입니다. *분양건 환불 후 해당지역 재구매 불가")
            }
            PreContractAvailable.WAITING -> {
                val waitingList = waitingListRepo.findByUserAndPreOrderId(user = user, id = targetPreOrder.id)
                    ?: throw NotFoundException("대기청약한 지역의 셀이 아닙니다.")
                // 이미 분양 받은 지역인지 확인
                val alreadyExist = repo.findByUserAndWaitingList(user = user, waitingList = waitingList)
                if (alreadyExist != null) throw DuplicatedException("사전분양은 1지역당 1개의 셀만 구매가능합니다. ${targetPreOrder.name}은 이미 분양받은 지역입니다. *분양건 환불 후 해당지역 재구매 불가")
            } else -> {
                throw WrongStatusException("현재 분양 신청이 가능한 시간이 아닙니다.")
            }
        }
        // 회원이 사전청약/대기청약한 청약건에 해당하는 셀이 아닐경우

        // 분양 완료 셀인지 확인
        if (cell.owner != null) throw DuplicatedException("이미 구매된 셀 입니다.")

        // redis 용 변수 선언
        val redisKey = "_$cellId"                                                               // 레디스 키(셀ID)-벨류(유저ID) 검증을 위한 key 값
        val redisValue = redissonClient.getBucket<Long>(redisKey).get()                         // 이미 진행중인 결제건이면 이 값이 null 일 수 없음
        val redisRemainingTime = redissonClient.getBucket<Long>(redisKey).remainTimeToLive()    // 이미 진행중인 결제건이면 이 값은 남은 제한시간
        appConfig.logger.info { "[TWINKOREA API] 유저#${userId} 셀#${cellId} 결제 시도" }
        when (redisValue) {
            // redis value 가 없는경우 > 첫시도 = 정상
            null -> {
                redissonClient.getBucket<Long>(redisKey).set(userId, 600, TimeUnit.SECONDS)  // 아니면 결제 진행 하기 위해 redis 에 key - value 등록
                cellRepo.save(cell.copy(
                    onPayment = true,
                    onPaymentBy = userId,
                    updatedAt = now
                ))
                appConfig.logger.info { "[TWINKOREA API] 유저#${userId} 셀#${cellId} 결제 시도 성공" }
                return ContractResponse(
                    cellId = cellId,
                    message = "PAYMENT_START",
                    timeRemaining = 600_000
                )
            }
            // redis value 가 userId 인 경우 -> 재시도 = 기존에 결제 시도했던 유저 / 정상
            userId -> {
                appConfig.logger.info { "[TWINKOREA API] 유저#${userId} 셀#${cellId} 이미 진행중인 건에 추가 결제 시도" }
                return ContractResponse(
                    cellId = cellId,
                    message = "ON_PAYMENT",
                    timeRemaining = redisRemainingTime
                )
            }
            // 오류
            else -> {
                appConfig.logger.info { "[TWINKOREA API] 유저#${userId} 셀#${cellId} 결제 시도 실패 (redis key 획득 실패: 다른사람이 이미 점유중인 셀)" }
                throw MsgException("이미 다른 유저가 결제 프로세스를 진행중입니다.")
            }
            }
        }

    /**
     * 구매 절차 종료 with 내통장결제 (트랜잭션은 코드내에서 직접 관리)
     */
    @Transactional
    fun endPaymentMyAccount(cellId: Long, request: MyAccountPaymentRequest): SettleBankResponse {
        val nowAvailable = getNowAvailable()
        val user = userService.readUser(request.mercntParam1)                                   // mercntParam1 로 userId 를 떙겨옴
        val cell = cellService.readCell(cellId)
        val discount = settlebankService.convertMercntParam2ToLong(request.mercntParam2) > 0    // mercntParam2 는 할인되는 셀 갯수
        if (cell.owner != null) throw DuplicatedException("이미 구매된 셀 입니다.")
        val userId = user.id
        val now = LocalDateTime.now()

        val redisKey = "_$cellId"                                                               // 레디스 키(셀ID)-벨류(유저ID) 검증을 위한 key 값
        val redisBucket = redissonClient.getBucket<Long>(redisKey)
        val redisValue = redisBucket.get()                                                      // 유저가 진행한 결제라면 이 value = userId
        when (redisValue) {
            // redis value 가 userId 인 경우 -> 정상
            userId -> {
                val targetPreOrderAreaId = cell.areaId
                    ?: throw NotFoundException("사전청약을 범위 내에서 찾을 수 없는 셀 입니다.")
                val targetPreOrder = preOrderRepo.findByAreaId(targetPreOrderAreaId)
                    ?: throw NotFoundException("사전 청약 정보를 불러올 수 없습니다.")
                if (targetPreOrder.status != PreOrderStatus.PRECONTRACT) throw WrongStatusException("현재 사전분양 가능 지역이 아닙니다.")

                // 대기인지 사전인지 분기
                when (nowAvailable) {
                    PreContractAvailable.PREORDER -> {
                        preOrderUserRepo.findByUserAndPreOrderId(user = user, id = targetPreOrder.id)
                            ?: throw NotFoundException("사전청약한 지역의 셀이 아닙니다.")
                    }
                    PreContractAvailable.WAITING -> {
                        waitingListRepo.findByUserAndPreOrderId(user = user, id = targetPreOrder.id)
                            ?: throw NotFoundException("대기청약한 지역의 셀이 아닙니다.")
                    }
                    PreContractAvailable.NOBODY -> {
                        throw WrongStatusException("이미 분양 가능 시간이 종료되었습니다.")
                    }
                }


                val status = transactionManager.getTransaction(DefaultTransactionDefinition())
                try {
                    val paymentProcess = settlebankService.paymentRequest(request, user.id)
                    appConfig.logger.info { paymentProcess }
                    if (!paymentProcess.resultIsSuccess) {
                        cellRepo.save(
                            cell.copy(onPayment = null, onPaymentBy = null) // 결제 실패시
                        )
                        appConfig.logger.info { "[TWINKOREA API] 유저#${userId} 셀#${cellId} 결제 실패 (세틀뱅크상 결제 실패): ${paymentProcess.resultMsg}" }
                        transactionManager.commit(status)
                        return paymentProcess
                    } else {
                        // 셀 획득 처리
                        cellRepo.save(
                            cell.copy(
                                owner = user,
                                onPayment = null,
                                updatedAt = now,
                                onPaymentBy = null,
                            )
                        )

                        // 대기인지 사전인지 분기하여 pre-contract 데이터 저장
                        when (nowAvailable) {
                            PreContractAvailable.PREORDER -> {
                                val preOrderUser = preOrderUserRepo.findByUserAndPreOrderId(user = user, id = targetPreOrder.id)
                                repo.save(
                                    PreContract(
                                        preOrderUser = preOrderUser!!,
                                        trNo = paymentProcess.trNo!!,       // 성공인경우 trNo 는 무조건 있음
                                        ordNo = paymentProcess.ordNo!!,     // 성공인경우 ordNo 는 무조건 있음
                                        user = user,
                                        cell = cell,
                                        updatedAt = now,
                                        createdAt = now,
                                    )
                                )
                            }
                            PreContractAvailable.WAITING -> {
                                val waitingList = waitingListRepo.findByUserAndPreOrderId(user = user, id = targetPreOrder.id)
                                repo.save(
                                    PreContract(
                                        waitingList = waitingList!!,
                                        trNo = paymentProcess.trNo!!,       // 성공인경우 trNo 는 무조건 있음
                                        ordNo = paymentProcess.ordNo!!,     // 성공인경우 ordNo 는 무조건 있음
                                        user = user,
                                        cell = cell,
                                        updatedAt = now,
                                        createdAt = now,
                                    )
                                )
                            }
                            PreContractAvailable.NOBODY -> {
                                throw WrongStatusException("이미 분양 가능 시간이 종료되었습니다.")
                            }
                        }

                        // 해당 사전청약 지역의 purchaseCount를 계산한다.
                        val purchasedCellCount = cellRepo.countAllByAreaIdAndOwnerIsNotNull(cell.areaId)
                        preOrderRepo.save(
                            targetPreOrder.copy(
                                purchaseCount = purchasedCellCount
                            )
                        )

                        // 결제 내역을 paymentLog에 기록한다.
                        paymentLogRepo.save(
                            PaymentLog(
                                trNo = paymentProcess.trNo,
                                ordNo = paymentProcess.ordNo,
                                method = PaymentMethod.MYACCOUNT,
                                trPrice = paymentProcess.trPrice!!, // 성공인경우 trPrice 는 무조건 있음
                                createdAt = now,
                                user = user,
                                cell = cell,
                            )
                        )

                        // 할인인경우 쿠폰 보유 여부를 확인하고 할인되는 셀 갯수만큼 유저가 보유한 할인 쿠폰을 제거한다
                        if (discount) {
                            if (user.preContractCouponCount < settlebankService.convertMercntParam2ToLong(request.mercntParam2)) throw WrongStatusException("사용할 수 있는 할인 쿠폰이 없습니다.")
                            appConfig.logger.info { "[TWINKOREA API] 유저#${userId} 셀#${cellId} 구매시 할인쿠폰 사용 (남은 할인쿠폰 갯수: ${user.preContractCouponCount - settlebankService.convertMercntParam2ToLong(request.mercntParam2)}" }
                            userRepo.save(user.copy(preContractCouponCount = user.preContractCouponCount - settlebankService.convertMercntParam2ToLong(request.mercntParam2)))
                        }

                        // redis key 삭제
                        redisBucket.delete()
                        transactionManager.commit(status)
                        appConfig.logger.info { "[TWINKOREA API] 유저#${userId} 셀#${cellId} 결제 성공" }
                        return paymentProcess
                    }
                } catch (e: RuntimeException) {
                    transactionManager.rollback(status)
                    val rollbackTransaction = transactionManager.getTransaction(DefaultTransactionDefinition())
                    cellRepo.save(cell.copy(onPayment = null, onPaymentBy = null))
                    transactionManager.commit(rollbackTransaction)
                    redisBucket.delete()
                    appConfig.logger.info { "[TWINKOREA API] 유저#${userId} 셀#${cellId} 결제 실패 (DB 롤백 후 셀 점유 해제)" }
                    throw MsgException(e.message.toString())
                }
            }
            else -> {
                throw MsgException("결제 진행 시간 초과 (600초가 지나 더이상 해당 셀에 대한 점유자가 아닙니다.)")
            }
        }
    }

    /**
     * 구매 도중 이탈
     */
    fun exitWhilePaymentMyAccount(cellId: Long): Boolean? {
        val targetCell = cellService.readCell(cellId)
        val editedCell = cellRepo.save(
            targetCell.copy(
                onPayment = null,
                onPaymentBy = null,
            )
        )
        return editedCell.onPayment
    }

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

    /**
     * 현재 어떠한 청약건이 구매가능한 시간대인지를 확인합니다.
     * 대기청약자들이 구매 가능한 시기이면, WATING
     * 사전청약자들이 구매 가능한 시기이면, PREODER
     * 사전/대기 청약자 모두 구매 불가능한 시기이면 (모두분양 시기이거나, 분양불가 시기인경우), NOBODY
     * 를 리턴합니다.
     */
    private fun getNowAvailable(): PreContractAvailable {
        val now = LocalDateTime.now()
        // 시간대 검증
        return if (now.isAfter(waitingOrderAvailableFrom) && now.isBefore(waitingOrderAvailableTo)) {
            PreContractAvailable.WAITING
        }  else if (now.isAfter(preOrderAvailableFrom) && now.isBefore(preOrderAvailableTo)) {
            PreContractAvailable.PREORDER
        } else {
            PreContractAvailable.NOBODY
        }
    }
}