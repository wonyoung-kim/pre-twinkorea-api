package io.amona.twinkorea.service

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.Cell
import io.amona.twinkorea.domain.PreOrder
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.dtos.CellDetailDto
import io.amona.twinkorea.dtos.CellOnPaymentStatusDto
import io.amona.twinkorea.dtos.MultiCellDetailDto
import io.amona.twinkorea.enums.CellType
import io.amona.twinkorea.exception.DuplicatedException
import io.amona.twinkorea.exception.NotFoundException
import io.amona.twinkorea.repository.CellRepository
import io.amona.twinkorea.repository.PreOrderRepository
import io.amona.twinkorea.request.CellRequest
import io.amona.twinkorea.request.MultiCellRequest
import io.amona.twinkorea.response.CellInfoResponse
import io.amona.twinkorea.response.CellOnPaymentStatusListResponse
import io.amona.twinkorea.service.external.PlaceService
import io.amona.twinkorea.utils.CoordsTransform
import org.locationtech.proj4j.ProjCoordinate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class CellService(private val repo: CellRepository,
                  private val preOrderRepo: PreOrderRepository,
                  private val placeService: PlaceService,
                  private val userService: UserService,
                  private val appConfig: AppConfig,
) {
    val cityPrice = appConfig.cityPrice.toLong()
    val ruralPrice = appConfig.ruralPrice.toLong()
    val seaPrice = appConfig.seaPrice.toLong()
    val cellDiscountTo = appConfig.cellDiscountTo

    // 대기 청약자 구매 가능 시간 범위
    val cellDiscountToDateTime  = LocalDateTime.parse(cellDiscountTo, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))

    /**
     * 셀 정보를 조회한다.
     */
    fun readCell(cellId: Long): Cell {
        return repo.findById(id = cellId)
            ?: throw NotFoundException("입력한 셀 ID#${cellId}로 셀을 찾아낼 수 없습니다.")
    }

    /**
     * 셀 타입을 조회한다.
     * 네이버 reverse geocoding API 특성상 법정코드를 제공하지 않습니다 (네이버 자체 분류 코드만 제공함)
     * 땨라서 현재로서 가장 쉽게 City / Rural / Sea 여부를 판단할 수 있는 방법은 행정구역 "명칭"을 통해서 비교하는 방법입니다.
     * "~~로" 로 끝나는 경우는 찾아보니, 군사보안 목적으로 네이버에서 법정동을 알려주지 않는 경우가 있습니다. (사직동을 세종로로 표현)
     * 이 경우, 네이버 크롤링시 요청 url을 https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?coords=%s&output=json
     * 에서 https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?orders=admcode&coords=%s&output=json 로 바꿔주면 됩니다.
     * 법정동은 군사보안 목적으로 응답하지 않지만, 행정동은 범위가 넓어 정상적으로 리턴되는 것으로 확인했습니다.
     * 자세한 내용은 크롤링 코드 naverapi.go 의 26 번째 라인을 확인해주세요.
     */
    private fun getCellType(cell: Cell): Map<String, Any> {
        val cellType: CellType
        val price: Long
        if (cell.centerCity == "없음") {
            cellType = CellType.SEA
            price = seaPrice
        } else if (cell.centerCity!!.takeLast(1) == "리") {
            cellType = CellType.RURAL
            price = cityPrice
        } else {
            cellType = CellType.CITY
            price = cityPrice
        }
        return mapOf("cellType" to cellType, "price" to price)
    }

    /**
     * 셀 상세정보를 조회한다.
     */
    fun getCellDetail(cellId: Long, user: User? = null): CellDetailDto {
        val targetCell = readCell(cellId)

        // 추후 할인 기획이 정해지지 않아, 할인 지역 판단 여부는 하드코딩으로 처리했습니다. 할인지역(경인지역)의 셀이고 할인가능한 상태인경우 할인가격을 제공합니다.
        val discountArea = arrayListOf<Long>(111,107,108,109,110,112,114,119,1163,1166,146,122,125,133,155,157,509,1144)
        val discount = userService.checkUserHasPreContractCoupon(user) && targetCell.areaId in discountArea
        val discountedPrice = if (discount && targetCell.areaId in discountArea) {
            (getCellType(targetCell)["price"] as Long * 0.7).toLong()
        } else {
            null
        }
        return CellDetailDto(
            cellId = targetCell.id,
            cellType = getCellType(targetCell)["cellType"] as CellType,
            areaId = targetCell.areaId ?: 0,
            leftTop = targetCell.leftTop!!,
            rightTop = targetCell.rightTop!!,
            leftBottom = targetCell.leftBottom!!,
            rightBottom = targetCell.rightBottom!!,
            centerLatitude = targetCell.centerY!!,
            centerLongitude = targetCell.centerX!!,
            centerCity = targetCell.centerCity!!,
            price = getCellType(targetCell)["price"] as Long,
            onPayment = targetCell.onPayment,
            // 회원이 할인 받을 수 있을 때만 이 값 부여 / 비로그인-할인 불가능할 경우 false
            discount = discount,
            // 회원이 할인 받을 수 있을 때는 30% 할인된 가격 / 비로그인-할인 불가능힐 경우 null
            discountedPrice = discountedPrice
        )
    }

    /**
     * 여러개의 셀 상세정보를 조회한다.
     */
    fun getMultiCellDetail(cellIds: MultiCellRequest, user: User? = null): MultiCellDetailDto {
        val cellIdsList = cellIds.cellIds
        val targetCells = repo.findAllByIds(cellIdsList)

        val discountArea = arrayListOf<Long>(111,107,108,109,110,112,114,119,1163,1166,146,122,125,133,155,157,509,1144)
        val discount = userService.checkUserHasPreContractCoupon(user) && targetCells[0].areaId in discountArea
        val resultList = mutableListOf<CellDetailDto>()
        var price = 0L
        var discountedPrice = 0L
        var index = 1L
        targetCells.forEach {
            // 할인된 가격은 회원이 할인받을 수 있는 상태이며, 해당 지역이 할인 지역이며,
            // 조회하고자하는 셀 순번이 할인 쿠폰 갯수 미만일이며,
            // 할인 기간 이내 일 경우에만 할인 적용
            val discountedPricePerCell = if (
                discount && it.areaId in discountArea &&
                user!!.preContractCouponCount >= index &&
                LocalDateTime.now().isBefore(cellDiscountToDateTime)
            ) {
                (getCellType(it)["price"] as Long * 0.7).toLong()
            } else {
                null
            }
            val cellDetail = CellDetailDto(
                cellId = it.id,
                cellType = getCellType(it)["cellType"] as CellType,
                areaId = it.areaId ?: 0,
                leftTop = it.leftTop!!,
                rightTop = it.rightTop!!,
                leftBottom = it.leftBottom!!,
                rightBottom = it.rightBottom!!,
                centerLatitude = it.centerY!!,
                centerLongitude = it.centerX!!,
                centerCity = it.centerCity!!,
                price = getCellType(it)["price"] as Long,
                onPayment = it.onPayment,
                // 회원이 할인 받을 수 있을 때만 이 값 부여 / 비로그인-할인 불가능할 경우 false
                discount = discountedPricePerCell != null,
                // 회원이 할인 받을 수 있을 때는 30% 할인된 가격 / 비로그인-할인 불가능힐 경우 null
                discountedPrice = discountedPricePerCell
            )
            resultList.add(cellDetail)
            price += getCellType(it)["price"] as Long
            // 할인금액이 있으면 할인 금액을 더하고, 할인금액이 없으면 정상가를 더해 할인되는 총 금액을 구한다.
            discountedPrice += discountedPricePerCell ?: getCellType(it)["price"] as Long
            index += 1
        }
        return MultiCellDetailDto(
            cellDetailList = resultList,
            cellCount = resultList.size.toLong(),
            totalPrice = price,
            discount = discount,
            discountedPrice = discountedPrice,
        )
    }

    /**
     * 여러개의 셀 점유여부를 조회한다.
     */
    fun getMultiCellOnPaymentStatus(cellIds: MultiCellRequest): CellOnPaymentStatusListResponse {
        val cellIdsList = cellIds.cellIds
        val targetCells = repo.findAllByIds(cellIdsList)
        val resultList = mutableListOf<CellOnPaymentStatusDto>()
        var hasOnPayment = false
        targetCells.forEach {
            if (it.onPayment == true) hasOnPayment = true
            val onPayment = it.onPayment == true
            val cellOnPaymentStatus = CellOnPaymentStatusDto(
                cellId = it.id,
                onPayment = onPayment
            )
            resultList.add(cellOnPaymentStatus)
        }
        return CellOnPaymentStatusListResponse(hasOnPayment, resultList)
    }

    /**
     * 셀에 포함된 식당들의 식신 등급 정보를 확인한다.
     */
    fun readCellWithInfo(cellIds: List<Long>): CellInfoResponse {
        var cellCount: Long = 0
        var siksinStar: Long = 0
        var siksinHot: Long = 0
        var siksinNormal: Long = 0
        cellIds.forEach {
            // 개별 셀의 범위를 가져와서
            val cellRangeMap = getCellRange(it)
            // 해당 범위로 쿼리 날린 식신의 카운트 정보를 확인한다.
            val siksinCountInfo = placeService.getSiksinInfoByRange(cellRangeMap)
            cellCount += siksinCountInfo["cellCount"]!!
            siksinStar += siksinCountInfo["siksinStar"]!!
            siksinHot += siksinCountInfo["siksinHot"]!!
            siksinNormal += siksinCountInfo["siksinNormal"]!!
        }
        return CellInfoResponse(
            cellCount = cellCount,
            siksinStar = siksinStar,
            siksinHot = siksinHot,
            siksinNormal = siksinNormal,
            pricePerCell = cityPrice,
        )
    }

    /**
     * 좌표 범위 내 셀 목록을 조회한다. 945600,191400
     */
    fun readCellsByRange(start: String, end: String): MutableList<Cell> {
        val startArray = start.replace(" ","").split(",")
        val endArray = end.replace(" ","").split(",")

        val minX: Double = startArray[0].toDouble()
        val maxX: Double = endArray[0].toDouble()
        val minY: Double = endArray[1].toDouble()
        val maxY: Double = startArray[1].toDouble()
        appConfig.logger.info {"[TWINKOREA API] 경도 검색 범위 (EPSG:4326) : ${minX} ~ ${maxX}"}
        appConfig.logger.info {"[TWINKOREA API] 위도 검색 범위 (EPSG:4326): ${minY} ~ ${maxY}"}

        val utmKStart = CoordsTransform.toUtmK(minX, minY)
        val utmKEnd = CoordsTransform.toUtmK(maxX, maxY)
        appConfig.logger.info {"[TWINKOREA API] 경도 검색 범위 (EPSG:5179): ${utmKStart.x} ~ ${utmKEnd.x}"}
        appConfig.logger.info {"[TWINKOREA API] 위도 검색 범위 (EPSG:5179): ${utmKStart.y} ~ ${utmKEnd.y}"}
        return repo.findAllByCenterXBetweenAndCenterYBetween(
            centerXStart = utmKStart.x, centerXEnd = utmKEnd.x,
            centerYStart = utmKStart.y, centerYEnd = utmKEnd.y,
        )
    }

    /**
     * 셀을 수정한다.
     */
    fun updateCell(cellId: Long, request: CellRequest): Cell {
        val cell = readCell(cellId = cellId)
        return repo.save(cell.copy(
            land = request.land,
            owner = request.owner,
        ))
    }

    /**
     * 셀에서 셀이 해당하는 지역의 preOrder 데이터를 찾아낸다.
     */
    fun findPreOrderByCell(cell: Cell): PreOrder {
        return preOrderRepo.findByAreaId(cell.areaId!!) ?: throw NotFoundException("구매건과 일치하는 지역을 찾을 수 없습니다.")
    }

    /**
     * 문자열로 된 cellIds에서 첫번째 셀을 추출한다.
     */
    fun getFirstCellFromCellIds(cellIds: String?): Cell {
        return if (cellIds != null) {
            val firstCellId = cellIds
                .replace(" ", "")
                .split(",")
                .toMutableList()
                .map{it.toLong()}[0]
            repo.findById(id = firstCellId) ?: throw NotFoundException("입력한 값으로 해당하는 셀을 찾을 수 없습니다.")
        } else {
            throw NotFoundException("입력한 값으로 해당하는 셀을 찾을 수 없습니다.")
        }
    }

    /**
     * 문자열로 된 cellIds에서 셀들의 리스트를 조회한다.
     */
    fun getCellListFromCellIds(cellIds: String): MutableList<Cell> {
        val cellIdsList = cellIds.replace(" " ,"").split(",").toMutableList().map{it.toLong()}
        return repo.findAllById(cellIdsList)
    }

    /**
     * 셀들이 구매 가능한지 여부 확인 (이미 구매됐는지, 다른 유저가 결제했는지 확인)
     */
    fun checkCellIsPurchasableByCellList(cells: MutableList<Cell>, user: User) {
        cells.forEach {
            if (it.owner != null || it.reserved) throw DuplicatedException("이미 구매된 셀 입니다.")
            else if (it.onPayment == true && it.onPaymentBy != user.id) throw DuplicatedException("이미 다른 유저에 의해 결제 진행중입니다.")
        }
    }

    /**
     * 셀의 좌하단과 우상단 좌표를 EPSG:4326 으로 변환한 값을 Map으로 반환한다.
     */
    private fun getCellRange(cellId: Long): MutableMap<String, ProjCoordinate> {
        val cell = readCell(cellId = cellId)
        val x = cell.centerX!!
        val y = cell.centerY!!
        val min = CoordsTransform.fromUtmK(x-50, y-50)
        val max = CoordsTransform.fromUtmK(x+50, y+50)
        return mutableMapOf("min" to min, "max" to max)
    }

    /**
     * 10분동안 결제 마무리 안된 셀 점유권 해제
     */
    @Scheduled(fixedDelay = 10L * 60 * 1000)
    fun scheduledTask() {
        val now = LocalDateTime.now()
        val nowMinusTenMin = now.minusMinutes(10)
        val cellList = repo.findAllByOnPaymentIsTrueAndUpdatedAtIsBefore(nowMinusTenMin)
        if (cellList.size > 0) {
            cellList.forEach {
                repo.save(it.copy(
                    onPayment = null,
                    updatedAt = now,
                    onPaymentBy = null,
                ))
            }
            appConfig.logger.info {"[TWINKOREA API] 결제 제한 시간 초과로 cell #${cellList.size}개의 상태를 변경했습니다."}
        }
    }
}