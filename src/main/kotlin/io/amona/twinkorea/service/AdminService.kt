package io.amona.twinkorea.service

import io.amona.twinkorea.auth.JwtTokenProvider
import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.Admin
import io.amona.twinkorea.domain.Cell
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.dtos.*
import io.amona.twinkorea.enums.PaymentStatus
import io.amona.twinkorea.enums.PreContractStatus
import io.amona.twinkorea.enums.SnsProvider
import io.amona.twinkorea.exception.*
import io.amona.twinkorea.repository.*
import io.amona.twinkorea.request.*
import io.amona.twinkorea.response.*
import io.amona.twinkorea.service.external.AuthService
import io.amona.twinkorea.service.external.SettlebankService
import io.amona.twinkorea.transformer.AdminUserTransformer
import io.amona.twinkorea.transformer.UserTransformer
import io.amona.twinkorea.utils.TOPT.TOTPTokenGenerator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

@Service
class AdminService(
    private val settlebankService: SettlebankService,
    private val authService: AuthService,
    private val repo: AdminRepository,
    private val dslRepo: AdminRepositorySupport,
    private val userDslRepo: UserRepositorySupport,
    private val userRepo: UserRepository,
    private val preOrderRepo: PreOrderRepository,
    private val preOrderDslRepo: PreOrderRepositorySupport,
    private val paymentLogRepo: PaymentLogRepository,
    private val paymentLogDslRepo: PaymentLogRepositorySupport,
    private val preContractRepo: PreContractRepository,
    private val cellRepo: CellRepository,
    private val contractRepo: ContractRepository,
    private val contractCellRepo: ContractCellRepository,
    private val cellService: CellService,
    private val userService: UserService,
    private val transformer: AdminUserTransformer,
    private val userTransformer: UserTransformer,
    private val jwtTokenProvider: JwtTokenProvider,
    private val appConfig: AppConfig,
    private val transactionManager: PlatformTransactionManager,
) {
    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

    /**
     * 어드민 회원 데이터를 조회합니다.
     */
    fun readAdmin(adminId: Long): Admin {
        return repo.findByIdAndUser_DeactivateIsFalse(id = adminId)
            ?: throw NotFoundException("입력한 어드민 ID로 어드민을 찾아낼 수 없습니다.")
    }

    /**
     * userId 로 어드민 데이터를 조회합니다. userId는 admin 테이블과 user 테이블을 연결하는 user 테이블의 pk 입니다.
     */
    fun readAdminByUserId(userId: Long): Admin {
        return repo.findByUserIdAndUser_DeactivateIsFalse(userId = userId)
            ?: throw NotFoundException("입력한 회원 ID#${userId}로 어드민을 찾아낼 수 없습니다.")
    }

    /**
     * 어드민 로그인
     */
    fun login(userRequest: UserRequest): LoginResponse {
        // 입력값 검증
        if (
            userRequest.email == null ||
            userRequest.pw == null    ||
            userRequest.otpCode == null
        ) {throw NotNullException("phoneNumber, pw, otpCode 는 필수 입력값입니다.")}
        val user = userRepo.findByEmailAndDeactivateIsFalse(userRequest.email)
            ?: throw NotFoundException("요청한 입력값과 매칭되는 회원을 찾을 수 없습니다. 입력값을 확인해주세요.")

        // OTP 검증
        val userSecretKey = user.otpSecretKey ?: throw WrongStatusException("OTP 인증용 시크릿키가 등록되지않았습니다. 등록후 이용해주세요.")
        val validateToken = TOTPTokenValidation.validate(userSecretKey, userRequest.otpCode)
        if (!validateToken) throw AuthenticationException("OTP 인증 과정 중 오류가 발생했습니다.")

        // 비밀번호 검증
        if(passwordEncoder.matches(userRequest.pw, user.pw)) {
            return LoginResponse(
                jwt=jwtTokenProvider.createToken(snsId = user.snsId, admin = true),
                user=User(id = user.id, email = user.email,
                    createdAt = user.createdAt, updatedAt = user.updatedAt, phoneNumber = user.phoneNumber,
                    refreshToken = user.refreshToken)
            )
        } else {
            throw AuthenticationException("비밀번호가 회원 정보와 일치하지 않습니다.")
        }
    }

    /**
     * 특정 어드민의 OTP 시크릿 키 재발급
     */
    fun renewSecretKey(user: User, adminId: Long): CreateAdminResponse {
        val superAdmin = readAdminByUserId(user.id)
        if (!superAdmin.superAdmin) throw AuthenticationException("어드민 회원의 시크릿 키 재발급 요청은 슈퍼관리자만 할 수 있습니다.")
        val targetAdminUser = readAdmin(adminId) // 시크릿키를 재발급 할 어드민
        val secretKey = TOTPTokenGenerator.generateSecretKey()
        val barcodeUrl = TOTPTokenGenerator.getGoogleAuthenticatorBarcode(
            secretKey, targetAdminUser.id.toString(), "TwinKorea Admin Auth"
        )
        userRepo.save(user.copy(otpSecretKey = secretKey))
        return CreateAdminResponse(otpSecretKey = secretKey, qrBarcodeUrl = barcodeUrl)
    }

    /**
     * 관리자 정보 불러오기
     */
    fun getAdminInfo(adminId: Long): AdminInfoDto {
        val admin = readAdmin(adminId = adminId)
        return AdminInfoDto(
            id = admin.id,
            email = admin.user.email,
            nickname = admin.user.nickname,
            adminRole = admin.adminRole ?: "없음",
            phoneNumber = admin.user.phoneNumber,
            isSuper = admin.superAdmin
        )
    }

    /**
     * 관리자 리스트 불러오기
     */
    fun getAdminList(request: UserSearchRequest, pageRequest: Pageable): Page<AdminInfoDto> {
        val adminUserList = dslRepo.findAll(request = request, pageable = pageRequest)
        return adminUserList.map { admin: Admin ->
            AdminInfoDto(
                id = admin.id,
                email = admin.user.email,
                nickname = admin.user.nickname,
                adminRole = admin.adminRole ?: "없음",
                phoneNumber = admin.user.phoneNumber,
                isSuper = admin.superAdmin
            )
        }
    }

    /**
     * 특정 회원 정보 보기
     */
    fun getUserInfo(userId: Long): UserInfoDto {
        val user = userService.readUser(userId)
        val userType = when (user.admin) {
            false -> "일반회원"
            true -> if (readAdminByUserId(user.id).superAdmin) {
                "슈퍼관리자"
            } else {
                "일반관리자"
            }
        }
        return UserInfoDto(
            id = user.id,
            email = user.email,
            nickname = user.nickname,
            phoneNumber = user.phoneNumber,
            userType = userType,
            // snsProvider 값은 null 일경우 로컬회원으로 간주
            snsProvider = SnsProvider.valueOf(user.snsProvider ?: "X"),
            createdAt = user.createdAt!!
        )
    }

    /**
     * 회원 리스트 불러오기
     */
    fun getUserList(request: UserSearchRequest, pageRequest: Pageable): Page<UserInfoDto> {
        val userList = userDslRepo.findAll(request = request, pageable = pageRequest)
        return userList.map { user: User ->
            val userType = when (user.admin) {
                false -> "일반회원"
                true -> if (readAdminByUserId(user.id).superAdmin) {
                    "슈퍼관리자"
                } else {
                    "일반관리자"
                }
            }
            UserInfoDto(
                id = user.id,
                email = user.email,
                nickname = user.nickname,
                phoneNumber = user.phoneNumber,
                userType = userType,
                snsProvider = SnsProvider.valueOf(user.snsProvider ?: "X"),
                createdAt = user.createdAt!!
            )
        }
    }

    /**
     * 결제 내역 보기
     */
    fun getPaymentHistory(request: PaymentLogSearchRequest, pageRequest: Pageable): Page<PaymentHistoryDto> {
        val paymentHistoryList = paymentLogDslRepo.findAll(request = request, pageable = pageRequest)
        return paymentHistoryList.map { it ->
            val checkSingleCell = it.cell != null
            val cell: Cell = when(checkSingleCell) {
                true -> it.cell!!
                false -> cellService.getFirstCellFromCellIds(it.cellIds)
            }
            val today = LocalDate.now()
            val daysAfter = Period.between(it.createdAt.toLocalDate(), today).days
            // 7일이 지난경우 환불 불가
            val paymentStatus = if (daysAfter > 7 && it.cancelAdmin == null) {
                PaymentStatus.EXPIRED
            // 이미 환불한 경우 환불 완료
            } else if (it.cancelAdmin != null) {
                PaymentStatus.REFUNDED
            } else {
                PaymentStatus.REFUNDABLE
            }
            PaymentHistoryDto(
                tid = it.trNo,
                authDate = it.createdAt,
                email = it.user.email,
                district = cell.centerCity!!,
                cellIds = if (checkSingleCell) { arrayListOf(it.cell!!.id) } else {it.cellIds!!.split(",").map{it.toLong()}} ,
                amt = it.trPrice,
                paymentMethod = it.method,
                status = paymentStatus.displayName,
                cancelReason = it.cancelReason
            )
        }
    }

    /**
     * 전체 폴리곤의 셀 데이터 확인
     */
    fun getCellDataInAllPolygon(request: PolygonCellDataSearchRequest, pageRequest: Pageable): Page<PolygonCellDataDto> {
        val preOrderList = if (request.district == null && request.name == null && request.sort == null) {
            preOrderRepo.findAll(pageRequest)
        } else {
            preOrderDslRepo.findAll(request = request, pageable = pageRequest)
        }
        return preOrderList.map {
            PolygonCellDataDto(
                id = it.id,
                areaId = it.areaId,
                name = it.name,
                totalCellCount = it.cellCount,
                reservedCellCount = it.reservedCellCount,
                purchasableCellCount = it.cellCount - it.reservedCellCount,
                purchasedCellCount = it.purchaseCount,
                soldOut = (it.purchaseCount - (it.cellCount - it.reservedCellCount)) == 0L
            )
        }
    }

    /**
     * 특정 폴리곤에 속한 셀들의 리스트와 판매여부 확인
     */
    fun getCellListByAreaId(areaId: Long): CellListInfoByAreaIdResponse {
        val result = mutableListOf<CellDtoForCellList>()
        val area = preOrderRepo.findByAreaId(areaId) ?: throw NotFoundException("입력한 areaId로 해당하는 사전청약건을 찾을 수 없습니다.")
        val cellList = cellRepo.findAllByAreaId(areaId)
        var reservedCellCount = 0
        cellList.forEach {
            var status = PreContractStatus.PURCHASABLE
            if (it.reserved) {
                reservedCellCount += 1
                status = PreContractStatus.RESERVED
            } else if (it.owner != null) {
                status = PreContractStatus.PURCHASED
            }
            result.add(
                CellDtoForCellList(
                    cellId = it.id,
                    areaId = it.areaId!!,
                    status = status,
                    centerX = it.centerX!!.toDouble(),
                    centerY = it.centerY!!.toDouble(),
                )
            )
        }
        val purchasedCellCount = cellRepo.countAllByAreaIdAndOwnerIsNotNull(area.areaId)
        preOrderRepo.save(area.copy(
            purchaseCount = purchasedCellCount
        ))
//        val geoJson = redissonClient.getBucket<JsonNode>("geojson_${areaId}").get()
        return CellListInfoByAreaIdResponse(
            areaId = areaId,
            name = area.name,
            centerLat = area.latitude, centerLong = area.longitude,
            totalCellCount = cellList.size.toLong(),
            reservedCellCount = reservedCellCount.toLong(),
            purchasableCellCount = (cellList.size - reservedCellCount).toLong(),
            purchasedCellCount = purchasedCellCount,
            cellInfoList = result,
            soldOut = (area.purchaseCount - (area.cellCount - area.reservedCellCount)) == 0L,
            preOrderStatus = area.status,
//            geoJson = geoJson
        )
    }

    /**
     * 특정 셀들의 판매 상태 변경 (한번에 하나의 polygon 영역 내의 Cell 들만 수정된다는 가정하에 작동)
     */
    fun changeCellReservingStatus(request: CellIdsRequest): CellListInfoByAreaIdResponse {
        if (request.status == null) throw NotNullException("status 필드는 필수 입력값입니다.")

        val cellIds = request.cellIds
        var areaId = 0L
        val firstTransaction = transactionManager.getTransaction(DefaultTransactionDefinition())
        try {
            val cellIdList = cellIds.replace(" ","").split(",")
            cellIdList.forEach {
                val targetCell = cellRepo.findById(id = it.toLong()) ?: throw NotFoundException("셀 ID#${it}로 찾을 수 없는 셀 입니다.")
                areaId = targetCell.areaId!!
                if (targetCell.owner != null) throw AuthenticationException("이미 구매된 셀은 상태를 변경할 수 없습니다.")
                when (request.status) {
                    "RESERVED" -> {
                        cellRepo.save(targetCell.copy(reserved = true))
                        appConfig.logger.info { "[TWINKOREA API] 셀#${it}의 판매 상태 판매 가능에서 예약됨으로 변경" }
                    }
                    "PURCHASABLE" -> {
                        cellRepo.save(targetCell.copy(reserved = false))
                        appConfig.logger.info { "[TWINKOREA API] 셀#${it}의 판매 상태 예약됨에서 판매 가능으로 변경" }
                    }
                    else -> {
                        throw WrongStatusException("처리할 수 있는 입력값(${request.status})이 아닙니다. ")
                    }
                }
            }
            transactionManager.commit(firstTransaction)
        } catch (e: Exception) {
            transactionManager.rollback(firstTransaction)
            throw MsgException(e.message.toString())
        }
        val secondTransaction = transactionManager.getTransaction(DefaultTransactionDefinition())
        try {
            val targetPreOrder = preOrderRepo.findByAreaId(areaId) ?: throw NotFoundException("대상 사전청약건의 폴리곤 영역 데이터를 참조할 수 없습니다.")
            val totalReserved = cellRepo.countAllByAreaIdAndReservedIsTrue(areaId)
            print("totalReserved = $totalReserved")
            preOrderRepo.save(targetPreOrder.copy(reservedCellCount = totalReserved))
            transactionManager.commit(secondTransaction)
        }catch (e: Exception) {
            transactionManager.rollback(secondTransaction)
            throw MsgException(e.message.toString())
        }

        val result = mutableListOf<CellDtoForCellList>()
        val area = preOrderRepo.findByAreaId(areaId) ?: throw NotFoundException("입력한 areaId로 해당하는 사전청약건을 찾을 수 없습니다.")
        val cellList = cellRepo.findAllByAreaId(areaId)
        var reservedCellCount = 0
        cellList.forEach {
            var status = PreContractStatus.PURCHASABLE
            if (it.reserved) {
                reservedCellCount += 1
                status = PreContractStatus.RESERVED
            } else if (it.owner != null) {
                status = PreContractStatus.PURCHASED
            }
            result.add(
                CellDtoForCellList(
                    cellId = it.id,
                    areaId = it.areaId!!,
                    status = status,
                    centerX = it.centerX!!.toDouble(),
                    centerY = it.centerY!!.toDouble(),
                )
            )
        }
        return CellListInfoByAreaIdResponse(
            areaId = areaId,
            name = area.name,
            centerLat = area.latitude, centerLong = area.longitude,
            totalCellCount = cellList.size.toLong(),
            reservedCellCount = reservedCellCount.toLong(),
            purchasableCellCount = (cellList.size - reservedCellCount).toLong(),
            purchasedCellCount = area.purchaseCount,
            cellInfoList = result,
            preOrderStatus = area.status,
            )
    }

    /**
     * 관리자 추가
     */
    @Transactional
    fun createAdminUser(request: AdminUserRequest): CreateAdminResponse {
        if (request.adminRole == null
            || request.email == null
            || request.pw == null
            || request.nickname == null
            || request.phoneNumber == null) {
            throw NotNullException("pw, nickname, phoneNumber, adminRole, email 은 필수입력값입니다.")
        }
        val userRequest = UserRequest(
            snsProvider = SnsProvider.X,
            email = request.email,
            pw = request.pw,
            nickname = request.nickname,
            phoneNumber = request.phoneNumber,
        )
        val userTransformed = userTransformer.from(request = userRequest)

        val secretKey = TOTPTokenGenerator.generateSecretKey()
        val newUser = userRepo.save(userTransformed.copy(admin = true, otpSecretKey = secretKey))
        val barcodeUrl = TOTPTokenGenerator.getGoogleAuthenticatorBarcode(
            secretKey, newUser.email, "TwinKorea Admin Auth"
        )
        val newAdminModel = transformer.from(request = request)
        val newAdmin = repo.save(newAdminModel.copy(
            user = newUser
        ))

        val localUserSnsId = "LOCAL${newAdmin.user.id}"
        val refreshToken = jwtTokenProvider.createRefreshToken(localUserSnsId)
        userRepo.save(newAdmin.user.copy(snsId = localUserSnsId, refreshToken = refreshToken))
        appConfig.logger.info {"[TWINKOREA API] 관리자#${newAdmin.id} 추가 완료"}

        return CreateAdminResponse(otpSecretKey = secretKey, qrBarcodeUrl = barcodeUrl)
    }

    /**
     * 관리자 비활성화
     */
    @Transactional
    fun deActiveAdminUser(adminId: Long, user: User): Admin {
        val superAdmin = readAdminByUserId(user.id)
        if (!superAdmin.superAdmin) throw AuthenticationException("슈퍼관리자만 삭제를 진행할 수 있습니다.")

        val targetAdmin = readAdmin(adminId = adminId)
        val now = LocalDateTime.now()
        // email에 unique index 가 붙어있어서 이런식으로 처리해줘야 재가입이 가능함 -> 기획에서 재가입되는지 이런걸 들은바 없어서 임의로 재가입 가능하게 설정해둠
        // 다만 회원의 경우 재가입 불가능하도록 해둠 (왠지 그래야할것 같아서..)
        userRepo.save(targetAdmin.user.copy(deactivate = true, updatedAt = now, otpSecretKey = null, email = "${targetAdmin.user.email}-deactivated"))
        val deActivatedAdmin = repo.save(targetAdmin.copy(updatedAt = now))
        appConfig.logger.info {"[TWINKOREA API] 관리자#${deActivatedAdmin.id} 비활성화 완료"}
        return deActivatedAdmin
    }

    /**
     * 관리자 정보 수정
     */
    @Transactional
    fun editAdminUser(adminId: Long, request: AdminUserRequest, user: User): Admin {
        val superAdmin = readAdminByUserId(user.id)
        if (!superAdmin.superAdmin) throw AuthenticationException("슈퍼관리자만 삭제를 진행할 수 있습니다.")

        val targetAdmin = readAdmin(adminId = adminId)
        val now = LocalDateTime.now()
        userRepo.save(targetAdmin.user.copy(
            email = request.email ?: targetAdmin.user.email,
            phoneNumber = request.phoneNumber ?: targetAdmin.user.phoneNumber,
            nickname = request.nickname ?: targetAdmin.user.nickname,
            updatedAt = now
        ))
        val editedAdmin = repo.save(targetAdmin.copy(
            adminRole = request.adminRole ?: targetAdmin.adminRole,
            superAdmin = request.superAdmin ?: targetAdmin.superAdmin
        ))
        appConfig.logger.info {"[TWINKOREA API] 관리자#${editedAdmin.id} 수정 완료"}
        return editedAdmin
    }

    /**
     * 기존 결제건 환불 처리
     */
    @Transactional
    fun refundPayment(trNo: String, refundRequest: RefundRequest, user: User): SettleBankResponse {
        val admin = readAdminByUserId(user.id)
        val now = LocalDateTime.now()
        val targetPaymentLog = settlebankService.readPaymentLog(trNo)
        appConfig.logger.info { "[TWINKOREA API] 어드민#${user.id} 환불신청 처리 시작: 대상회원#${targetPaymentLog.user.id} / 대상주문건#${targetPaymentLog.ordNo}" }

        val targetPreOrder = if (targetPaymentLog.cell != null) {
            val targetPreContract = preContractRepo.findByTrNo(trNo) ?: throw NotFoundException("입력한 거래건을 찾을 수 없습니다.")
            if (targetPreContract.preOrderUser != null) {
                targetPreContract.preOrderUser.preOrder
            } else if (targetPreContract.waitingList != null) {
                targetPreContract.waitingList.preOrder
            } else {
                throw NotFoundException("환불 대상 지역을 찾을 수 없습니다. 관리자에게 문의해주세요.")
            }
        } else if (targetPaymentLog.cellIds != null) {
            val targetContract = contractRepo.findByTrNo(targetPaymentLog.trNo) ?: throw NotFoundException("해당하는 주문건을 찾을 수 없습니다.")
            targetContract.preOrder
        } else {
            throw NotFoundException("환불 대상 지역을 찾을 수 없습니다. 관리자에게 문의해주세요.")
        }

        // 서울 지역 구매한 할인 쿠폰으로 경인지역에서 이득을 본 뒤 서울지역 할인 할 때 환수할 쿠폰이 마이너스가 되면 환불을 못하게 해야함 (2022년 3월 8일 오후 3시경 이세호님한테 유선으로 전달받음)
        var couponCntAfterRefund = 0L
        if (targetPreOrder.id < 630) {
            if (targetPaymentLog.cell != null) {
                couponCntAfterRefund = targetPaymentLog.user.preContractCouponCount - 1
            } else if (targetPaymentLog.cellIds != null) {
                couponCntAfterRefund = targetPaymentLog.user.preContractCouponCount - targetPaymentLog.cellIds.split(",").size
            }
        }
        if (couponCntAfterRefund < 0) {throw WrongStatusException("이미 서울지역 구매로 받은 쿠폰을 사용하여 경인지역 구매시 이득을 획득한 회원입니다. 환불이 불가능합니다.")}


        val requestRefund = settlebankService.cancelRequest(trNo)
        return when (requestRefund.resultIsSuccess) {
            true -> {
                paymentLogRepo.save(
                    targetPaymentLog.copy(
                        canceledAt = now,
                        cancelTrNum = requestRefund.cancelTrNo,
                        cancelReason = refundRequest.refundReason ?: "미입력",
                        cancelAdmin = admin,
                    )
                )
                // 단일셀 결제
                if (targetPaymentLog.cell != null) {
                    val targetPreContract = preContractRepo.findByTrNo(trNo) ?: throw NotFoundException("입력한 거래건을 찾을 수 없습니다.")
                    cellRepo.save(
                        targetPaymentLog.cell.copy(
                            owner = null,
                        )
                    )
                    preContractRepo.save(targetPreContract.copy(refunded = true))
                    // 서울지역건 환불하는 경우 쿠폰 환수
                    if (targetPreOrder.id < 630) {
                        userRepo.save(targetPaymentLog.user.copy(preContractCouponCount = couponCntAfterRefund))
                    }
                // 멀티셀 결제
                } else if (targetPaymentLog.cellIds != null) {
                    val targetContract = contractRepo.findByTrNo(targetPaymentLog.trNo) ?: throw NotFoundException("해당하는 주문건을 찾을 수 없습니다.")
                    val targetCellContracts = contractCellRepo.findAllByContract(targetContract)
                    val targetCells = targetCellContracts.map { it.cell }
                    contractRepo.save((targetContract).copy(refunded = true))
                    targetCells.forEach {
                        cellRepo.save(
                            it.copy(
                                owner = null
                            )
                        )
                    }
                    // 서울지역건 환불하는 경우 쿠폰 환수
                    if (targetPreOrder.id < 630) {
                        userRepo.save(targetPaymentLog.user.copy(preContractCouponCount = couponCntAfterRefund))
                    }
                } else {
                    throw WrongStatusException("환불할 대상 셀을 찾지 못했습니다.")
                }
                val purchasedCellCount = cellRepo.countAllByAreaIdAndOwnerIsNotNull(targetPreOrder.areaId)
                preOrderRepo.save(targetPreOrder.copy(
                    purchaseCount = purchasedCellCount))



                appConfig.logger.info { "[TWINKOREA API] 어드민#${user.id} 환불신청 처리 완료: 대상회원#${targetPaymentLog.user.id} / 대상주문건#${targetPaymentLog.ordNo}" }
                requestRefund
            }
            false -> requestRefund
        }
    }

    /**
     * 회원 탈퇴
     */
    fun deactivateUser(userId: Long, adminUser: User): DeactivateResponse {
        val admin = readAdminByUserId(adminUser.id)
        if (!admin.superAdmin) throw AuthenticationException("회원 탈퇴는 슈퍼 어드민만 가능합니다.")
        val user = userService.readUser(userId)
        val snsProvider = SnsProvider.valueOf(user.snsProvider!!)
        val unlinkUser = authService.unlinkSns(snsProvider = snsProvider, userId = user.id)
        return if (unlinkUser) {
            userRepo.save(user.copy(
                deactivate = true,
                snsId = "deactivated${user.snsId}",
                refreshToken = "",
            ))
            DeactivateResponse(success = true, user = user)
        } else {
            DeactivateResponse(success = false, user = user)
        }
    }
}