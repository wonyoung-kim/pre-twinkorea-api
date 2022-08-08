
package io.amona.twinkorea.service

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.*
import io.amona.twinkorea.dtos.*
import io.amona.twinkorea.enums.PreContractStatus
import io.amona.twinkorea.enums.PreOrderStatus
import io.amona.twinkorea.exception.*
import io.amona.twinkorea.repository.*
import io.amona.twinkorea.request.MyPreOrderListPageRequest
import io.amona.twinkorea.request.PageRequest
import io.amona.twinkorea.request.PreOrderRequest
import io.amona.twinkorea.response.CellListInfoByAreaIdResponse
import io.amona.twinkorea.response.MyPreOrderPageResponse
import org.redisson.api.RLock
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
class PreOrderService(
    private val repo: PreOrderRepository,
    private val preOrderUserRepo: PreOrderUserRepository,
    private val waitingListRepo: WaitingListRepository,
    private val preContractRepo: PreContractRepository,
    private val preOrderRepo: PreOrderRepository,
    private val userRepo: UserRepository,
    private val contractRepo: ContractRepository,
    private val contractCellRepo: ContractCellRepository,
    private val cellRepo: CellRepository,
    private val restaurantService: RestaurantService,
    private val userService: UserService,
    private val appConfig: AppConfig,
    private val redissonClient: RedissonClient,
    private val transactionManager: PlatformTransactionManager,
)
{
    // 사전청약 가능 시간
    val preOrderAvailableTo = appConfig.preOrderAvailableTo
    // 대기청약 가능 시간
    val waitingOrderAvailableTo = appConfig.waitingOrderAvailableTo

    /**
     * 사전청약 데이터 조회
     */
    fun readPreOrder(preOrderId: Long): PreOrder {
        return repo.findById(id = preOrderId)
            ?: throw NotFoundException("요청한 사전청약 ID에 해당하는 사전청약건을 찾을 수 없습니다.")
    }

    /**
     * areaId로 사전청약 데이터 조회
     */
    fun readPreOrderByAreaId(areaId: Long): PreOrder {
        return repo.findByAreaId(areaId)
            ?: throw NotFoundException("요청한 폴리곤 ID에 해당하는 사전청약건을 찾을 수 없습니다.")
    }

    /**
     * 사전청약 생성 (현재 이세호님이 주신 엑셀 기반으로 데이터를 수기로 입력해야하는 구조라 사용하지 않음)
     */
    @Transactional
    fun createPreOrder(user: User, request: PreOrderRequest): String {
        if (!user.admin) throw AuthenticationException("사전청약을 생성할 권한이 없습니다.")
        val areaIdInfoList = restaurantService.getAreaId(request.upHpAreaId.toInt(), null)
        val now = LocalDateTime.now()
        areaIdInfoList.forEach {
            // 베스트 모음집은 특정 구역에 속하지 않기 때문에 패스
            if (it.areaId != 0L) {
                val result = repo.save(
                    PreOrder(
                        name = "${it.areaTitle} 지역 사전청약",
                        applyCount = 0,
                        waitingCount = 0,
                        done = false,
                        areaId = it.areaId!!.toLong(),
                        latitude = it.centerLatitude,
                        longitude = it.centerLongitude,
                        district = it.areaTitle,
                        limit = 0,
                        ratio = 0.00,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                appConfig.logger.info {
                    "[TWINKOREA API] 관리자#${user.id}가 지역#${result.district}를 사전청약 리스트에 등록하였습니다. 사전청약 ID#${result.id}"
                }
            }
        }
        return "${areaIdInfoList.size} 개 사전청약 추가 완료"
    }

    /**
     * 회원이 신청한 사전청약 정보를 불러옵니다.
     * 사전청약과 대기청약을 각각 다른 테이블에서 관리하고있으며,
     * 페이징을 통해 2개 테이블의 데이터를 조회해야함으로
     * 1. 회원이 청약한 대기청약과 사전청약 전부를 하나의 리스트에 넣은 후
     * 2. 리스트를 created_at 시간순으로 정리하고
     * 3. 수동으로 페이징을 정리한뒤
     * 4. 입력된 pageRequest 데이터에 맞게 페이징된 데이터를 리턴
     * 하는 방식으로 구성해놨습니다.
     */
    fun getUserPreOrderList(user: User, pageRequest: MyPreOrderListPageRequest): MyPreOrderPageResponse {
        val result: MutableList<MyPreOrderListDto> = mutableListOf()
        val allWaitingOrder = waitingListRepo.findAllByUserOrderByCreatedAtDesc(user)
        val allPreOrder = preOrderUserRepo.findAllByUserOrderByCreatedAtDesc(user)
        val allPurchasedPreOrder = preContractRepo.findAllPurchasedPreOrderUserIdByUser(user)
        val allRefundedPreOrder = preContractRepo.findAllRefundedPreOrderUserIdByUser(user)
        val allPurchasedWaitingList = preContractRepo.findAllWaitingListIdByUser(user)
        val allRefundedWaitingList = preContractRepo.findAllRefundedWaitingListIdByUser(user)
        allWaitingOrder.forEach {
            // 경인지역 청약하면 629 / 630 으로 필터링하는거 없애면 됨
            val type = if (it.id in allPurchasedWaitingList) {
                "purchased"
            } else if (it.id in allRefundedWaitingList) {
                "refunded"
            } else {"waitingOrder"}
            result.add(
                MyPreOrderListDto(
                    id = 0,
                    preOrderId = it.preOrder.id,
                    areaId = it.preOrder.areaId,
                    type = type,
                    name = it.preOrder.district,
                    createdAt = it.createdAt!!,
                    preOrderStatus = it.preOrder.status,
                )
            )
        }
        allPreOrder.forEach {
            // 경인지역 청약하면 629 / 630 으로 필터링하는거 없애면 됨
            val type = if (it.id in allPurchasedPreOrder) {
                "purchased"
            } else if (it.id in allRefundedPreOrder) {
                "refunded"
            } else {"preOrder"}
            result.add(
                MyPreOrderListDto(
                    id = 0,
                    preOrderId = it.preOrder.id,
                    areaId = it.preOrder.areaId,
                    type = type,
                    name = it.preOrder.district,
                    createdAt = it.createdAt!!,
                    preOrderStatus = it.preOrder.status,
                    )
            )
        }
        result.sortByDescending { it.createdAt }
        result.forEach {
            it.id = result.indexOf(it).toLong() + 1L
        }
        val resultCounts = result.size
        val size = pageRequest.limit
        val page = pageRequest.page
        val start = (size * page).toInt()
        val end = (start + size).toInt()
        val pagedList: List<MyPreOrderListDto> = try {
            result.slice(start until end)
        } catch (e: IndexOutOfBoundsException) {
            result.slice(start until result.size)
        }
        return pageRequest.getPageData(resultCounts.toLong(), pagedList)
    }

    /**
     * 사전청약 정보 조회
     */
    fun getPreOrderDetail(preOrderId: Long): PreOrder {
        return readPreOrder(preOrderId)
    }

    /**
     * areaId로 사전청약 정보 조회
     */
    fun getPreOrderDetailByAreaId(areaId: Long): PreOrderDto {
        val preOrderModel = readPreOrderByAreaId(areaId)
        return PreOrderDto (
            id = preOrderModel.id,
            areaId = preOrderModel.areaId,
            applyCount = preOrderModel.applyCount,
            waitingCount = preOrderModel.waitingCount,
            district = preOrderModel.district,
            limit = preOrderModel.limit,
            ratio = preOrderModel.ratio,
            done = preOrderModel.done,
            status = null,
            preOrderStatus = preOrderModel.status,
        )
    }

    /**
     * 지도 렌더링에 필요한 데이터를 리턴합니다.
     */
    fun getPreOrderListForMap(user: User): List<PreOrderDto> {
        val preOrderDtoList: MutableList<PreOrderDto> = mutableListOf()
        val preOrderModels = repo.findAll()
        return when (user.id) {
            // 비회원인 경우
            0L -> {
                preOrderModels.forEach {
                    val preOrderDto = PreOrderDto(
                        id = it.id,
                        areaId = it.areaId,
                        district = it.district,
                        applyCount = it.applyCount,
                        waitingCount = it.waitingCount,
                        done = it.done,
                        ratio = it.ratio,
                        limit = it.limit,
                        status = null,
                        soldOut = (it.purchaseCount - (it.cellCount - it.reservedCellCount)) == 0L,
                        purchaseRatio = ((it.purchaseCount.toDouble() + it.reservedCellCount.toDouble())/it.cellCount.toDouble()),
                        preOrderStatus = it.status,
                    )
                    preOrderDtoList.add(preOrderDto)
                }
                preOrderDtoList
            }
            // 회원인 경우
            else -> {
                val userWaitingOrderIds = waitingListRepo.findAllByUserOrderByCreatedAtDesc(user).map{it.preOrder.id}
                val userPreOrderIds = preOrderUserRepo.findAllByUserOrderByCreatedAtDesc(user).map{it.preOrder.id}
                preOrderModels.forEach {
                    when (it.id) {
                        in userWaitingOrderIds -> {
                            preOrderDtoList.add(
                                PreOrderDto(
                                    id = it.id,
                                    areaId = it.areaId,
                                    district = it.district,
                                    applyCount = it.applyCount,
                                    waitingCount = it.waitingCount,
                                    done = it.done,
                                    ratio = it.ratio,
                                    limit = it.limit,
                                    status = "waitingOrder",
                                    soldOut = (it.purchaseCount - (it.cellCount - it.reservedCellCount)) == 0L,
                                    purchaseRatio = ((it.purchaseCount.toDouble() + it.reservedCellCount.toDouble())/it.cellCount.toDouble()),
                                    preOrderStatus = it.status,
                                )
                            )
                        }
                        in userPreOrderIds -> {
                            preOrderDtoList.add(
                                PreOrderDto(
                                    id = it.id,
                                    areaId = it.areaId,
                                    district = it.district,
                                    applyCount = it.applyCount,
                                    waitingCount = it.waitingCount,
                                    done = it.done,
                                    ratio = it.ratio,
                                    limit = it.limit,
                                    status = "appliedOrder",
                                    soldOut = (it.purchaseCount - (it.cellCount - it.reservedCellCount)) == 0L,
                                    purchaseRatio = ((it.purchaseCount.toDouble() + it.reservedCellCount.toDouble())/it.cellCount.toDouble()),
                                    preOrderStatus = it.status,
                                )
                            )
                        }
                        else -> {
                            preOrderDtoList.add(
                                PreOrderDto(
                                    id = it.id,
                                    areaId = it.areaId,
                                    district = it.district,
                                    applyCount = it.applyCount,
                                    waitingCount = it.waitingCount,
                                    done = it.done,
                                    ratio = it.ratio,
                                    limit = it.limit,
                                    status = null,
                                    soldOut = (it.purchaseCount - (it.cellCount - it.reservedCellCount)) == 0L,
                                    purchaseRatio = ((it.purchaseCount.toDouble() + it.reservedCellCount.toDouble())/it.cellCount.toDouble()),
                                    preOrderStatus = it.status
                                )
                            )
                        }
                    }
                }
                preOrderDtoList
            }
        }
    }

    /**
     * 모든 사전청약 리스트를 불러옵니다
     */
    fun getAllPreOrderList(option: String?, pageRequest: Pageable): Page<PreOrder> {
        return when (option) {
            "soldout" -> {
                repo.findAllByDoneIsTrueOrderByApplyCountDescLimitAsc(pageRequest = pageRequest)
            }
            "onsale" -> {
                repo.findAllByDoneIsFalseOrderByRatioDescApplyCountDescLimitAsc(pageRequest = pageRequest)
            }
            else -> {
                repo.findAllByOrderByDoneAscRatioDescApplyCountDescLimitAsc(pageRequest = pageRequest)
            }
        }
    }

    /**
     * 친구초대 랭킹 탑 10을 불러옵니다.
     */
    fun getTop10Inviter(): MutableList<InvitingRankingDto> {
        val rankList: MutableList<InvitingRankingDto> = mutableListOf()
        val rankModel = userRepo.findTop10ByIdIsNotAndDeactivateIsFalseOrderByInvitingCountDesc()
        var rank = 1L
        rankModel.forEach {
            val ranker = InvitingRankingDto(
                ranking = rank,
                invitingCount = it.invitingCount,
                email = maskEmail(it.email)
            )
            rankList.add(ranker)
            rank += 1L
        }
        return rankList
    }

    /**
     * 내 초대 랭킹을 불러옵니다.
     */
    fun getMyInvitingRank(user: User): InvitingRankingDto {
        return if (user.invitingCount == 0L) {
                InvitingRankingDto(ranking = 0, invitingCount = 0, email = user.email)
            } else {
                val userRanking = userRepo.getMyRanking(id = user.id)
                InvitingRankingDto(
                    ranking = userRanking.ranking, invitingCount = userRanking.invitingCount, email = userRanking.email
                )
            }
    }

    /**
     * 내 사전청약 관련 수치 정보를 조회합니다.
     */
    fun getMyPreOrderInfo(user: User): MyInvitingInfoDto {
        val userModel = userService.readUser(userId = user.id)
        // 서울지역만
        val purchasableCount = preOrderUserRepo.findAllByUserAndPreOrderIsInSeoul(user, PageRequest(0, 999).of()).totalElements
        val preOrderUserCount = preOrderUserRepo.countAllByUser(user)
        val waitingOrderCount = waitingListRepo.countAllByUser(user)
        return MyInvitingInfoDto(
            applyCount = preOrderUserCount,
            waitingCount = waitingOrderCount,
            invitingCount = userModel.invitingCount,
            couponCount = userModel.couponCount,
            purchasableCount = purchasableCount,
            popUp = userModel.preOrderPopup,
        )
    }

    /**
     * areaId를 통해 해당 지역에 속한 셀들의 정보를 불러옵니다.
     * 불러오는 도중, 요청한 회원 이름으로 해당 지역에 점유된 셀이 있는 경우 점유를 해제합니다.
     * 과도한 셀 점유를 막기 위해 조회 서비스이지만 update 쿼리를 추가했습니다.
     */
    @Transactional
    fun getCellsByAreaId(areaId: Long, user: User): CellListInfoByAreaIdResponse {
        val result = mutableListOf<CellDtoForCellList>()
        val targetPreOrder = preOrderRepo.findByAreaId(areaId) ?: throw NotFoundException("입력한 값으로 일치하는 사전청약건을 찾을 수 없습니다.")
        val purchasableCell: MutableList<Cell> = cellRepo.findAllByOnPaymentIsNullAndOwnerIsNullAndReservedIsFalseAndAreaId(areaId = areaId)
        val purchasedCell: MutableList<Cell> = cellRepo.findAllByOnPaymentIsTrueOrOwnerIsNotNullOrReservedIsTrueAndAreaId(areaId = areaId)

        // 비회원인경우
        if (user.id == 0L) {
            purchasedCell.forEach {
                result.add(
                    CellDtoForCellList(
                        cellId = it.id, areaId = it.areaId!!,
                        status = PreContractStatus.PURCHASED, centerX = it.centerX!!, centerY = it.centerY!!,
                    )
                )
            }
            purchasableCell.forEach {
                result.add(
                    CellDtoForCellList(
                        cellId = it.id, areaId = it.areaId!!,
                        status = PreContractStatus.PURCHASABLE, centerX = it.centerX!!, centerY = it.centerY!!,
                    )
                )
            }
            return CellListInfoByAreaIdResponse(
                refunded = false,
                areaId = targetPreOrder.areaId,
                name = targetPreOrder.name,
                centerLat = targetPreOrder.latitude,
                centerLong = targetPreOrder.longitude,
                totalCellCount = targetPreOrder.cellCount,
                reservedCellCount = null,
                purchasableCellCount = targetPreOrder.cellCount - targetPreOrder.reservedCellCount,
                purchasedCellCount = targetPreOrder.purchaseCount,
                cellInfoList = result,
                preOrderStatus = targetPreOrder.status,
            )
        }

        // 환불한 셀 찾기
        val refundedPreContract = preContractRepo.findAllByUserAndRefundedIsTrue(user)
        val refundedContract = contractRepo.findAllByUserAndRefundedIsTrue(user)
        val refundedId = mutableListOf<Long>()

        // 사전분양에 환불한게 있으면 환불한 건의 preOrderId를 넣어서 나중에 비교하여 환불한 지역인지 여부를 판단한다.
        if (refundedPreContract.size > 0) {
            refundedPreContract.forEach { preContract ->
                preContract.preOrderUser?.let {
                    refundedId.add(it.preOrder.id)
                }
                preContract.waitingList?.let {
                    refundedId.add(it.preOrder.id)
                }
            }
        }
        // 분양에 환불한게 있으면 분양때 샀던 셀에서 areaId를 추출하고, 그걸 통해 preOrder를 찾아낸다.
        if (refundedContract.size > 0) {
            refundedContract.forEach { contract ->
                val targetContractCell = contractCellRepo.findAllByContract(contract)
                refundedId.add(preOrderRepo.findByAreaId(targetContractCell[0].cell.areaId!!)!!.id)
            }
        }

        // 이미 보유했는지 여부 파악, 그리고 이미 구매한 것 먼저 result 리스트에 집어넣음
        var alreadyOwn = false
        purchasedCell.forEach {
            val status = if (it.owner == user) {
                alreadyOwn = true
                PreContractStatus.OWNED
            } else if (it.onPaymentBy == user.id) {
                cellRepo.save(it.copy(onPayment = null, onPaymentBy = null))        // 셀 리스트를 다시 조회할때 이미 점유하고있던 셀이 있으면 초기화
                PreContractStatus.PURCHASABLE
            } else {
                PreContractStatus.PURCHASED
            }
            result.add(
                CellDtoForCellList(
                    cellId = it.id, areaId = it.areaId!!,
                    status = status, centerX = it.centerX!!, centerY = it.centerY!!
                )
            )
        }

        // 이미 해당 지역의 셀을 소유했고 모두 구매 가능한 기간이 아니라면, 나머지 셀들에 대해서는 구매 불가 처리를 한다.
        when (alreadyOwn) {
            true -> {
                val allPurchaseAvailableFrom = LocalDateTime.parse(appConfig.allPurchaseAvailableFrom, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                val now = LocalDateTime.now()
                // 모두 구매 가능한 경우 -> 소유하지 않은 것은 구매 가능
                if (now.isAfter(allPurchaseAvailableFrom)) {
                    purchasableCell.forEach {
                        result.add(
                            CellDtoForCellList(
                                cellId = it.id, areaId = it.areaId!!,
                                status = PreContractStatus.PURCHASABLE, centerX = it.centerX!!, centerY = it.centerY!!,
                            )
                        )
                    }
                } else {
                    // 모두 구매 불가능한 경우 -> 나머지는 구매 불가
                    purchasableCell.forEach {
                        result.add(
                            CellDtoForCellList(
                                cellId = it.id, areaId = it.areaId!!,
                                status = PreContractStatus.PURCHASED, centerX = it.centerX!!, centerY = it.centerY!!,
                            )
                        )
                    }
                }
            }
            false -> {
                purchasableCell.forEach {
                    result.add(
                        CellDtoForCellList(
                            cellId = it.id, areaId = it.areaId!!,
                            status = PreContractStatus.PURCHASABLE, centerX = it.centerX!!, centerY = it.centerY!!,
                        )
                    )
                }
            }
        }
        return CellListInfoByAreaIdResponse(
            refunded = targetPreOrder.id in refundedId,
            areaId = targetPreOrder.areaId,
            name = targetPreOrder.name,
            centerLat = targetPreOrder.latitude,
            centerLong = targetPreOrder.longitude,
            totalCellCount = targetPreOrder.cellCount,
            reservedCellCount = null,
            purchasableCellCount = targetPreOrder.cellCount - targetPreOrder.reservedCellCount,
            purchasedCellCount = targetPreOrder.purchaseCount,
            cellInfoList = result,
            preOrderStatus = targetPreOrder.status,
            )
    }

    /**
     * 사전청약 신청 (정해진 기간 밖이면 오류 발생)
     * code 내에서 트랜잭션 관리 함
     */
    fun applyPreOrder(preOrderId: Long, user: User): PreOrderUser {
        val now = LocalDateTime.now()
        val availableTo = LocalDateTime.parse(preOrderAvailableTo, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
        val availableToString = availableTo.plusHours(9).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        if (now > availableTo) throw WrongStatusException("현재는 사전청약 기간이 아닙니다. *사전청약 가능 기간: ${availableToString}까지")

        // 동시에 1개의 신청만 받을 수 있도록 함 -> 원자성 보장을 위해
        val lock: RLock = redissonClient.getLock("$preOrderId")
        val isLocked = lock.tryLock(5, 6, TimeUnit.SECONDS)
        val status = transactionManager.getTransaction(DefaultTransactionDefinition())
        try {
            if (!isLocked) {
                throw MsgException("요청 처리중 오류가 발생했습니다.")
            }
            try {
                val preOrder = readPreOrder(preOrderId)
                if (preOrder.status != PreOrderStatus.PREORDER) throw WrongStatusException("사전청약 가능한 건이 아닙니다.")
                val alreadyApply = preOrderUserRepo.findByUserAndPreOrderId(user, preOrderId)
                if (alreadyApply != null) throw DuplicatedException("이미 청약 완료된 건입니다.")
                if (user.couponCount < 1) throw BalanceException("사전청약 쿠폰이 모자랍니다.")
                if (preOrder.done) throw WrongStatusException("이미 마감된 사전 청약입니다.")
                // 온전히 락이 걸린상태 -> 다른 유저의 개입이 방지되는 상황
                // 사전 청약 응모
                // 프리오더 참가자수 + 1 (사전청약 마감이면 상태를 마감으로 변경)
                val editedPreOrder = preOrder.applyCount.let {
                    when {
                        (it == (preOrder.limit - 1)) -> {
                            val applyCount = preOrder.applyCount + 1
                            repo.save(preOrder.copy(
                                applyCount = preOrder.applyCount + 1,
                                done = true,
                                ratio = (applyCount.toDouble()/preOrder.limit.toDouble())
                            ))
                        }
                        else -> {
                            val applyCount = preOrder.applyCount + 1
                            repo.save(preOrder.copy(
                                applyCount = preOrder.applyCount + 1,
                                ratio = (applyCount.toDouble()/preOrder.limit.toDouble())
                            ))
                        }
                    }
                }
                // 프리오더 <> 유저 관계 테이블에 등록
                val applied = preOrderUserRepo.save(
                    PreOrderUser(
                        preOrder = preOrder,
                        user = user,
                        createdAt = now, updatedAt = now
                    )
                )
                // 유저의 이용권 - 1
                val editedUser = userRepo.save(
                    user.copy(
                        couponCount = user.couponCount - 1
                    )
                )
                appConfig.logger.info {"[TWINKOREA API] 회원#${user.id}가 사전청약#${preOrder.id}에 응모하였습니다. " +
                        "회원의 남은 추천권은 ${editedUser.couponCount}이며 사전청약의 응모자는 ${editedPreOrder.applyCount}입니다."}
                transactionManager.commit(status)
                return applied
            } catch (e: RuntimeException) {
                transactionManager.rollback(status)
                throw MsgException(e.message.toString())
            }
        } catch (e: InterruptedException) {
            throw MsgException("Interrupted Lock")
        } finally {
            if (lock.isLocked && lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }

    /**
     * 대기청약 신청 (정해진 기간 밖이면 오류 발생)
     */
    @Transactional
    fun applyWaitingOrder(preOrderId: Long?, areaId: Long?, user: User): WaitingList {
        val now = LocalDateTime.now()
        val availableTo = LocalDateTime.parse(waitingOrderAvailableTo, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
        val availableToString = availableTo.plusHours(9).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        if (now > availableTo) throw WrongStatusException("현재는 대기청약 기간이 아닙니다. *대기청약 가능 기간: ${availableToString}까지")

        val preOrder: PreOrder = when {
            (preOrderId != null) -> {
                readPreOrder(preOrderId)
            }
            (areaId != null) -> {
                readPreOrderByAreaId(areaId)
            }
            else -> {
                throw NotNullException("preOrderId 혹은 areaId는 필수 인자입니다.")
            }
        }
        val alreadyApply = preOrderUserRepo.findByUserAndPreOrderId(user, preOrder.id)
        if (preOrder.status == PreOrderStatus.NONE || preOrder.status == PreOrderStatus.OPEN) throw WrongStatusException("사전청약 가능한 건이 아닙니다.")
        if (alreadyApply != null) throw  DuplicatedException("이미 사전청약을 완료했습니다.")
        val alreadyWaitingApply = waitingListRepo.findByUserAndPreOrder(user, preOrder)
        if (alreadyWaitingApply != null) throw DuplicatedException("이미 대기청약을 완료했습니다.")
        if (user.couponCount < 1) throw BalanceException("사전청약 쿠폰이 모자랍니다.")
        if (!preOrder.done) throw WrongStatusException("아직 마감되지 않은 청약건입니다. 대기 청약이 아닌, 청약을 이용해주세요.")

        // 사전청약의 대기 청약 숫자 + 1
        repo.save(
            preOrder.copy(
                waitingCount = preOrder.waitingCount + 1
            )
        )
        // 대기 인원 등록
        val applied = waitingListRepo.save(
            WaitingList(
                preOrder = preOrder,
                user = user,
                createdAt =  now, updatedAt = now
            )
        )
        val userTurn = waitingListRepo.countAllByCreatedAtBefore(createdAt = now)
        // 유저의 이용권 - 1
        val editedUser = userRepo.save(
            user.copy(
                couponCount = user.couponCount - 1
            )
        )
        appConfig.logger.info {"[TWINKOREA API] 회원#${user.id}가 대기청약#${preOrder.id}에 응모하였습니다. " +
                "회원의 남은 추천권은 ${editedUser.couponCount}이며 청약자의 순번은 ${userTurn}입니다."}
        return applied
    }

    /**
     * 해당 사전청약을 환불했는지 여부를 파악
     */
    fun checkPreOrderIsRefundedByUser(preOrder: PreOrder, user: User) {
        val refundedContract = contractRepo.findByPreOrderAndRefundedIsTrueAndUser(preOrder, user)
        val refundedPreOrderIds = preContractRepo.findAllRefundedPreOrderIdByUser(user)
        val refundedWaitingOrderIds = preContractRepo.findAllRefundedWaitingOrderIdByUser(user)
        if (refundedContract != null
            || preOrder.id in refundedPreOrderIds
            || preOrder.id in refundedWaitingOrderIds
        ) throw WrongStatusException("이미 환불했던 지역입니다. 한번 환불한 지역은 재구매 할 수 없습니다.")
    }

    /**
     * 사전청약 수정 -> 현재 쓰일일 없음 (따로 기획된 사항없어서 수정할 내용 있으면 직접 DB에서 수정해야함)
     */
    fun editPreOrder(user: User, request: PreOrderRequest, preOrderId: Long): PreOrder {
        if (!user.admin) throw AuthenticationException("사전청약을 수정할 권한이 없습니다.")
        if (request.name == null) throw NotNullException("name 은 필수 파라미터입니다.")
        val targetPreOrder = readPreOrder(preOrderId)
        val result = repo.save(targetPreOrder.copy(
            name = request.name
        ))
        appConfig.logger.info {"[TWINKOREA API] 관리자#${user.id}가 사전청약${targetPreOrder.id}의 제목을 ${result.name}로 수정하였습니다."}
        return result
    }

    /**
     * 사전청약 삭제 -> 현재 쓰일일 없음
     */
    fun deletePreOrder(user: User, preOrderId: Long): String {
        if (!user.admin) throw AuthenticationException("사전청약을 삭제할 권한이 없습니다.")
        val targetPreOrder = readPreOrder(preOrderId)
        if (targetPreOrder.preOrderUsers.size != 0) throw WrongStatusException("이미 사전청약이 시작된 상태에서는 삭제할 수 없습니다.")
        repo.delete(targetPreOrder)
        appConfig.logger.info {"[TWINKOREA API] 관리자#${user.id}가 사전청약${targetPreOrder.id}를 삭제하였습니다."}
        return "SUCCESS"
    }

    /**
     * 랭킹 조회시 유저 이메일 마스킹하는 메서드
     */
    private fun maskEmail(email: String): String {
        return email.replace(Regex("""((?:\.|^).)?.(?=.*@)"""), "$1*")
    }
}